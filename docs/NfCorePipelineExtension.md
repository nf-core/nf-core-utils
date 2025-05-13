# NfCorePipelineExtension

This extension provides utility functions for nf-core pipelines that were previously available in the `utils_nfcore_pipeline` subworkflow:

```nextflow
// Import utilities
include { checkConfigProvided; checkProfileProvided; getWorkflowVersion;
          paramsSummaryMultiqc; logColours; completionSummary } from 'plugin/nf-utils'

// Check if a valid configuration was provided
valid_config = checkConfigProvided()

// Check for profile formatting issues
checkProfileProvided(workflow.commandLine.tokenize())

// Get workflow version string
version_str = getWorkflowVersion()
println "Pipeline version: ${version_str}"

// Get workflow summary for MultiQC
summary_params = [
    'Parameters': [
        input: params.input,
        outdir: params.outdir
    ]
]
def summary_yaml = paramsSummaryMultiqc(summary_params)

// Use colored terminal output
def colors = logColours(params.monochrome_logs)
println "${colors.green}Processing sample: ${colors.reset}${sample_id}"

// Print completion summary
workflow.onComplete {
    completionSummary(params.monochrome_logs)
}
```

## Available Functions

### `checkConfigProvided()`

Warns if a -profile or Nextflow config has not been provided to run the pipeline. Returns a boolean indicating if a valid config was provided.

### `checkProfileProvided(List nextflow_cli_args)`

Checks if the -profile option is correctly formatted (no trailing commas) and warns about positional arguments.

### `getWorkflowVersion()`

Generates a version string for the workflow (e.g., "v1.0.0-g1234567").

### `processVersionsFromYAML(String yaml_file)`

Processes software versions from a YAML file.

### `workflowVersionToYAML()`

Gets workflow version in YAML format for MultiQC reporting.

### `softwareVersionsToYAML(Object ch_versions)`

Combines software versions into a unified YAML format for reporting.

### `paramsSummaryMultiqc(Map summary_params)`

Generates workflow parameter summary for MultiQC reports.

### `logColours(boolean monochrome_logs=true)`

Returns ANSI color codes for terminal output, respecting monochrome setting.

### `getSingleReport(Object multiqc_reports)`

Returns a single report from an object that may be a Path or List.

### `completionSummary(boolean monochrome_logs=true)`

Prints a formatted summary of the pipeline completion status.
