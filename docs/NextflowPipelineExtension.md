# NextflowPipelineExtension

This extension provides utility functions for Nextflow pipelines that were previously available in the `utils_nextflow_pipeline` subworkflow:

```nextflow
// Import utilities
include { getWorkflowVersion; dumpParametersToJSON; checkCondaChannels } from 'plugin/nf-utils'

// Get workflow version string
version_str = getWorkflowVersion(workflow.manifest.version, workflow.commitId)
println "Pipeline version: ${version_str}"

// Dump parameters to JSON
if (params.outdir) {
    dumpParametersToJSON(params.outdir, params, workflow.launchDir)
}

// Check conda channels
if (workflow.profile.contains('conda')) {
    checkCondaChannels()
}
```