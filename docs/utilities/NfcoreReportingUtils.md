# MultiQC Integration Tutorial

MultiQC is the cornerstone of nf-core pipeline reporting, and proper integration requires structured data formatting. This tutorial teaches you how to create professional pipeline reports using the `NfcoreReportingUtils` utility.

## 1. Overview

MultiQC transforms scattered log files and statistics into comprehensive, interactive HTML reports. However, it needs properly formatted input data to work effectively. The `NfcoreReportingUtils` utility bridges this gap by:

- Converting pipeline parameters into MultiQC-compatible YAML format
- Creating structured workflow summaries with metadata

### 1.1. Why Structured Reporting Matters

Raw pipeline outputs are often scattered across multiple files and formats. Users struggle to find:

- Which parameters were used in the analysis
- What software versions were executed
- How long each step took and what resources were used
- Summary statistics and quality metrics

Structured reporting solves these challenges by consolidating information into searchable, interactive dashboards.

## Available Functions

### `paramsSummaryMultiqc()`

**Description:**  
Generates a YAML-formatted string from a map of summary parameters for MultiQC workflow summary integration. This function creates properly structured YAML that MultiQC can parse and display in its workflow summary section.

**Function Signature:**

```groovy
String paramsSummaryMultiqc(Map summaryParams)
```

**Parameters:**

- `summaryParams` (Map): Nested map of parameter groups and their parameters

**Returns:**

- `String`: YAML-formatted string suitable for MultiQC workflow summary

**Usage Example:**

```nextflow
include { paramsSummaryMultiqc } from 'plugin/nf-core-utils'
include { FASTQC               } from './modules/nf-core/fastqc'
include { MULTIQC              } from './modules/local/multiqc'

params {
    input: Path
    outdir: Path
    genome: String?
}

workflow {
    main:
    ch_reads = channel.fromPath(params.input).map { fastq -> tuple([id: fastq.simpleName], fastq) }
    FASTQC(ch_reads)

    def summary_params = [
        'Core Nextflow options': [
            revision: workflow.revision,
            runName: workflow.runName,
            profile: workflow.profile,
        ],
        'Input/output options': [
            input: params.input,
            outdir: params.outdir,
        ],
        'Reference genome options': [
            genome: params.genome,
        ],
    ]
    ch_workflow_summary = channel.value(paramsSummaryMultiqc(summary_params))
        .collectFile(name: 'workflow_summary_mqc.yaml')

    MULTIQC(ch_workflow_summary.mix(FASTQC.out.zip.map { _meta, zip -> zip }).collect())
}
```

Each group must map to a Map of parameters: groups render as separate blocks in the MultiQC summary, empty groups are skipped, and null or empty values render as `N/A`.

---

### `workflowSummaryMQC()` and `sectionLogs()`

Static Groovy helpers in `NfcoreReportingUtils`. They are not `@Function`s, so pipelines cannot `include` them.

```groovy
static Map workflowSummaryMQC(Map summary, List nfMetadataList, Map results)
static Map sectionLogs(Map sections, boolean monochrome = false)
```

- `workflowSummaryMQC` returns a Markdown summary of `summary`, the session properties named in `nfMetadataList` and `results`, under the keys `workflow_summary_txt` and `workflow_summary_html` (the same text wrapped in `<pre>`).
- `sectionLogs` returns a Map of section name to its content, with every line prefixed by `-[section name]-` (purple unless `monochrome`).

Unit tests: `src/test/groovy/nfcore/plugin/nfcore/NfcoreReportingUtilsTest.groovy`.

For more information on MultiQC integration and pipeline reporting, see the [MultiQC documentation](https://multiqc.info/) and [nf-core reporting guidelines](https://nf-co.re/docs/developing/components/automated-methods-text).
