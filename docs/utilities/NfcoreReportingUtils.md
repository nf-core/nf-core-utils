# MultiQC Integration Tutorial

MultiQC is the cornerstone of nf-core pipeline reporting, and proper integration requires structured data formatting. This tutorial teaches you how to create professional pipeline reports using the `NfcoreReportingUtils` utility.

## 1. Overview

MultiQC transforms scattered log files and statistics into comprehensive, interactive HTML reports. However, it needs properly formatted input data to work effectively. The `NfcoreReportingUtils` utility bridges this gap by:

- Converting pipeline parameters into MultiQC-compatible YAML format
- Handling complex data structures for professional report presentation

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
static String paramsSummaryMultiqc(Map summaryParams)
```

**Parameters:**

- `summaryParams` (Map): Nested map of parameter groups and their parameters

**Returns:**

- `String`: YAML-formatted string suitable for MultiQC workflow summary

**Usage Example:**

```nextflow
include { paramsSummaryMultiqc } from 'plugin/nf-core-utils'

workflow {
    // Organize parameters into logical groups
    def summary_params = [
        'Core Nextflow options': [
            'revision': workflow.revision ?: 'N/A',
            'runName': workflow.runName,
            'containerEngine': workflow.containerEngine,
            'profile': workflow.profile
        ],
        'Input/output options': [
            'input': params.input ?: 'N/A',
            'outdir': params.outdir ?: 'N/A',
            'publish_dir_mode': params.publish_dir_mode
        ],
        'Reference genome options': [
            'genome': params.genome ?: 'None',
            'fasta': params.fasta ?: 'N/A'
        ]
    ]

    // Generate MultiQC-compatible YAML
    def yaml_content = paramsSummaryMultiqc(summary_params)

    // Write to file for MultiQC consumption
    file("${params.outdir}/pipeline_info/workflow_summary_mqc.yaml").text = yaml_content
}
```

**MultiQC Integration Pattern:**

```nextflow
process MULTIQC {
    input:
    path('*')

    output:
    path("multiqc_report.html"), emit: report

    script:
    // Include workflow summary in MultiQC
    def summary = [
        'Pipeline': workflow.manifest.name,
        'Version': workflow.manifest.version,
        'Run Name': workflow.runName
    ]
    def yaml = paramsSummaryMultiqc(['Workflow Summary': summary])
    """
    echo '$yaml' > workflow_summary_mqc.yaml
    multiqc -f .
    """
}
```

---

## MultiQC Integration Best Practices

### 1. Parameter Organization

Structure parameters logically for better MultiQC display:

```nextflow
// GOOD - Grouped structure
def summary_params = [
    'Core Nextflow options': [
        'runName': workflow.runName,
        'profile': workflow.profile,
        'revision': workflow.revision
    ],
    'Input/output options': [
        'input': params.input,
        'outdir': params.outdir
    ],
    'Analysis parameters': [
        'genome': params.genome,
        'aligner': params.aligner
    ]
]

// AVOID - Flat structure
def summary_params = [
    runName: workflow.runName,
    input: params.input,
    genome: params.genome
]
```

### 2. Null Value Handling

Always provide fallback values for potentially null parameters:

```nextflow
def summary_params = [
    'Core Options': [
        'revision': workflow.revision ?: 'N/A',
        'runName': workflow.runName ?: 'Unknown',
        'profile': workflow.profile ?: 'default'
    ]
]
```

### 3. MultiQC File Integration

Integrate reporting utilities with MultiQC processes:

```nextflow
process MULTIQC {
    input:
    path('*')

    output:
    path("multiqc_report.html"), emit: report

    script:
    def summary = ['Pipeline': workflow.manifest.name]
    def workflow_yaml = paramsSummaryMultiqc(['Workflow': summary])
    """
    echo '$workflow_yaml' > workflow_summary_mqc.yaml
    multiqc -f .
    """
}
```

---

## Testing Strategies

### Unit Testing

```groovy
test("Parameter summary YAML generation") {
    when {
        def params = [
            'Core Options': [
                'runName': 'test_run',
                'profile': 'docker'
            ]
        ]
        def yaml = paramsSummaryMultiqc(params)
    }

    then {
        assert yaml.contains('Core Options:')
        assert yaml.contains('runName: test_run')
        assert yaml.contains('profile: docker')
    }
}
```

---

## Common Issues and Solutions

### Issue: YAML Formatting Problems

**Problem:**
Generated YAML has formatting issues for MultiQC

**Solution:**

```nextflow
// Ensure proper parameter structure
def params = [
    'Group Name': [  // Use quoted keys for complex names
        'param1': value1?.toString() ?: 'N/A',  // Handle null values
        'param2': value2?.toString() ?: 'Unknown'
    ]
]

def yaml = paramsSummaryMultiqc(params)
```

---

## Performance Considerations

### Efficient Parameter Processing

```nextflow
// Optimize large parameter sets
workflow LARGE_PARAM_PROCESSING {
    // Process parameters in chunks for very large pipelines
    def param_chunks = large_params.collate(100)

    def yaml_sections = []
    param_chunks.eachWithIndex { chunk, index ->
        def chunk_yaml = paramsSummaryMultiqc(["Section_${index}": chunk])
        yaml_sections.add(chunk_yaml)
    }

    // Combine sections
    def final_yaml = yaml_sections.join('\n---\n')
}
```

For more information on MultiQC integration and pipeline reporting, see the [MultiQC documentation](https://multiqc.info/) and [nf-core reporting guidelines](https://nf-co.re/docs/developing/components/automated-methods-text).
