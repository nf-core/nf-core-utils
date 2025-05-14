# NfCorePipelineExtension

This extension provides utility functions for nf-core pipelines that were previously available in the `utils_nfcore_pipeline` subworkflow. **All functions now require the `workflow` object as the first argument, following the style of nf-schema.**

```nextflow
// Import utilities
include { checkConfigProvided; checkProfileProvided; getWorkflowVersion;
          paramsSummaryMultiqc; logColours; completionSummary } from 'plugin/nf-utils'

// Check if a valid configuration was provided
valid_config = checkConfigProvided(workflow)

// Check for profile formatting issues
checkProfileProvided(workflow)

// Get workflow version string
version_str = getWorkflowVersion(workflow)
println "Pipeline version: ${version_str}"

// Get workflow summary for MultiQC
summary_params = [
    'Parameters': [
        input: params.input,
        outdir: params.outdir
    ]
]
def summary_yaml = paramsSummaryMultiqc(workflow, summary_params)

// Use colored terminal output
def colors = logColours(params.monochrome_logs)
println "${colors.green}Processing sample: ${colors.reset}${sample_id}"

// Print completion summary
workflow.onComplete {
    completionSummary(workflow, params.monochrome_logs)
}
```

## Available Functions

### `checkConfigProvided(WorkflowMetadata workflow)`

Warns if a -profile or Nextflow config has not been provided to run the pipeline. Returns a boolean indicating if a valid config was provided.

### `checkProfileProvided(WorkflowMetadata workflow)`

Checks if the -profile option is correctly formatted (no trailing commas) and warns about positional arguments.

### `getWorkflowVersion(WorkflowMetadata workflow)`

Generates a version string for the workflow (e.g., "v1.0.0-g1234567").

### `processVersionsFromYAML(String yaml_file)`

Processes software versions from a YAML file.

### `workflowVersionToYAML(WorkflowMetadata workflow)`

Gets workflow version in YAML format for MultiQC reporting.

### `softwareVersionsToYAML(WorkflowMetadata workflow, Object ch_versions)`

Combines software versions into a unified YAML format for reporting.

### `paramsSummaryMultiqc(WorkflowMetadata workflow, Map summary_params)`

Generates workflow parameter summary for MultiQC reports.

### `logColours(boolean monochrome_logs=true)`

Returns ANSI color codes for terminal output, respecting monochrome setting.

### `getSingleReport(Object multiqc_reports)`

Returns a single report from an object that may be a Path or List.

### `completionSummary(WorkflowMetadata workflow, boolean monochrome_logs=true)`

Prints a formatted summary of the pipeline completion status.
