# nf-utils plugin

The nf-utils plugin provides utility functions used by nf-core pipelines.

## Features

### NextflowPipelineExtension

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

See the [NextflowPipelineExtension documentation](docs/NextflowPipelineExtension.md) for more details.

## Building

To build the plugin:
```bash
make assemble
```

## Testing with Nextflow

The plugin can be tested without a local Nextflow installation:

1. Build and install the plugin to your local Nextflow installation: `make install`
2. Run a pipeline with the plugin: `nextflow run hello -plugins nf-utils@0.1.0`

## Publishing

Plugins can be published to a central plugin registry to make them accessible to the Nextflow community. 


Follow these steps to publish the plugin to the Nextflow Plugin Registry:

1. Create a file named `$HOME/.gradle/gradle.properties`, where $HOME is your home directory. Add the following properties:

    * `pluginRegistry.accessToken`: Your Nextflow Plugin Registry access token. 

2. Use the following command to package and create a release for your plugin on GitHub: `make release`.


> [!NOTE]
> The Nextflow Plugin registry is currently available as private beta technology. Contact info@nextflow.io to learn how to get access to it.
> 