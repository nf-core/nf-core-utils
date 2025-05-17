# nf-core Utility Functions

This document describes the utility functions provided by the nf-utils plugin for nf-core pipelines.

---

## Importing Functions

Import functions in your Nextflow DSL2 script as follows:

```nextflow
include { checkConfigProvided; completionEmail; logColours; paramsSummaryMultiqc; 
          completionSummary; imNotification; getWorkflowVersion } from 'plugin/nf-utils'
```

---

## Quick Reference Table

| Function                | Purpose                                      | Typical Usage Example                |
|-------------------------|----------------------------------------------|--------------------------------------|
| checkConfigProvided     | Warn if no custom config/profile is provided | `checkConfigProvided()`              |
| checkProfileProvided    | Validate profile argument                    | `checkProfileProvided(args)`         |
| getWorkflowVersion      | Get workflow version string                  | `getWorkflowVersion()`               |
| paramsSummaryMultiqc    | Generate MultiQC summary YAML                | `paramsSummaryMultiqc([Summary: ...])`|
| workflowSummaryMQC      | Create MultiQC summary template              | `workflowSummaryMQC(...)`            |
| sectionLogs             | Generate colored section logs                | `sectionLogs(sections, monochrome)`  |
| logColours              | Get ANSI color codes for logs                | `logColours(params.monochrome_logs)` |
| completionSummary       | Print summary at pipeline completion         | `completionSummary(params.monochrome_logs)` |
| completionEmail         | Send completion email                        | `completionEmail(...)`               |
| imNotification          | Send Slack/Teams notification                | `imNotification(..., hook_url)`      |
| getSingleReport         | Get a single report from Path/List           | `getSingleReport(multiqc_report)`    |

---

## Function Details

---

### Configuration and Validation

#### `checkConfigProvided()`

**Description:**  
Checks if a custom Nextflow config or profile has been provided. Logs a warning if not.

**Returns:**  
- `true` if a custom config/profile is provided  
- `false` otherwise

**Example:**
```nextflow
if (!checkConfigProvided()) {
    log.warn "No custom configuration provided! Please provide a profile or custom config."
}
```

---

#### `checkProfileProvided(args)`

**Description:**  
Checks if the `-profile` argument is valid and warns about positional arguments.

**Parameters:**
- `args` (Array): Command-line arguments passed to the pipeline

**Example:**
```nextflow
checkProfileProvided(args)
```

---

### Workflow Information

#### `getWorkflowVersion()`

**Description:**  
Returns a string representing the workflow version, including the git commit short SHA if available.

**Returns:**  
- `String` (e.g., `v1.2.3-gabcdef1`)

**Example:**
```nextflow
log.info "Pipeline version: ${getWorkflowVersion()}"
```

---

### Reporting and MultiQC

#### `paramsSummaryMultiqc(summaryParams)`

**Description:**  
Generates a YAML-formatted string for MultiQC workflow summary.

**Parameters:**
- `summaryParams` (Map): Map of parameter groups and their parameters

**Returns:**  
- `String`: YAML for MultiQC

**Example:**
```nextflow
def summary = [Run_Name: workflow.runName, Output_Dir: params.outdir]
def yaml = paramsSummaryMultiqc([Summary: summary])
```

---

#### `workflowSummaryMQC(summary, nfMetadataList, results)`

**Description:**  
Creates a workflow summary template for MultiQC.

**Parameters:**
- `summary` (Map): Map of parameters
- `nfMetadataList` (List): List of metadata fields to include
- `results` (Map): Map of pipeline results

**Returns:**  
- `Map` with HTML and text summaries for MultiQC

**Example:**
```nextflow
def summary = [Run_id: workflow.runName, User: workflow.userName]
def metadata = ['version', 'start', 'complete', 'duration']
def results = [Reads_Processed: '1,000,000', Failed_QC: '0.1%']
def mqc_summary = workflowSummaryMQC(summary, metadata, results)
```

---

#### `sectionLogs(sections, monochrome=false)`

**Description:**  
Generates summary logs for each section of a pipeline, optionally with color.

**Parameters:**
- `sections` (Map): Section names and log messages
- `monochrome` (Boolean, default: `false`): Use monochrome logs

**Returns:**  
- `Map` of colored section logs

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

---

### Notification and Logging

#### `logColours(monochrome_logs=true)`

**Description:**  
Returns a map of ANSI color codes for terminal logging.

**Parameters:**
- `monochrome_logs` (Boolean, default: `true`): If true, disables color codes

**Returns:**  
- `Map<String, String>`: Color codes

**Example:**
```nextflow
def colors = logColours(params.monochrome_logs)
log.info "${colors.purple}Pipeline started${colors.reset}"
```

---

#### `completionSummary(monochrome_logs=true)`

**Description:**  
Prints a summary of the pipeline run at completion.

**Parameters:**
- `monochrome_logs` (Boolean, default: `true`): If true, disables color codes

**Example:**
```nextflow
workflow.onComplete {
    completionSummary(params.monochrome_logs)
}
```

---

#### `completionEmail(summary_params, email, email_on_fail, plaintext_email, outdir, monochrome_logs=true, multiqc_report=null)`

**Description:**  
Constructs and sends a completion email with pipeline summary and optional MultiQC report.

**Parameters:**
- `summary_params` (Map): Summary parameters
- `email` (String): Email address to notify
- `email_on_fail` (String): Email for failures only
- `plaintext_email` (Boolean): Send plaintext email if true
- `outdir` (String): Output directory for reports
- `monochrome_logs` (Boolean, default: `true`): Use monochrome logs
- `multiqc_report` (Path|List, optional): MultiQC report file(s)

**Example:**
```nextflow
workflow.onComplete {
    def summary = [Run_Name: workflow.runName]
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

---

#### `imNotification(summary_params, hook_url)`

**Description:**  
Sends a notification to a webhook (e.g., Slack, Teams) with pipeline summary.

**Parameters:**
- `summary_params` (Map): Summary parameters
- `hook_url` (String): Webhook URL

**Example:**
```nextflow
if (params.hook_url) {
    def summary = [Run_Name: workflow.runName]
    imNotification([Summary: summary], params.hook_url)
}
```

---

#### `getSingleReport(multiqc_reports)`

**Description:**  
Returns a single report file from a Path or List of Paths.

**Parameters:**
- `multiqc_reports` (Path or List): MultiQC report(s)

**Returns:**  
- `Path` or `null`

**Example:**
```nextflow
def mqc_report = getSingleReport(multiqc_report)
```

---

## Internal Helper Classes

The plugin organizes functionality into several internal utility classes:
- **NfcoreNotificationUtils:** Handles notifications, emails, and terminal output formatting.
- **NfcoreReportingUtils:** Manages reporting functions for MultiQC and pipeline summaries.
- **NfcoreVersionUtils:** Provides version-related utility functions.
- **NfcoreConfigValidator:** Validates pipeline configurations and profiles.

For advanced usage or troubleshooting, see the source code in `src/main/groovy/nfcore/plugin/util/`.

---