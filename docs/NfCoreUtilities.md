# nf-core Utility Functions

This documentation covers the utility functions provided by the nf-utils plugin for nf-core pipelines.

## Import

```nextflow
include { checkConfigProvided; completionEmail; logColours; paramsSummaryMultiqc; 
          completionSummary; imNotification; getWorkflowVersion } from 'plugin/nf-utils'
```

## Main Facade Class

### NfcorePipelineUtils

The main utility class that provides a facade to specialized utility classes.

## Functions

### Configuration and Validation

#### checkConfigProvided

Checks if a custom config or profile has been provided, logs a warning if not.

**Returns:**
- `true` if config is valid, `false` otherwise

**Example:**
```nextflow
if (!checkConfigProvided()) {
    log.warn "No custom configuration provided! Please provide a profile or custom config."
}
```

#### checkProfileProvided

Checks if the profile string is valid and warns about positional arguments.

**Parameters:**
- `args` (Array): The command line arguments

**Example:**
```nextflow
checkProfileProvided(args)
```

### Workflow Information

#### getWorkflowVersion

Generate workflow version string from session manifest.

**Returns:**
- Version string for the workflow

**Example:**
```nextflow
version_string = getWorkflowVersion()
log.info "Pipeline version: ${version_string}"
```

### Reporting and MultiQC

#### paramsSummaryMultiqc

Generate workflow summary for MultiQC.

**Parameters:**
- `summaryParams` (Map): Map of parameter groups and their parameters

**Returns:**
- YAML formatted string for MultiQC

**Example:**
```nextflow
def summary = [:]
summary['Run Name'] = workflow.runName
summary['Output Dir'] = params.outdir
def yaml_file = paramsSummaryMultiqc([Summary: summary])
```

#### workflowSummaryMQC

Create workflow summary template for MultiQC.

**Parameters:**
- `summary` (Map): Map of parameters
- `nfMetadataList` (List): List of metadata fields to include
- `results` (Map): Map of pipeline results

**Returns:**
- Map with HTML and text summaries for MultiQC

**Example:**
```nextflow
def summary = [Run_id: workflow.runName, User: workflow.userName]
def metadata = ['version', 'start', 'complete', 'duration']
def results = [Reads_Processed: '1,000,000', Failed_QC: '0.1%']
def mqc_summary = workflowSummaryMQC(summary, metadata, results)
```

### Notification and Logging

#### logColours

ANSI colour codes used for terminal logging.

**Parameters:**
- `monochrome_logs` (Boolean, default: `true`): Whether to use monochrome logs

**Returns:**
- Map of colour codes

**Example:**
```nextflow
def colors = logColours(params.monochrome_logs)
log.info "${colors.green}Starting pipeline${colors.reset}"
```

#### sectionLogs

Generate summary logs for each section of a pipeline.

**Parameters:**
- `sections` (Map): Map of section names with their log messages
- `monochrome` (Boolean, default: `false`): Whether to use colors in logs

**Returns:**
- Map of colored section logs

**Example:**
```nextflow
def sections = [
    FastQC: 'Processed 10 samples',
    Alignment: 'Aligned to reference genome',
    MultiQC: 'Report generated'
]
def logs = sectionLogs(sections, params.monochrome_logs)
log.info logs.FastQC
```

#### completionSummary

Print pipeline summary on completion.

**Parameters:**
- `monochrome_logs` (Boolean, default: `true`): Whether to use monochrome logs

**Example:**
```nextflow
workflow.onComplete {
    completionSummary(params.monochrome_logs)
}
```

#### completionEmail

Construct and send completion email.

**Parameters:**
- `summary_params` (Map): Map of summary parameters
- `email` (String): Email address
- `email_on_fail` (String): Email address for failures only
- `plaintext_email` (Boolean): Whether to send plaintext email
- `outdir` (String): Output directory
- `monochrome_logs` (Boolean, default: `true`): Whether to use monochrome logs
- `multiqc_report` (Path|List, optional): MultiQC report file

**Example:**
```nextflow
workflow.onComplete {
    def summary = [:]
    summary['Run Name'] = workflow.runName
    summary['Output Dir'] = params.outdir
    
    completionEmail(
        [Summary: summary], 
        params.email, 
        params.email_on_fail, 
        params.plaintext_email,
        params.outdir,
        params.monochrome_logs,
        multiqc_report
    )
}
```

#### imNotification

Construct and send a notification to a web server as JSON (e.g., Microsoft Teams and Slack).

**Parameters:**
- `summary_params` (Map): Map of summary parameters
- `hook_url` (String): Webhook URL

**Example:**
```nextflow
if (params.hook_url) {
    def summary = [:]
    summary['Run Name'] = workflow.runName
    summary['Status'] = workflow.success ? 'SUCCESS' : 'FAILED'
    
    imNotification([Summary: summary], params.hook_url)
}
```

## Internal Helper Classes

The functionality is organized into several internal utility classes:

### NfcoreNotificationUtils

Handles notifications, emails, and terminal output formatting.

### NfcoreReportingUtils

Manages reporting functions for MultiQC and pipeline summaries.

### NfcoreVersionUtils

Provides version-related utility functions.

### NfcoreConfigValidator

Validates pipeline configurations and profiles. 