# Nextflow Pipeline Extension

This extension provides essential utility functions for modern Nextflow pipelines, replacing the legacy `utils_nextflow_pipeline` subworkflow with a cleaner, plugin-based approach. These functions streamline common pipeline tasks like version management, parameter handling, and environment validation.

## 1. Overview

The Nextflow Pipeline Extension offers three core utilities designed to enhance your pipeline development experience:

- **Version Management**: Generate consistent, git-aware version strings
- **Parameter Documentation**: Export pipeline parameters for reproducibility
- **Environment Validation**: Verify conda channel configurations

:::note{title="Migration from Legacy Subworkflows"}
These functions replace those previously found in the `utils_nextflow_pipeline` subworkflow. The plugin-based approach provides easier imports, better maintenance, and improved reliability.
:::

## 2. Getting Started

### 2.1. Basic Import

Let's start by importing the essential functions into your pipeline:

```nextflow title="main.nf" hl_lines="3"
#!/usr/bin/env nextflow

include { getWorkflowVersion; dumpParametersToJSON; checkCondaChannels } from 'plugin/nf-core-utils'

workflow {
    log.info "Pipeline version: ${getWorkflowVersion(workflow.manifest.version, workflow.commitId)}"
}
```

:::tip{title="Function Selection"}
Import only the functions you need. Each function is independent and can be imported separately for lighter dependency management.
:::

### 2.2. Quick Start Example

Here's a minimal example showing all three functions in action:

```nextflow title="example.nf"
#!/usr/bin/env nextflow

include {
    getWorkflowVersion;
    dumpParametersToJSON;
    checkCondaChannels
} from 'plugin/nf-core-utils'

params {
    outdir: String = 'results'
}

workflow {
    main:
    log.info "Running ${workflow.manifest.name} ${getWorkflowVersion()}"

    if (workflow.profile.contains('conda')) {
        checkCondaChannels()
    }

    onComplete:
    dumpParametersToJSON(params.outdir, params)
}
```

```console title="Output"
N E X T F L O W  ~  version 25.04.0
Launching `example.nf` [example-run] - revision: abc1234 [main]

Running my-pipeline v1.2.0-gabc1234

[INFO] Pipeline completed successfully
[INFO] Parameters saved to results/pipeline_info/params_2024-01-15_10-30-45.json
```

## 3. Core Functions Reference

Each function serves a specific purpose in pipeline management. Let's explore them in order of typical usage.

### 3.1. getWorkflowVersion - Version String Generation

The `getWorkflowVersion` function creates consistent, git-aware version strings for your pipeline. It combines your manifest version with git commit information to provide traceable version identifiers.

**Basic Usage**

Let's start with a simple version string generation:

```nextflow title="Basic version example"
#!/usr/bin/env nextflow

include { getWorkflowVersion } from 'plugin/nf-core-utils'

workflow {
    log.info "Version: ${getWorkflowVersion('1.0.0')}"
}
```

```console title="Output"
Version: v1.0.0
```

**Git-Aware Versioning**

For better traceability, include git commit information:

```nextflow title="Git-aware version example"
#!/usr/bin/env nextflow

include { getWorkflowVersion } from 'plugin/nf-core-utils'

workflow {
    log.info "Full version: ${getWorkflowVersion(workflow.manifest.version, workflow.commitId)}"
}
```

```console title="Output"
Full version: v1.0.0-gabc1234
```

Called without arguments, `getWorkflowVersion()` takes both from the running workflow, so it gives the same result.

**Function Parameters**

| Parameter         | Type              | Description                           | Example                 |
| ----------------- | ----------------- | ------------------------------------- | ----------------------- |
| `manifestVersion` | String            | Version from nextflow.config manifest | `"1.0.0"` or `"v1.0.0"` |
| `commitId`        | String (optional) | Full git SHA hash                     | `"abc1234def5678..."`   |

**Version Format Rules**

The function applies these formatting rules:

1. **Version Prefix**: Adds 'v' prefix if not present (`1.0.0` → `v1.0.0`)
2. **Git Shortening**: Uses first 7 characters of commit ID
3. **Git Prefix**: Adds 'g' prefix to git hash (`abc1234` → `gabc1234`)

:::tip{title="Best Practice"}
Always use `workflow.manifest.version` and `workflow.commitId` for automatic version detection in pipelines.
:::

---

### 3.2. dumpParametersToJSON - Parameter Documentation

The `dumpParametersToJSON` function creates JSON documentation of your pipeline run parameters, essential for reproducibility and troubleshooting. It automatically timestamps and organizes parameter files for easy reference, serializes common Nextflow parameter types, and writes via Nextflow file handling so local and cloud-backed output paths are supported.

**Basic Parameter Export**

Let's implement basic parameter logging:

```nextflow title="Basic parameter export"
#!/usr/bin/env nextflow

include { dumpParametersToJSON } from 'plugin/nf-core-utils'

params {
    outdir: String = 'results'
    input: String = 'samples.csv'
    genome: String = 'GRCh38'
}

workflow {
    main:
    log.info "Processing with genome: ${params.genome}"

    onComplete:
    dumpParametersToJSON(params.outdir, params)
}
```

After running this pipeline, you'll find:

```console title="Generated file structure"
results/
└── pipeline_info/
    └── params_2024-01-15_10-30-45.json
```

```json title="params_2024-01-15_10-30-45.json"
{
  "outdir": "results",
  "input": "samples.csv",
  "genome": "GRCh38",
  "max_memory": "128.GB",
  "max_cpus": 16,
  "max_time": "240.h"
}
```

**Function Parameters**

| Parameter | Type        | Required | Description                         |
| --------- | ----------- | -------- | ----------------------------------- |
| `outdir`  | Path/String | Yes      | Output directory for parameter file |
| `params`  | Map         | Yes      | Pipeline parameters to document     |

**File Organization**

The function follows nf-core conventions for file organization:

- **Location**: `${outdir}/pipeline_info/`
- **Filename**: `params_<timestamp>.json`
- **Format**: Pretty-printed JSON for readability
- **Type handling**: `Path`, `Duration`, `MemoryUnit`, and `VersionNumber` parameters are converted to JSON-safe values
- **Path handling**: files are copied with Nextflow file utilities so remote/cloud output locations behave consistently with other pipeline outputs

:::warning{title="Safety Check"}
The function safely handles null `outdir` values by returning immediately without error, preventing pipeline crashes.
:::

---

### 3.3. checkCondaChannels - Environment Validation

The `checkCondaChannels` function validates your conda environment setup, ensuring that required bioinformatics channels are properly configured. This prevents common dependency resolution issues in conda-based pipelines.

**Understanding Conda Channel Priority**

Conda channels have priority order that affects package resolution:

```bash title="Correct channel order"
# Higher priority (searched first)
conda-forge     # General scientific packages
bioconda        # Bioinformatics packages
defaults        # Base conda packages
# Lower priority (searched last)
```

Wrong channel order can lead to:

- Package conflicts
- Outdated software versions
- Failed environment creation

**Basic Channel Validation**

Let's implement conda channel checking:

```nextflow title="Basic conda validation"
#!/usr/bin/env nextflow

include { checkCondaChannels } from 'plugin/nf-core-utils'

workflow {
    // Only check channels when using conda profile
    if (workflow.profile.contains('conda')) {
        log.info "Validating conda channel configuration..."

        if (checkCondaChannels()) {
            log.info "✓ Conda channels configured correctly"
        } else {
            log.warn "⚠ Conda channel configuration needs attention"
            log.warn "Please run: conda config --add channels conda-forge"
            log.warn "Please run: conda config --add channels bioconda"
        }
    }

    // Rest of your workflow...
}
```

**Example Output Scenarios**

**Correct Configuration:**

```console title="Valid channel setup"
INFO  [main] - Validating conda channel configuration...
INFO  [main] - ✓ Conda channels configured correctly
```

**Incorrect Configuration:**

```console title="Invalid channel setup"
INFO  [main] - Validating conda channel configuration...
WARN  [main] - ⚠ Conda channel configuration needs attention
WARN  [main] - Expected channel order: [conda-forge, bioconda]
WARN  [main] - Current channel order: [bioconda, conda-forge]
```

**Function Return Values**

| Return Value | Condition          | Description                                          |
| ------------ | ------------------ | ---------------------------------------------------- |
| `true`       | Channels correct   | conda-forge and bioconda in proper order             |
| `true`       | Conda unavailable  | Conda not installed or command failed (non-blocking) |
| `false`      | Channels incorrect | Required channels missing or wrong order             |

**Validation Process**

The function performs these steps:

1. **Execute**: Runs `conda config --show channels`
2. **Parse**: Interprets YAML output
3. **Validate**: Checks for conda-forge and bioconda presence and order
4. **Report**: Provides clear feedback on issues found

:::tip{title="Non-Blocking Design"}
The function is designed to be non-blocking. If conda is not available or checking fails, it returns `true` to allow pipeline continuation.
:::

---

## 4. Integration Patterns

### 4.1. Complete Pipeline Integration

```nextflow title="main.nf"
#!/usr/bin/env nextflow

include { getWorkflowVersion; dumpParametersToJSON; checkCondaChannels } from 'plugin/nf-core-utils'

params {
    input: String
    outdir: String = 'results'
}

workflow {
    main:
    log.info "Starting ${workflow.manifest.name} ${getWorkflowVersion()}"

    if (workflow.profile.contains('conda') && !checkCondaChannels()) {
        error "Conda channels not configured correctly: run `conda config --add channels conda-forge` and `conda config --add channels bioconda`"
    }

    // Pipeline logic here...

    onComplete:
    if (workflow.success) {
        dumpParametersToJSON(params.outdir, params)
    }

    onError:
    log.error "Pipeline ${getWorkflowVersion()} failed: ${workflow.errorMessage}"
}
```

## 5. Best Practices

### 5.1. Version Management

:::tip{title="Version String Usage"}

- Include version in all log messages
- Add version to output filenames for traceability
- Use consistent version format across your organization

:::

### 5.2. Parameter Documentation

:::warning{title="Documentation Timing"}

- Call `dumpParametersToJSON` in the `onComplete:` section of the entry workflow
- Consider saving parameters on error for debugging failed runs
- Include the timestamp to distinguish between multiple runs

:::

### 5.3. Environment Validation

:::note{title="Validation Strategy"}

- Check conda channels early in pipeline execution
- Make validation non-fatal in development environments
- Provide clear fixing instructions in error messages

:::

## 6. Migration Guide

### 6.1. From Legacy Subworkflows

If you're migrating from the old `utils_nextflow_pipeline` subworkflow:

**Before:**

```nextflow
include { getWorkflowVersion } from '../subworkflows/local/utils_nextflow_pipeline'
```

**After:**

```nextflow
include { getWorkflowVersion } from 'plugin/nf-core-utils'
```

### 6.2. Update Checklist

- [ ] Replace subworkflow includes with plugin imports
- [ ] Update any custom function calls to match new signatures
- [ ] Test version string format matches expectations
- [ ] Verify parameter JSON files are created correctly
- [ ] Confirm conda validation works with your profiles

## 7. Takeaway

The Nextflow Pipeline Extension provides three essential utilities that every pipeline should use:

1. **`getWorkflowVersion`**: Creates traceable, git-aware version strings
2. **`dumpParametersToJSON`**: Documents run parameters for reproducibility
3. **`checkCondaChannels`**: Validates conda environment setup

These functions replace legacy subworkflows with a cleaner, plugin-based approach that's easier to maintain and more reliable.

## 8. What's Next?

- Explore the **[NfCore Utilities](NfCoreUtilities.md)** for advanced pipeline features like notifications and reporting
- Learn about **[References Extension](ReferencesExtension.md)** for genome reference handling
- Check out individual utility documentation in the `utilities/` directory for specialized functions
