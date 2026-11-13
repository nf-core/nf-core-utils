# NfcoreCitationUtils

Citation management and topic channel utilities for nf-core pipelines.

## Overview

The `NfcoreCitationUtils` utility provides comprehensive citation management functionality for nf-core pipelines, supporting both traditional file-based approaches and modern topic channel patterns. This utility enables progressive migration from legacy meta.yml citation files to the more efficient and accurate topic channel system.

### Topic Channel Migration Strategy for Citations

Similar to version management, the citation system supports a three-phase migration approach:

1. **Legacy Stage**: Traditional meta.yml file processing with manual collection
2. **Transition Stage**: Mixed approach supporting both files and topic channels
3. **Modern Stage**: Pure topic channel approach with automatic runtime citation collection

This migration strategy ensures backward compatibility while enabling pipelines to benefit from runtime-accurate citations where only tools that actually execute are included in the final reports.

## Available Functions

### Citations on the Fly (Recommended)

The lowest-friction way to make citations reflect the tools a pipeline actually ran — generated _on the fly_ from the run itself, not from a hand-maintained list. It requires **no per-module changes**: every nf-core module already emits a `[process, tool, version]` tuple to the `versions` topic when it executes, so that topic _is_ the list of tools that ran. These functions intersect that list with citation metadata parsed from each module's `meta.yml`, so only tools that actually executed are cited.

Two functions cover the flow:

| Function                                          | Purpose                                                 | When to call        |
| ------------------------------------------------- | ------------------------------------------------------- | ------------------- |
| `toolsFromVersionsTopic(topicVersions)`           | Reduce collected `versions` data to the tool names used | Workflow completion |
| `citationsOnTheFly(topicVersions, metaFilePaths)` | Citations for only the tools that ran, from `meta.yml`  | Workflow completion |

---

#### `citationsOnTheFly()`

`citationsOnTheFly(List topicVersions, List<String> metaFilePaths)`

**Description:**
Builds a citations map for **only the tools that ran**, by intersecting the `versions` topic (what executed) with citations parsed from the supplied `meta.yml` files (the citation source). The returned map plugs directly into [`citationOutputs()`](#citation-outputs-html-bibtex-and-csl-json).

Each **short citation** is formatted for a methods paragraph — `tool (version, Author (year))` — with the version taken from the `versions` topic and the author/year from `meta.yml`. The reference segment degrades gracefully: short author + year → `doi: …` → homepage url → description. So richer `meta.yml` files produce richer citations:

!!! note "HTML output"
Short citations are HTML, meant for the MultiQC methods section: the reference links to the DOI (else the homepage) and `et al.` is wrapped in `<em>`. The table below shows the rendered text. Save `tool_citations` to an `.html` file, such as `auto_citations.html`, not a `.txt` file. To reuse a citation in a manuscript, open that file or the MultiQC report in a browser and copy the rendered text. Pasting the HTML source without formatting does not remove literal tags.

!!! info "Harvard style"
Short citations follow the Harvard style recommended by [nf-core/modules](https://github.com/nf-core/modules/blob/master/modules/meta-schema.json) for `meta.yml` author fields: `Last, F. M.` for author names, with `Author (year)` for single authors and `Author et al. (year)` for multiple.
For example, `Andrews, S.` renders as `Andrews (2010)`, while `van der Walt, S.` preserves the full surname. Legacy `Last Initials` author lists remain supported, including flat `author`/`year` metadata when no `publication` object is present.

| `meta.yml` fields present                           | Short citation                                       |
| --------------------------------------------------- | ---------------------------------------------------- |
| `publication.author`, `publication.year`            | `fastqc (0.12.1, Andrews (2010))`                    |
| `publication.author` (multiple), `publication.year` | `samtools (1.21, Danecek et al. (2021))`             |
| `doi` only                                          | `multiqc (1.21, doi: 10.1093/bioinformatics/btw354)` |
| `homepage` only                                     | `seqtk (1.4, https://github.com/lh3/seqtk)`          |

The full `bibliography` entry includes author, year, title and source when available, followed by a DOI link or, when no DOI is supplied, the homepage link. This applies to both nested publication metadata and legacy flat fields.

**Function Signature:**

```groovy
Map citationsOnTheFly(List topicVersions, List<String> metaFilePaths, List<String> extraTools = [])
```

**Parameters:**

- `topicVersions` (List): Collected `versions` topic data — `[process, tool, version]` tuples. Collect with `.toList()` so the tuples are preserved (plain `.collect()` flattens them, and the function logs a warning if it receives flattened data).
- `metaFilePaths` (List\<String\>): Paths to module `meta.yml` files to resolve citation metadata from.
- `extraTools` (List\<String\>, optional): Tool names to cite even though they never emit a version — e.g. a MultiQC plugin like `multiqcsav`. They're resolved from `meta.yml` like everything else (with no version segment), so you can still add a tool by hand: `citationsOnTheFly(versions, meta_yml_paths, ['multiqcsav'])`.

**Returns:**

- `Map`: Citations keyed by tool name (`[citation: ..., bibliography: ..., info: ..., version: ...]`), restricted to tools that ran. `info` is the raw `meta.yml` tool map and `version` the version from the `versions` topic; [`citationOutputs()`](#citation-outputs-html-bibtex-and-csl-json) reads them.

:::note{title="Tool-name matching"}
Tool names from the `versions` topic are matched against `meta.yml` tool keys **exactly first, then case-insensitively** (so `FastQC` lines up with `fastqc`). A tool that ran but has no matching `meta.yml` citation entry is logged as a warning — add it to that module's `meta.yml` `tools:` section to silence it.
:::

**Pipeline Integration:**

This is the reproducible replacement for a hand-maintained `toolReferencesMap` and a static `toolCitationText()` / `toolBibliographyText()`. Wire it into the methods-description channel:

```nextflow title="workflows/<pipeline>.nf"
include { citationsOnTheFly; citationOutputs } from 'plugin/nf-core-utils'

workflow PIPELINE {
    take:
    ch_multiqc_files

    main:
    // files() returns an empty list when nothing matches the glob
    def meta_yml_paths = files("${projectDir}/modules/**/meta.yml").collect { p -> p.toString() }

    ch_methods_description = channel.topic('versions')
        .toList()
        .map { topic_versions ->
            def citations = citationsOnTheFly(topic_versions, meta_yml_paths)
            citationOutputs(citations, ['html'], file("${projectDir}/assets/methods_description_template.yml")).methods_description
        }
        .collectFile(name: 'methods_description_mqc.yaml')

    emit:
    multiqc_files = ch_multiqc_files.mix(ch_methods_description)
}
```

**Before / After:**

```nextflow title="Before — static, hand-maintained"
// Every tool listed by hand; cited even if it never ran
def citation_text = [
    "Tools used in the workflow included:",
    "BWAMEM2 (Vasimuddin et al. 2019)",
    "FastQC (Andrews 2010),",
    // ...one line per tool, forever...
].join(' ').trim()
```

```nextflow title="After — citations on the fly, zero maintenance"
// Cites exactly the tools that ran, metadata from each module's meta.yml
def citations = citationsOnTheFly(topic_versions, meta_yml_paths)
def citation_text = citationOutputs(citations).tool_citations
```

---

#### Citation outputs: HTML, BibTeX and CSL-JSON

`citationOutputs(Map citations, List<String> formats = ['html'], Object mqcMethodsYaml = null)` returns several formats in one call. The template is a `Path` or a path string, local or remote. Pick any of:

| Format     | Output keys                                                                               |
| ---------- | ----------------------------------------------------------------------------------------- |
| `html`     | `tool_citations`, `tool_bibliography`, and `methods_description` when a template is given |
| `bibtex`   | `bibtex`                                                                                  |
| `csl_json` | `csl_json`                                                                                |

`html` is the default. An unknown format, or a template without `html`, throws an `IllegalArgumentException`.

`bibtex` and `csl_json` hold one entry per tool, built from each entry's `info` (set by `citationsOnTheFly()`, `processCitationsFromTopic()` and `generateModuleToolCitation()`) and `version` (set by `citationsOnTheFly()` only).

- **BibTeX:** `@article` when `publication.title` and `publication.source` are set, else `@misc`. Fields: `author`, `title`, `journal`, `year`, `doi`, `url`, `version`.
- **CSL-JSON:** for Zotero, Pandoc and citeproc. Type `article-journal` or `software`, with `title`, `container-title`, `author`, `issued`, `DOI`, `URL`, `version`.

Authors are copied verbatim from `meta.yml` (a BibTeX `{{...}}` literal, a CSL `literal` name), so citation styles do not abbreviate them.

```nextflow
include { citationsOnTheFly; citationOutputs } from 'plugin/nf-core-utils'

workflow {
    def meta_yml_paths = files("${projectDir}/modules/**/meta.yml").collect { f -> f.toString() }

    ch_citations = channel.topic('versions')
        .toList()
        .map { versions ->
            citationOutputs(
                citationsOnTheFly(versions, meta_yml_paths),
                ['html', 'bibtex', 'csl_json'],
                file("${projectDir}/assets/methods_description_template.yml")
            )
        }

    // MultiQC methods section
    ch_multiqc_files = ch_multiqc_files.mix(
        ch_citations.map { outputs -> outputs.methods_description }.collectFile(name: 'methods_description_mqc.yaml')
    )

    // Reference-manager files
    ch_citations
        .flatMap { outputs -> [['citations.bib', outputs.bibtex], ['citations.json', outputs.csl_json]] }
        .collectFile(storeDir: "${params.outdir}/pipeline_info") { name, text -> [name, text] }
}
```

---

#### `toolsFromVersionsTopic()`

**Description:**
Extracts the unique, sorted set of tool names that actually executed from collected `versions` topic data. Useful on its own — e.g. for conditional logic like `if ('star' in tools)`. (`citationsOnTheFly()` uses it internally to select which citations to emit.)

**Function Signature:**

```groovy
List<String> toolsFromVersionsTopic(List topicVersions)
```

**Parameters:**

- `topicVersions` (List): Collected `versions` topic data — `[process, tool, version]` tuples (collect with `.toList()`).

**Returns:**

- `List<String>`: Sorted, de-duplicated tool names. Empty list for `null`/empty input.

**Usage Example:**

```nextflow
include { toolsFromVersionsTopic } from 'plugin/nf-core-utils'

workflow {
    channel.topic('versions')
        .toList()
        .map { versions -> toolsFromVersionsTopic(versions) }
        .view { tools -> "Tools that ran: ${tools.join(', ')}" }
}
```

:::tip{title="meta.yml vs. the citation topic"}
`citationsOnTheFly()` keeps the citation source as canonical `meta.yml` files while gating by what ran — no need to add `getCitation(...)` emissions to every module. If you have already migrated modules to emit a dedicated `citation` topic, the [`getCitation` + `autoToolCitationText`](#getcitation) path below is equivalent and needs no `meta.yml` paths.
:::

---

### Modern Approach Functions (Recommended)

### `getCitation()`

**Description:**  
Extracts citation information from a module's meta.yml file and formats it for topic channel emission. This is the modern approach for automatic citation collection that only includes citations for tools that actually execute.

**Function Signature:**

```groovy
List getCitation(String metaYmlPath)
```

**Parameters:**

- `metaYmlPath` (String): Path to the module's local meta.yml file, typically `"${moduleDir}/meta.yml"`

**Returns:**

- `List`: One `[module, tool, citation_data]` tuple per tool in the meta.yml, ready for topic channel emission. Empty list (with a warning) if the file is missing or unreadable.

**Usage in Process:**

```nextflow
include { getCitation } from 'plugin/nf-core-utils'

process FASTQC {
    input:
    tuple val(meta), path(reads)

    output:
    tuple val(meta), path("*.html"), emit: html
    tuple val(meta), path("*.zip"), emit: zip
    tuple val("${task.process}"), val('fastqc'), eval('fastqc --version | sed "s/FastQC v//"'), topic: versions
    val citation_data, topic: citation

    script:
    citation_data = getCitation("${moduleDir}/meta.yml")
    """
    fastqc --quiet --threads ${task.cpus} ${reads}
    """
}
```

**Pipeline Integration:**

`.toList()` emits once, with an empty list when no process ran, and the auto functions accept the per-process lists of tuples as collected.

```nextflow
include { autoToolCitationText; autoToolBibliographyText } from 'plugin/nf-core-utils'

workflow {
    FASTQC(ch_reads)

    ch_citations = channel.topic('citation').toList()
    ch_citation_text = ch_citations
        .map { citations -> autoToolCitationText(citations) }
        .collectFile(name: 'citations.html', storeDir: "${params.outdir}/pipeline_info")
    ch_bibliography = ch_citations
        .map { citations -> autoToolBibliographyText(citations) }
        .collectFile(name: 'bibliography.html', storeDir: "${params.outdir}/pipeline_info")
}
```

---

### `autoToolCitationText()`

**Description:**  
Automatically generates formatted citation text from topic channel citation data. This processes the automatic citation collection from executed processes, ensuring runtime accuracy. See [`getCitation()`](#getcitation) for usage.

**Function Signature:**

```groovy
String autoToolCitationText(List topicCitations = [])
```

**Parameters:**

- `topicCitations` (List): Citation data collected from topic channel

**Returns:**

- `String`: Formatted citation text for methods descriptions, e.g. `Tools used in the workflow included: fastqc (DOI: 10.…), multiqc (DOI: 10.1093/bioinformatics/btw354).`; `No tools used in the workflow.` for empty input

---

### `autoToolBibliographyText()`

**Description:**  
Automatically generates HTML bibliography from topic channel citation data. Creates bibliography entries for tools that were actually executed. See [`getCitation()`](#getcitation) for usage.

**Function Signature:**

```groovy
String autoToolBibliographyText(List topicCitations = [])
```

**Parameters:**

- `topicCitations` (List): Citation data collected from topic channel

**Returns:**

- `String`: HTML bibliography for MultiQC reports; `No bibliography entries found.` for empty input

---

### Legacy Functions (Still Supported)

### `generateModuleToolCitation()`

**Description:**  
Extracts citation information from a module's meta.yml file using the traditional approach. This function processes all tools in the meta.yml regardless of execution.

**Function Signature:**

```groovy
Map generateModuleToolCitation(Object metaFilePath)
```

**Parameters:**

- `metaFilePath` (String|File): Path to the local meta.yml file. Throws if the file does not exist.

**Returns:**

- `Map`: Citations map with tool names as keys and citation/bibliography data as values, e.g. `[fastqc: [citation: 'fastqc (DOI: …)', bibliography: '<li>…</li>']]`

---

### `toolCitationText()`

**Description:**  
Generates formatted citation text from collected citations map. [`citationOutputs()`](#citation-outputs-html-bibtex-and-csl-json) returns it as `tool_citations`, with the bibliography and the other formats; prefer it in pipelines.

**Function Signature:**

```groovy
String toolCitationText(Map collectedCitations)
```

**Parameters:**

- `collectedCitations` (Map): Map of tool citations, e.g. from `generateModuleToolCitation()` or `citationsOnTheFly()`

**Returns:**

- `String`: Formatted citation text for use in methods descriptions; `No tools used in the workflow.` for an empty map

**Usage Example:**

```nextflow
include { generateModuleToolCitation; citationOutputs } from 'plugin/nf-core-utils'

workflow {
    def citations = ['fastqc', 'multiqc'].collectEntries { tool ->
        generateModuleToolCitation("${projectDir}/modules/nf-core/${tool}/meta.yml")
    }

    def outputs = citationOutputs(citations)
    log.info(outputs.tool_citations)
    // Tools used in the workflow included: fastqc (DOI: …), multiqc (DOI: 10.1093/bioinformatics/btw354).
    log.info(outputs.tool_bibliography)
}
```

---

### `toolBibliographyText()`

**Description:**  
Generates HTML bibliography from collected citations. [`citationOutputs()`](#citation-outputs-html-bibtex-and-csl-json) returns it as `tool_bibliography`. See [`toolCitationText()`](#toolcitationtext) for usage.

**Function Signature:**

```groovy
String toolBibliographyText(Map collectedCitations)
```

**Parameters:**

- `collectedCitations` (Map): Map of tool citations

**Returns:**

- `String`: HTML bibliography for MultiQC reports; `No bibliography entries found.` for an empty map

---

### `methodsDescriptionText()`

**Description:**  
Generates methods description text using collected citations and a MultiQC methods template. This function substitutes citation and bibliography variables into the template. [`citationOutputs()`](#citation-outputs-html-bibtex-and-csl-json) returns it as `methods_description` when given a template; call `methodsDescriptionText()` directly to pass extra `meta` variables.

**Function Signature:**

```groovy
String methodsDescriptionText(Object mqcMethodsYaml, Map collectedCitations = [:], Map meta = [:])
```

**Parameters:**

- `mqcMethodsYaml` (Path or String): MultiQC methods YAML template file, local or remote (`s3://`, `gs://`, `az://`). Pass a remote file as a `Path` or as its full URI string: `Path.toString()` drops the scheme of a remote path.
- `collectedCitations` (Map, optional): Map containing all tool citations
- `meta` (Map, optional): Additional template variables. `workflow`, `manifest_map`, `doi_text`, `nodoi_text`, `tool_citations` and `tool_bibliography` are filled in when absent; `tool_citations` and `tool_bibliography` come from `collectedCitations`.

**Returns:**

- `String`: Formatted methods description HTML

**Usage Example:**

```nextflow
include { collectCitationsFromFiles; methodsDescriptionText } from 'plugin/nf-core-utils'

workflow {
    def citations = collectCitationsFromFiles(["${projectDir}/modules/nf-core/fastqc/meta.yml"])
    def methods_yaml = file("${projectDir}/assets/methods_description_template.yml")

    channel.of(methodsDescriptionText(methods_yaml, citations))
        .collectFile(name: 'methods_description_mqc.yaml', storeDir: "${params.outdir}/pipeline_info")
}
```

**Template Example (`methods_description_template.yml`):**

```yaml
id: "custom-methods"
section_name: "nf-core/rnaseq Methods Description"
description: |
  <h4>Methods</h4>
  <p>Data was processed using nf-core/rnaseq v${workflow.manifest.version}.
  ${tool_citations}</p>

  <h4>References</h4>
  ${tool_bibliography}
```

---

### `collectCitationsFromFiles()`

**Description:**  
Collects citations from multiple meta.yml files (legacy batch processing approach). Files that cannot be read are logged and skipped. See [`methodsDescriptionText()`](#methodsdescriptiontext) for usage.

**Function Signature:**

```groovy
Map collectCitationsFromFiles(List<String> metaFilePaths)
```

**Parameters:**

- `metaFilePaths` (List\<String\>): List of paths to module meta.yml files

**Returns:**

- `Map`: Combined citations from all files

---

### Migration Functions

### `processMixedCitationSources()`

**Description:**  
Processes citations from both topic channels (modern approach) and legacy files for progressive migration.

**Function Signature:**

```groovy
Map processMixedCitationSources(List<List> topicCitations, List<String> citationFiles)
```

**Parameters:**

- `topicCitations` (List\<List\>): Flat list of `[module, tool, citation_data]` tuples. `getCitation()` emits one list of tuples per task, so flatten the topic with `.flatMap { c -> c }` before `.toList()`.
- `citationFiles` (List\<String\>): List of meta.yml file paths

**Returns:**

- `Map`: Combined citations from both sources

**Migration Example:**

```nextflow
include { processMixedCitationSources; citationOutputs } from 'plugin/nf-core-utils'

workflow {
    MODERN_PROCESS(ch_input) // emits getCitation() to the citation topic
    LEGACY_PROCESS(ch_input) // no citation emission

    def legacy_files = ["${projectDir}/modules/local/legacy_tool/meta.yml"]

    channel.topic('citation')
        .flatMap { citations -> citations }
        .toList()
        .map { topic_citations -> citationOutputs(processMixedCitationSources(topic_citations, legacy_files)).tool_citations }
        .view()
}
```

---

### `convertMetaYamlToTopicFormat()`

**Description:**  
Converts legacy meta.yml data to new topic channel format for migration purposes.

**Function Signature:**

```groovy
List<List> convertMetaYamlToTopicFormat(String metaFilePath, String moduleName = null)
```

**Parameters:**

- `metaFilePath` (String): Path to meta.yml file
- `moduleName` (String, optional): Name of the module (defaults to the meta.yml parent directory name)

**Returns:**

- `List<List>`: List of `[module, tool, citation_data]` tuples

**Migration Usage:**

```nextflow
include { convertMetaYamlToTopicFormat; processCitationsFromTopic } from 'plugin/nf-core-utils'

workflow {
    def tuples = convertMetaYamlToTopicFormat("${projectDir}/modules/local/legacy_tool/meta.yml", 'LEGACY_TOOL')
    def citations = processCitationsFromTopic(tuples)
}
```

---

### Topic Channel Processing Functions

### `processCitationsFromTopic()`

**Description:**  
Processes citations exclusively from topic channel format (modern approach).

**Function Signature:**

```groovy
Map processCitationsFromTopic(List<List> topicData)
```

**Parameters:**

- `topicData` (List\<List\>): Flat list of `[module, tool, citation_data]` tuples (flatten collected `citation` topic data first, see [`processMixedCitationSources()`](#processmixedcitationsources))

**Returns:**

- `Map`: Processed citations map, ready for [`citationOutputs()`](#citation-outputs-html-bibtex-and-csl-json)

---

### `processCitationsFromFile()`

**Description:**  
Processes citations exclusively from traditional YAML files (legacy approach). Same result as [`collectCitationsFromFiles()`](#collectcitationsfromfiles): files that cannot be read are logged and skipped.

**Function Signature:**

```groovy
Map processCitationsFromFile(List<String> citationFiles)
```

**Parameters:**

- `citationFiles` (List\<String\>): List of file paths to meta.yml files

**Returns:**

- `Map`: Tool citations from files

---

## Topic Channel Migration Guide

### Stage 1: Legacy File-Based Approach

Citations are collected from a fixed list of meta.yml files with [`collectCitationsFromFiles()`](#collectcitationsfromfiles), so tools are cited even if their process did not run.

### Stage 2: Mixed Migration Approach

Migrated modules emit `getCitation()` to the `citation` topic, and [`processMixedCitationSources()`](#processmixedcitationsources) merges them with the meta.yml files of modules not yet migrated.

### Stage 3: Modern Topic Channel Approach

Every module emits citations at runtime, collected with [`autoToolCitationText()` / `autoToolBibliographyText()`](#getcitation). Alternatively, [`citationsOnTheFly()`](#citations-on-the-fly-recommended) gets the same runtime accuracy from the `versions` topic without changing modules.

---

## Best Practices

### 1. Runtime Citation Accuracy

Ensure citations reflect actual tool execution:

```nextflow
include { autoToolCitationText } from 'plugin/nf-core-utils'

workflow {
    if (params.run_fastqc) {
        FASTQC(ch_reads) // only emits a citation if it runs
    }
    if (params.run_multiqc) {
        MULTIQC(ch_reports) // only emits a citation if it runs
    }

    channel.topic('citation')
        .toList()
        .map { citations -> autoToolCitationText(citations) }
        .view()
}
```

### 2. Testing Citation Functions

Test citation processing across migration stages:

```groovy
test("Modern topic channel citations") {
    when {
        def topicCitations = [
            ['FASTQC', 'fastqc', [doi: '10.1093/bioinformatics/btv033', publication: [author: 'Andrews S']]],
            ['MULTIQC', 'multiqc', [doi: '10.1093/bioinformatics/btw354', publication: [author: 'Ewels P']]]
        ]
        def result = autoToolCitationText(topicCitations)
    }

    then {
        assert result.contains('fastqc')
        assert result.contains('multiqc')
        assert result.contains('Andrews')
    }
}

test("Legacy file citations") {
    when {
        def citations = generateModuleToolCitation('test_meta.yml')
        def result = toolCitationText(citations)
    }

    then {
        assert result.contains('Tools used in the workflow')
        assert citations.size() > 0
    }
}

test("Mixed citation processing") {
    when {
        def topicCitations = [['MODERN', 'modern_tool', [doi: 'test']]]
        def legacyFiles = ['test_legacy_meta.yml']
        def result = processMixedCitationSources(topicCitations, legacyFiles)
    }

    then {
        assert result.containsKey('modern_tool')
        // Also contains legacy citations
    }
}
```

---

## Integration Examples

### MultiQC Methods Template Integration

Pass pre-rendered text as the `tool_citations` / `tool_bibliography` template variables:

```nextflow
include { autoToolCitationText; autoToolBibliographyText; methodsDescriptionText } from 'plugin/nf-core-utils'

workflow {
    def methods_yaml = file("${projectDir}/assets/methods_description_template.yml")

    channel.topic('citation')
        .toList()
        .map { citations ->
            methodsDescriptionText(methods_yaml, [:], [
                tool_citations: autoToolCitationText(citations),
                tool_bibliography: autoToolBibliographyText(citations),
            ])
        }
        .collectFile(name: 'methods_description_mqc.yaml', storeDir: "${params.outdir}/pipeline_info")
}
```

---

## Common Issues and Solutions

### Issue: Empty Citation Output

**Problem:**
`channel.topic('citation').collect()` emits nothing when no process emitted a citation, so downstream steps never run.

**Solution:**
Collect with `.toList()`, which emits `[]` in that case; `autoToolCitationText([])` returns `No tools used in the workflow.`. To check what is emitted:

```nextflow
workflow {
    channel.topic('citation').view { c -> "Citation collected: ${c}" }
}
```

### Issue: Missing Module Citations

**Problem:**
Process doesn't emit citations even with `getCitation()` call

**Solution:**
`getCitation()` reads a local `meta.yml` and returns an empty list, with a `meta.yml not found` or `Failed to extract citation` warning in the log, when the file is missing or invalid. Check that `${moduleDir}/meta.yml` exists and has a `tools:` section.

---

## Performance Considerations

### Topic Channel Benefits

- **Runtime Accuracy**: Only tools that execute appear in citations
- **Zero Maintenance**: No manual citation file management
- **Automatic Updates**: Citations update when modules change

For more information on citation management and topic channels, see the [Nextflow documentation](https://www.nextflow.io/docs/latest/channel.html#topic) and [nf-core modules documentation](https://nf-co.re/docs/specifications/components/modules/documentation#documenting-of-all-tools).
