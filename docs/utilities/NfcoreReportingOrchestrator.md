# NfcoreReportingOrchestrator

Orchestrated comprehensive reporting utilities for nf-core pipelines.

## Overview

The `NfcoreReportingOrchestrator` utility provides high-level orchestration of reporting functionality by coordinating `SoftwareVersionReport` and `NfcoreCitationUtils`. Public extension functions remain stable, while the internal implementation now uses `PipelineExecutionContext` so report generation receives explicit workflow, manifest, and configuration metadata instead of reading global Nextflow session state.

### Key Benefits

- **Unified Interface**: Single entry point for comprehensive pipeline reporting
- **Explicit Context**: Manifest and workflow metadata are passed through `PipelineExecutionContext`, including data used by methods-description templates
- **Coordinated Processing**: Ensures consistent handling of both versions and citations
- **Error Resilience**: Robust error handling with graceful degradation
- **Performance Optimization**: Efficient processing of large datasets
- **Consistent Output**: Standardized report formats across all functions

## Available Functions

### `generateComprehensiveReport()`

**Description:**  
Generates a complete pipeline report including version information, tool citations, bibliography, and methods description. This is the primary function for full pipeline reporting that combines all available reporting capabilities.

**Function Signature:**

```groovy
Map generateComprehensiveReport(
    List<List> topicVersions,
    List<String> legacyVersions = [],
    List<String> metaFilePaths = [],
    Object mqcMethodsYaml = null
)
```

**Parameters:**

- `topicVersions` (List\<List\>): Version data from topic channels in `[process, name, version]` format
- `legacyVersions` (List\<String\>, optional): Legacy `versions.yml` contents or file paths
- `metaFilePaths` (List\<String\>, optional): List of paths to module meta.yml files for citations
- `mqcMethodsYaml` (Path or String, optional): MultiQC methods description template, local or remote (`s3://`, `gs://`, `az://`)

**Returns:**

- `Map`: Comprehensive report containing:
  - `versions_yaml` (String): Complete YAML with all version information
  - `tool_citations` (String): Formatted citation text for methods descriptions
  - `tool_bibliography` (String): HTML bibliography for references
  - `methods_description` (String): Rendered methods description (empty without a template)
  - `citations_map` (Map): Collected citations, keyed by tool

**Usage Example:**

```nextflow
include { generateComprehensiveReport } from 'plugin/nf-core-utils'

workflow {
    main:
    FASTQC(ch_reads)

    def meta_files = [
        "${projectDir}/modules/nf-core/fastqc/meta.yml",
        "${projectDir}/modules/nf-core/multiqc/meta.yml",
    ]
    def methods_template = file("${projectDir}/assets/methods_description_template.yml")

    ch_report = channel.topic('versions')
        .toList()
        .map { versions -> generateComprehensiveReport(versions, [], meta_files, methods_template) }

    ch_report
        .map { report -> report.versions_yaml }
        .collectFile(name: 'software_versions.yml', storeDir: "${params.outdir}/pipeline_info")
    ch_report
        .map { report -> report.methods_description }
        .collectFile(name: 'methods_description_mqc.yaml', storeDir: "${params.outdir}/pipeline_info")
}
```

---

### `generateVersionReport()`

**Description:**  
Generates a version-only report when citations are not needed or handled separately. This function focuses exclusively on version aggregation and formatting.

**Function Signature:**

```groovy
Map generateVersionReport(List<List> topicVersions, List<String> legacyVersions = [])
```

**Parameters:**

- `topicVersions` (List\<List\>): Version data from topic channels in `[process, name, version]` format
- `legacyVersions` (List\<String\>, optional): Legacy `versions.yml` contents or file paths

**Returns:**

- `Map`: Version report containing:
  - `versions_yaml` (String): Complete YAML with all version information, including the `Workflow` section

**Usage Example:**

```nextflow
include { generateVersionReport } from 'plugin/nf-core-utils'

workflow {
    main:
    channel.topic('versions')
        .toList()
        .map { versions -> generateVersionReport(versions).versions_yaml }
        .collectFile(name: 'software_versions.yml', storeDir: "${params.outdir}/pipeline_info")
}
```

---

### `generateCitationReport()`

**Description:**  
Generates a citation-only report when versions are handled separately. This function focuses exclusively on citation extraction, processing, and methods description generation.

**Function Signature:**

```groovy
Map generateCitationReport(List<String> metaFilePaths, Object mqcMethodsYaml = null)
```

**Parameters:**

- `metaFilePaths` (List\<String\>): List of paths to module meta.yml files
- `mqcMethodsYaml` (Path or String, optional): MultiQC methods description template, local or remote (`s3://`, `gs://`, `az://`)

**Returns:**

- `Map`: Citation report containing:
  - `tool_citations` (String): Formatted citation text for methods descriptions
  - `tool_bibliography` (String): HTML bibliography for references
  - `methods_description` (String): Methods description with citation integration (empty without a template)
  - `citations_map` (Map): Collected citations, keyed by tool

**Usage Example:**

```nextflow
include { generateCitationReport } from 'plugin/nf-core-utils'

workflow {
    main:
    def meta_files = [
        "${projectDir}/modules/nf-core/fastqc/meta.yml",
        "${projectDir}/modules/nf-core/multiqc/meta.yml",
        "${projectDir}/modules/local/custom_tool/meta.yml",
    ]
    def report = generateCitationReport(meta_files, file("${projectDir}/assets/methods_description_template.yml"))

    channel.value(report.methods_description)
        .collectFile(name: 'methods_description_mqc.yaml', storeDir: "${params.outdir}/pipeline_info")
}
```

---

## Orchestration Benefits

### Unified Processing

The orchestrator provides several advantages over calling individual utility functions:

```nextflow
// Instead of separate utility calls:
// processMixedVersionSources(topicVersions, legacyVersions)
// collectCitationsFromFiles(metaFiles)
// methodsDescriptionText(methodsTemplate, citations)

// Single orchestrated call:
// generateComprehensiveReport(topicVersions, legacyVersions, metaFiles, methodsTemplate)
// returns versions_yaml, tool_citations, tool_bibliography, methods_description, citations_map
```

### Error Handling Coordination

The report functions do not throw on bad inputs: a `meta.yml` that cannot be read is logged as a warning and skipped, and a methods template that fails to render yields an empty `methods_description`.

---

## Integration Examples

### Complete Pipeline Integration

```nextflow
include { generateComprehensiveReport } from 'plugin/nf-core-utils'

workflow {
    main:
    PREPROCESSING(ch_samples)
    QUALITY_CONTROL(PREPROCESSING.out.processed)

    def meta_files = [
        "${projectDir}/modules/nf-core/fastqc/meta.yml",
        "${projectDir}/modules/nf-core/multiqc/meta.yml",
    ]
    def methods_template = file("${projectDir}/assets/methods_description_template.yml")

    ch_report = channel.topic('versions')
        .toList()
        .map { versions -> generateComprehensiveReport(versions, [], meta_files, methods_template) }

    ch_versions_mqc = ch_report
        .map { report -> report.versions_yaml }
        .collectFile(name: 'software_versions_mqc.yml')
    ch_methods_mqc = ch_report
        .map { report -> report.methods_description }
        .collectFile(name: 'methods_description_mqc.yaml')

    MULTIQC(
        QUALITY_CONTROL.out.reports
            .mix(ch_versions_mqc, ch_methods_mqc)
            .collect()
    )
}
```

---

## Best Practices

### 1. Choose Appropriate Reporting Function

```nextflow
// Use comprehensive reporting for full pipelines
// generateComprehensiveReport(versions, [], metaFiles, methodsTemplate)

// Use specialized reporting for focused needs
// generateVersionReport(versions)                  // Version tracking only
// generateCitationReport(metaFiles, methodsTemplate) // Publication prep
```

### 2. Testing Orchestrated Functions

```groovy
test("Comprehensive reporting integration") {
    when {
        def versions = [
            ['FASTQC', 'fastqc', '0.11.9'],
            ['MULTIQC', 'multiqc', '1.12']
        ]
        def citations = ['modules/nf-core/fastqc/meta.yml']
        def methods = 'test_methods_template.yml'

        def report = generateComprehensiveReport(versions, [], citations, methods)
    }

    then {
        assert report.containsKey('versions_yaml')
        assert report.containsKey('tool_citations')
        assert report.containsKey('tool_bibliography')
        assert report.containsKey('methods_description')

        assert report.versions_yaml.contains('FASTQC')
        assert report.tool_citations.contains('fastqc')
        assert report.tool_bibliography.contains('<li>')
    }
}

test("Graceful degradation with missing data") {
    when {
        def report = generateComprehensiveReport([], [], [], null)
    }

    then {
        assert report.versions_yaml.contains('Workflow:')
        assert report.tool_citations == "No tools used in the workflow."
        assert report.tool_bibliography == "No bibliography entries found."
    }
}
```

---

## Common Issues and Solutions

### Issue: Inconsistent Report Components

**Problem:**
Different utility functions produce incompatible output formats

**Solution:**
Use `generateComprehensiveReport()`: all components are built from the same versions and citations.

### Issue: Missing Template Variables

**Problem:**
Methods description template has undefined variables

**Solution:**
The template is rendered with these variables only: `workflow`, `manifest_map`, `doi_text`, `nodoi_text`, `tool_citations` and `tool_bibliography`. Check the template does not reference any other.

---

## Performance Considerations

### Orchestration Efficiency

- **Reduced Function Calls**: Single orchestrated call vs multiple utility calls
- **Coordinated Processing**: Optimized data flow between version and citation processing
- **Memory Management**: Efficient handling of large datasets through coordinated processing
- **Error Recovery**: Centralized error handling reduces redundant processing

For more information on orchestrated reporting and utility coordination, see the individual utility documentation and [nf-core reporting best practices](https://nf-co.re/docs/developing/components/automated-methods-text).
