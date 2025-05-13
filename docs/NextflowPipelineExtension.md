# NextflowPipelineExtension

This extension provides utility functions that were previously available in the `utils_nextflow_pipeline` subworkflow.

## Import

```nextflow
include { getWorkflowVersion; dumpParametersToJSON; checkCondaChannels } from 'plugin/nf-utils'
```

## Functions

### getWorkflowVersion

Generates a formatted version string for a workflow.

**Parameters:**
- `manifestVersion` (String): The workflow version from manifest
- `commitId` (String, optional): The workflow commit ID

**Returns:**
- A formatted version string (e.g., "v1.0.0-gabcdef1")

**Example:**
```nextflow
version_str = getWorkflowVersion(workflow.manifest.version, workflow.commitId)
println "Pipeline version: ${version_str}"
```

### dumpParametersToJSON

Dumps pipeline parameters to a JSON file in the specified output directory.

**Parameters:**
- `outdir` (Path): The output directory
- `params` (Map): The pipeline parameters
- `launchDir` (Path): The launch directory

**Example:**
```nextflow
if (params.outdir) {
    dumpParametersToJSON(params.outdir, params, workflow.launchDir)
}
```

### checkCondaChannels

Checks if conda channels are set up correctly (conda-forge and bioconda).

**Returns:**
- `true` if channels are set up correctly or verification failed
- `false` if channels are missing or in the wrong order

**Example:**
```nextflow
if (workflow.profile.contains('conda')) {
    checkCondaChannels()
}
```