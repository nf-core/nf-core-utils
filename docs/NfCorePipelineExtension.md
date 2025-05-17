# NfCorePipelineExtension

This extension provides a DSL2-friendly wrapper for nf-core utility functions, making them easily accessible in your Nextflow pipelines.

---

## Importing Functions

You can import all or specific utility functions from the plugin:

```nextflow
// Import all functions
include { NfcorePipelineUtils } from 'plugin/nf-utils'

// Import specific functions
include { checkConfigProvided; completionEmail } from 'plugin/nf-utils'
```

---

## Quick Reference Table

| Function                | Purpose                                      |
|-------------------------|----------------------------------------------|
| checkConfigProvided     | Warn if no custom config/profile is provided |
| checkProfileProvided    | Validate profile argument                    |
| getWorkflowVersion      | Get workflow version string                  |
| paramsSummaryMultiqc    | Generate MultiQC summary YAML                |
| workflowSummaryMQC      | Create MultiQC summary template              |
| sectionLogs             | Generate colored section logs                |
| logColours              | Get ANSI color codes for logs                |
| completionSummary       | Print summary at pipeline completion         |
| completionEmail         | Send completion email                        |
| imNotification          | Send Slack/Teams notification                |
| getSingleReport         | Get a single report from Path/List           |

---

## Usage Examples

### Pipeline Initialization

```nextflow
#!/usr/bin/env nextflow

include { checkConfigProvided; checkProfileProvided } from 'plugin/nf-utils'
checkConfigProvided()
checkProfileProvided(args)

include { logColours } from 'plugin/nf-utils'
def colors = logColours(params.monochrome_logs)
log.info "${colors.purple}Pipeline started${colors.reset}"
```

---

### In Processes

```nextflow
include { paramsSummaryMultiqc } from 'plugin/nf-utils'

process MULTIQC {
    // ... process definition ...
    script:
    def summary = [Run_name: workflow.runName, Output_dir: params.outdir]
    def yaml = paramsSummaryMultiqc([Summary: summary])
    """
    echo '$yaml' > workflow_summary_mqc.yaml
    multiqc -f .
    """
}
```

---

### At Pipeline Completion

```nextflow
include { completionSummary; completionEmail; imNotification } from 'plugin/nf-utils'

workflow.onComplete {
    completionSummary(params.monochrome_logs)

    if (params.email || params.email_on_fail) {
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

    if (params.hook_url) {
        def summary = [Run_name: workflow.runName]
        imNotification([Summary: summary], params.hook_url)
    }
}
```

---

## Function Reference

For detailed documentation of each function, including parameters, return values, and advanced usage, see [NfCore Utilities](NfCoreUtilities.md).

---

## Internal Structure

The extension is implemented in Groovy and organized into several internal utility classes for configuration, reporting, notifications, and versioning.  
For advanced customization or troubleshooting, see the source code in `src/main/groovy/nfcore/plugin/` and `src/main/groovy/nfcore/plugin/util/`.

---