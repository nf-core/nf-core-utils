# NfcoreVersionUtils

Version management and topic channel utilities for nf-core pipelines.

## Overview

[`collectVersions()`](#collectversions) merges versions from topic tuples, `versions.yml` files and YAML in one call, so a pipeline can move modules to the `versions` topic one at a time.

## Available Functions

### `getWorkflowVersion()`

**Description:**  
Generates a formatted version string for the workflow, combining version information with git commit details when available. This is the primary function for getting workflow version information.

**Function Signature:**

```groovy
String getWorkflowVersion(String version = null, String commitId = null)
```

**Parameters:**

- `version` (String, optional): Explicit version string to use (defaults to workflow.manifest.version)
- `commitId` (String, optional): Explicit commit ID to use (defaults to workflow.commitId when no `version` is given, so runs from a repository get the `-g<sha>` suffix)

**Returns:**

- `String`: Formatted version string (e.g., `v1.2.3-gabcdef1`)

**Usage Example:**

```nextflow
include { getWorkflowVersion } from 'plugin/nf-core-utils'

workflow {
    log.info "Starting ${workflow.manifest.name} ${getWorkflowVersion()}"
    log.info "Custom version: ${getWorkflowVersion('2.0.0', 'abc123')}"
}
```

---

### `collectVersions()`

**Description:**  
Merges software versions from any mix of sources into one YAML string, with the workflow name, version and Nextflow version. This is the recommended function for version reports.

**Function Signature:**

```groovy
String collectVersions(Object input, Object nextflowVersion = null)
```

**Parameters:**

- `input` (Object): one entry, or a list of entries of any of these types:
  - Topic tuples: `[process, tool, version]`
  - `versions.yml` files, as `Path`, `File`, or a path string. Remote files (`s3://`) are read through their file system, given as a `Path` or a full URI string
  - YAML strings
  - Maps of `tool -> version`, or of `process -> [tool -> version]`
- `nextflowVersion` (String/VersionNumber, optional): Nextflow version to report instead of the running one

**Returns:**

- `String`: YAML with processes and tools sorted alphabetically, then the `Workflow` block

**Usage Example:**

```nextflow
include { collectVersions } from 'plugin/nf-core-utils'

workflow {
    // Modules emit tuples, or versions.yml files, on the versions topic
    channel.topic('versions')
        .toList()
        .map { versions -> collectVersions(versions) }
        .collectFile(name: 'software_versions.yml', storeDir: "${params.outdir}/pipeline_info")
}
```

Use `toList()`, not `collect()`: `collect()` flattens the tuples into one list, and emits nothing when no process sent versions. `toList()` keeps each tuple, and emits an empty list, so the report still has the `Workflow` block.

**With a custom Nextflow version:**

```nextflow
collectVersions(versions, '24.10.0')
```

**Output Format:**

Processes and tools are sorted alphabetically, then the `Workflow` block follows:

<!-- prettier-ignore -->
```yaml
FASTQC:
  fastqc: 0.12.1
SAMTOOLS:
  samtools: '1.17'
Software:
  multiqc: '1.15'
Workflow:
    nf-core/mypipeline: v1.0.0
    Nextflow: 24.10.0
```

- Process names keep only their last component: `NFCORE_RNASEQ:TRIMGALORE:FASTQC` becomes `FASTQC`
- Tool names keep only their last component: `tool:fastqc` becomes `fastqc`
- Versions without a process (flat YAML or maps) go under `Software`
- A tool reported twice keeps the last value
- A missing file, or an entry that fails to parse, is skipped with a warning

---

### `softwareVersionsToYAML()`

**Deprecated:** use [`collectVersions()`](#collectversions). It delegates to `collectVersions()`, and takes the same inputs.

**Function Signature:**

```groovy
Object softwareVersionsToYAML(Object versions, Map options = [:])
Object softwareVersionsToYAML(softwareVersions: versions, nextflowVersion: version)
```

Given a channel, it collects it with `toList()` and returns a channel emitting the YAML. Given anything else, it returns the YAML string. `options.nextflowVersion` overrides the reported Nextflow version.

---

### `processMixedVersionSources()`

**Deprecated:** use [`collectVersions()`](#collectversions).

```groovy
String processMixedVersionSources(List<List> topicVersions, List<String> versionsFiles)
```

---

### `convertLegacyYamlToEvalSyntax()`

**Description:**  
Converts traditional YAML version content to the new eval syntax format for topic channels. Useful for migrating existing version data or integrating legacy systems.

**Function Signature:**

```groovy
List<List> convertLegacyYamlToEvalSyntax(String yamlContent, String processName = 'LEGACY')
```

**Parameters:**

- `yamlContent` (String): YAML content from legacy versions.yml files
- `processName` (String, optional): Process name to assign (defaults to 'LEGACY')

**Returns:**

- `List<List>`: List of `[process, name, version]` tuples ready for topic channel processing

**Usage Example:**

```nextflow
include { convertLegacyYamlToEvalSyntax } from 'plugin/nf-core-utils'

workflow {
    def legacyYaml = '''
    FASTQC:
        fastqc: 0.11.9
    MULTIQC:
        multiqc: 1.12
        python: 3.9.0
    '''.stripIndent()

    def topicData = convertLegacyYamlToEvalSyntax(legacyYaml, 'CONVERTED_LEGACY')
    // [['CONVERTED_LEGACY', 'fastqc', '0.11.9'], ['CONVERTED_LEGACY', 'multiqc', '1.12'], ...]

    ch_converted_versions = channel.fromList(topicData)
}
```

---

### `generateYamlFromEvalSyntax()`

**Description:**  
Converts topic channel eval syntax data back to YAML format for reporting and MultiQC integration. Essential for generating traditional version reports from modern topic channels.

**Function Signature:**

```groovy
String generateYamlFromEvalSyntax(List<List> evalData, boolean includeWorkflow = true)
```

**Parameters:**

- `evalData` (List\<List\>): Topic channel data in `[process, name, version]` format
- `includeWorkflow` (boolean, optional): Include workflow version information (defaults to true)

**Returns:**

- `String`: YAML string suitable for MultiQC and reporting

**Usage Example:**

```nextflow
include { generateYamlFromEvalSyntax } from 'plugin/nf-core-utils'

workflow {
    ch_versions_yaml = channel.topic('versions')
        .toList()
        .map { versions -> generateYamlFromEvalSyntax(versions) }
        .collectFile(name: 'software_versions.yml', storeDir: "${params.outdir}/pipeline_info")

    MULTIQC(ch_multiqc_files.mix(ch_versions_yaml).collect())
}
```

---

### `processVersionsFromTopic()`

**Deprecated:** use [`collectVersions()`](#collectversions). It returns a flat `tool: version` map, without processes.

```groovy
String processVersionsFromTopic(List<List> topicData)
```

---

### `processVersionsFromFile()`

**Deprecated:** use [`collectVersions()`](#collectversions). It reads local files only: pass `versions.yml` `Path`s to `collectVersions()` instead, so remote files (`s3://`) are read too.

```groovy
String processVersionsFromFile(List<String> versionsFiles)
```

---

### `workflowVersionToChannel()`

**Description:**  
Converts workflow metadata to topic channel format, enabling consistent version handling across all pipeline components. Useful for including workflow version in topic channel processing.

**Function Signature:**

```groovy
List<List> workflowVersionToChannel()
```

**Parameters:**

- None (uses session context automatically)

**Returns:**

- `List<List>`: Workflow information in `[process, name, version]` format

**Usage Example:**

```nextflow
include { workflowVersionToChannel; generateYamlFromEvalSyntax } from 'plugin/nf-core-utils'

workflow {
    ch_versions_yaml = channel.topic('versions')
        .toList()
        .map { versions -> generateYamlFromEvalSyntax(versions + workflowVersionToChannel(), false) }
}
```

---

## Migrating to the versions topic

`collectVersions()` takes topic tuples and `versions.yml` files in the same list, so modules can move to the `versions` topic one at a time:

```nextflow
include { collectVersions } from 'plugin/nf-core-utils'

workflow {
    MODERN_PROCESS(input_modern)   // emits [process, tool, version] on the versions topic
    LEGACY_PROCESS(input_legacy)   // emits versions.yml

    channel.topic('versions')
        .mix(LEGACY_PROCESS.out.versions)
        .toList()
        .map { versions -> collectVersions(versions) }
        .collectFile(name: 'software_versions.yml', storeDir: "${params.outdir}/pipeline_info")
}
```

Pass the `versions.yml` files as `Path`s. `Path.toString()` drops the scheme of a remote path (`s3://bucket/x` becomes `/bucket/x`), so the file is not found.

## Common issues

- **No `software_versions.yml`:** the versions were collected with `collect()`, which emits nothing when no process sent versions. Use `toList()`.
- **Tools missing from the report:** a missing file, or an entry that fails to parse, is skipped with a warning in `.nextflow.log`.

For more on topic channels, see the [Nextflow documentation](https://www.nextflow.io/docs/latest/channel.html#topic) and [nf-core module specifications](https://nf-co.re/docs/specifications/components/modules/general#emission-of-versions).
