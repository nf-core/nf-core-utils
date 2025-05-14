# NfCorePipelineExtension

This extension provides a DSL2-friendly wrapper for the nf-core utilities, exposing them for use in nf-core pipelines.

## Import

```nextflow
// Import all functions
include { NfcorePipelineUtils } from 'plugin/nf-utils'

// Import specific functions
include { checkConfigProvided; completionEmail } from 'plugin/nf-utils'
```

## Extension Summary

This extension exposes the utility functions documented in [NfCore Utilities](NfCoreUtilities.md) for convenient use in DSL2 pipelines. Rather than duplicating the full documentation here, this page provides guidance on how to use these utilities in your pipeline.

## Usage Guide

The utilities are designed to be used at different stages of your pipeline:

### Pipeline Initialization

At the start of your pipeline script:

```nextflow
#!/usr/bin/env nextflow

// Check configuration and profile
include { checkConfigProvided; checkProfileProvided } from 'plugin/nf-utils'
checkConfigProvided()
checkProfileProvided(args)

// Get color codes for terminal output
include { logColours } from 'plugin/nf-utils'
def colors = logColours(params.monochrome_logs)
log.info "${colors.purple}Pipeline started${colors.reset}"
```

### In Processes

When generating MultiQC reports:

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

### At Pipeline Completion

In the workflow completion handler:

```nextflow
include { completionSummary; completionEmail; imNotification } from 'plugin/nf-utils'

workflow.onComplete {
    // Print summary to terminal
    completionSummary(params.monochrome_logs)
    
    // Send email notification if configured
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
    
    // Send Slack/Teams notification if webhook URL provided
    if (params.hook_url) {
        def summary = [Run_name: workflow.runName]
        imNotification([Summary: summary], params.hook_url)
    }
}
```

## Available Functions

All the following functions are available through this extension:

- **Configuration:** `checkConfigProvided`, `checkProfileProvided`
- **Version Info:** `getWorkflowVersion`
- **Reporting:** `paramsSummaryMultiqc`, `workflowSummaryMQC`, `sectionLogs`
- **Notifications:** `logColours`, `completionSummary`, `completionEmail`, `imNotification`
- **Reports:** `getSingleReport`

For detailed documentation of each function with parameters, return values, and examples, see the comprehensive [NfCore Utilities](NfCoreUtilities.md) documentation. 