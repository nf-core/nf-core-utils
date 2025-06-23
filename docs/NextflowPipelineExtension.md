# NextflowPipelineExtension

This extension provides utility functions for Nextflow pipelines, migrated from the legacy `utils_nextflow_pipeline` subworkflow. These functions are designed to be used in DSL2 pipelines and are especially useful for nf-core pipelines, but can be used in any Nextflow project.

## Migration Note

The functions in this extension replace those previously found in the `utils_nextflow_pipeline` subworkflow. They are now available as part of the nf-core-utils plugin, making them easier to import and maintain.

## Import

```nextflow
include { getWorkflowVersion; dumpParametersToJSON; checkCondaChannels } from 'plugin/nf-core-utils'
```

## Functions

### getWorkflowVersion

Generates a formatted version string for a workflow, combining the manifest version and (optionally) a short git commit ID.

**Parameters:**
- `manifestVersion` (String): The workflow version from the manifest (e.g., `1.0.0` or `v1.0.0`).
- `commitId` (String, optional): The workflow commit ID (full SHA).

**Returns:**
- A formatted version string (e.g., `v1.0.0-gabcdef1`). If the version does not start with `v`, it is prepended. If a commit ID is provided, the first 7 characters are appended as `-g<shortsha>`.

**Example:**
```nextflow
version_str = getWorkflowVersion(workflow.manifest.version, workflow.commitId)
println "Pipeline version: ${version_str}"
```

**Edge Cases:**
- If `manifestVersion` is null or empty, returns an empty string.
- If `commitId` is null or empty, only the version is returned.

---

### dumpParametersToJSON

Dumps pipeline parameters to a JSON file in the specified output directory. The file is named with a timestamp and placed in a `pipeline_info` subdirectory of the output directory.

**Parameters:**
- `outdir` (Path): The output directory. If null, the function does nothing.
- `params` (Map): The pipeline parameters to dump.
- `launchDir` (Path): The launch directory (used for temporary file creation).

**Behavior:**
- Creates a file named `params_<timestamp>.json` in `${outdir}/pipeline_info/`.
- The JSON is pretty-printed for readability.
- A temporary file is created in the launch directory and then moved to the output directory.
- If `outdir` is null, no file is created and the function returns immediately.

**Example:**
```nextflow
if (params.outdir) {
    dumpParametersToJSON(params.outdir, params, workflow.launchDir)
}
```

**Edge Cases & Notes:**
- If the output directory does not exist, Nextflow will attempt to create the `pipeline_info` subdirectory.
- If the parameters map contains nested structures, they are serialized as JSON objects.
- If the function fails to write the file (e.g., due to permissions), an error will be raised by Nextflow.

---

### checkCondaChannels

Checks if the conda channels are set up correctly (specifically, that `conda-forge` and `bioconda` are present and in the correct order).

**Returns:**
- `true` if channels are set up correctly, or if verification fails (e.g., conda is not installed, YAML parsing fails, or channels are missing/empty).
- `false` if the required channels are missing or in the wrong order.

**Behavior:**
- Runs `conda config --show channels` and parses the output as YAML.
- Checks that both `conda-forge` and `bioconda` are present and in the required order (`conda-forge` first, then `bioconda`).
- If the check fails, prints a warning to stderr with the observed and required channel order.
- If the check cannot be performed (e.g., conda not installed, YAML error), prints a warning and returns `true` (does not block pipeline execution).

**Example:**
```nextflow
if (workflow.profile.contains('conda')) {
    if (!checkCondaChannels()) {
        log.warn "Conda channels are not configured correctly!"
    }
}
```

**Edge Cases & Notes:**
- If the channels list is empty or null, the function returns `true` to avoid blocking execution.
- If the YAML output is invalid or cannot be parsed, a warning is printed and `true` is returned.
- The function is robust to most errors and will not throw exceptions that stop the pipeline.

---

## Example Usage in a Nextflow Pipeline

```nextflow
workflow.onComplete {
    if (params.outdir) {
        dumpParametersToJSON(params.outdir, params, workflow.launchDir)
    }
    version_str = getWorkflowVersion(workflow.manifest.version, workflow.commitId)
    println "Pipeline version: ${version_str}"
    if (workflow.profile.contains('conda')) {
        if (!checkCondaChannels()) {
            log.warn "Conda channels are not configured correctly!"
        }
    }
}
```

## See Also
- [NfCorePipelineExtension](NfCorePipelineExtension.md) for a higher-level wrapper and more utilities.
- [NfCore Utilities](NfCoreUtilities.md) for additional helper functions for nf-core pipelines.