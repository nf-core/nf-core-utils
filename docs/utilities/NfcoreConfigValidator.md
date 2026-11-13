# Configuration Validation Tutorial

Configuration validation is the first line of defense against common pipeline execution issues. This guide teaches you how to implement robust configuration checking in your nf-core pipelines using the `NfcoreConfigValidator` utility.

## 1. Overview

Poor configuration is one of the leading causes of pipeline failures and user frustration. Users often:

- Run pipelines without custom profiles, leading to suboptimal resource allocation
- Make syntax errors in profile specifications
- Provide positional arguments that are silently ignored
- Use default settings that aren't suitable for their environment

The `NfcoreConfigValidator` utility addresses these issues by providing proactive validation that catches problems early and guides users toward best practices.

### 1.1. What You'll Learn

By the end of this tutorial, you'll understand how to:

- Implement configuration validation that prevents common user errors
- Provide helpful, color-coded feedback messages
- Guide users toward nf-core configuration best practices
- Handle edge cases gracefully without breaking pipelines

### 1.2. Core Validation Functions

The utility provides two essential validation functions:

- **`checkConfigProvided()`**: Validates that custom configuration or profiles are being used
- **`checkProfileProvided(args, monochromeLogs)`**: Validates command-line profile arguments and warns about common mistakes

These checks are explicit: the plugin's observer does not run them. Call each once
during pipeline initialization, or retain the equivalent template-local check.
Do not call both implementations of the same check. Pipelines that only load the
plugin must add the calls if they want validation, including trailing-comma
rejection. An uncaught profile validation error aborts Nextflow with a nonzero exit
status and a single `ERROR` label (with optional colour); missing custom config and
positional arguments remain warnings. Call validation from pipeline initialization,
not an observer callback, where Nextflow catches and logs exceptions.

## 2. Getting Started with Configuration Validation

Let's start with a simple example that demonstrates why configuration validation matters.

### 2.1. The Problem: Default Configuration Issues

Consider what happens when users run pipelines without proper configuration:

```bash title="Problematic user commands"
# User runs with default settings - resource waste and poor performance
nextflow run nf-core/rnaseq --input samples.csv --outdir results

# User provides conflicting arguments
nextflow run nf-core/rnaseq --input samples.csv -profile docker,

# User provides positional arguments that are ignored
nextflow run nf-core/rnaseq samples.csv --outdir results
```

Without validation, these commands might run but produce suboptimal results or unexpected behavior.

### 2.2. The Solution: Proactive Validation

Let's implement basic validation that catches these issues:

```nextflow title="basic_validation.nf"
include { checkConfigProvided; checkProfileProvided } from 'plugin/nf-core-utils'

workflow {
    checkConfigProvided()
    checkProfileProvided(args, params.monochrome_logs)

    log.info "Configuration validation passed - proceeding with analysis"
}
```

```console title="Output without custom configuration"
$ nextflow run basic_validation.nf --input samples.csv

WARN: [basic_validation] You are attempting to run the pipeline without any custom configuration!

This will be dependent on your local compute environment but can be achieved via one or more of the following:
   (1) Using an existing pipeline profile e.g. `-profile docker` or `-profile singularity`
   (2) Using an existing nf-core/configs for your Institution e.g. `-profile crick` or `-profile uppmax`
   (3) Using your own local custom config e.g. `-c /path/to/your/custom.config`

Please refer to the quick start section and usage docs for the pipeline.
```

## 3. Core Validation Functions

### 3.1. checkConfigProvided - Custom Configuration Detection

This function encourages users to provide custom configurations for optimal performance.

**Understanding Configuration Types**

Nextflow can be configured in several ways, listed in order of preference:

1. **Institutional configs** (best): Pre-configured settings for specific institutions
2. **Custom profiles**: User-defined profiles for specific environments
3. **Custom config files**: Project-specific configuration files
4. **Default settings** (suboptimal): Basic Nextflow defaults

**Basic Implementation**

```nextflow title="config_detection_example.nf"
include { checkConfigProvided } from 'plugin/nf-core-utils'

workflow {
    def hasCustomConfig = checkConfigProvided()
    log.info "Starting pipeline with ${hasCustomConfig ? 'custom' : 'default'} configuration"
}
```

**Function Reference**

```groovy title="Function signature"
boolean checkConfigProvided()
```

| Aspect                   | Details                                             |
| ------------------------ | --------------------------------------------------- |
| **Parameters**           | None (uses session context automatically)           |
| **Returns**              | `boolean` - `true` if custom configuration detected |
| **Session Dependencies** | Requires Nextflow session context                   |
| **Side Effects**         | Logs warning messages if no custom config found     |

### 3.2. checkProfileProvided - Profile Argument Validation

This function validates command-line profile arguments and catches common user mistakes before they cause issues.

**Understanding Common Profile Mistakes**

Users frequently make syntax errors when specifying profiles:

```bash title="Common profile mistakes"
# Trailing comma (causes parsing errors)
nextflow run pipeline.nf -profile docker,

# Space-separated values (only first is used)
nextflow run pipeline.nf -profile test docker

# Positional arguments (silently ignored)
nextflow run pipeline.nf samples.csv --outdir results

# Correct usage
nextflow run pipeline.nf -profile test,docker --input samples.csv --outdir results
```

**Basic Profile Validation**

Let's implement profile validation with helpful error messages:

```nextflow title="profile_validation_example.nf"
include { checkProfileProvided } from 'plugin/nf-core-utils'

workflow {
    checkProfileProvided(args, params.monochrome_logs)
    log.info "Using profile: ${workflow.profile}"
}
```

**Color-Coded Error Messages**

Pass `false` as `monochromeLogs` to colour the error labels.

**Without colors (default):**

```console title="Monochrome error output"
ERROR ~ The `-profile` option cannot end with a trailing comma, please remove it and re-run the pipeline!
HINT: A common mistake is to provide multiple values separated by spaces e.g. `-profile test, docker`.
```

**With colors enabled:**

```console title="Color error output"
[31mERROR[0m ~ The `-profile` option cannot end with a trailing comma, please remove it and re-run the pipeline!
[33mHINT[0m: A common mistake is to provide multiple values separated by spaces e.g. `-profile test, docker`.
```

**Function Reference**

```groovy title="Function signature"
void checkProfileProvided(List args, boolean monochromeLogs = true)
```

| Parameter        | Type    | Default  | Description                                           |
| ---------------- | ------- | -------- | ----------------------------------------------------- |
| `args`           | List    | Required | Command-line arguments (use built-in `args` variable) |
| `monochromeLogs` | Boolean | `true`   | If `true`, disables color codes in error messages     |

## 4. Real-World Integration Patterns

Let's explore how to integrate configuration validation into different types of pipelines.

### 4.1. Basic nf-core Pipeline Integration

Here's the standard integration pattern for nf-core pipelines:

```nextflow title="main.nf"
include { checkConfigProvided; checkProfileProvided } from 'plugin/nf-core-utils'
include { INPUT_CHECK                             } from './subworkflows/local/input_check'
include { FASTQC                                  } from './modules/nf-core/fastqc/main'

params {
    input: Path
    outdir: String = 'results'
    monochrome_logs: Boolean = false
}

workflow {
    checkConfigProvided()
    checkProfileProvided(args, params.monochrome_logs)

    ch_input = INPUT_CHECK(params.input)
    FASTQC(ch_input)
}
```

## 5. Troubleshooting Common Issues

### 5.1. Profile Validation Errors

| Error                              | Cause                                | Solution                                     |
| ---------------------------------- | ------------------------------------ | -------------------------------------------- |
| "cannot end with a trailing comma" | `nextflow run -profile docker,`      | Remove trailing comma: `-profile docker`     |
| "positional argument detected"     | `nextflow run pipeline.nf input.csv` | Use proper syntax: `--input input.csv`       |
| "positional argument detected"     | `-profile test docker`               | Use comma separation: `-profile test,docker` |

### 5.2. Configuration Detection Issues

**Problem**: `checkConfigProvided()` returns `false` even with custom config

**Cause**: `checkConfigProvided()` returns `false` when the profile is `standard` (no `-profile` given) and at most one config file is loaded (the pipeline's own `nextflow.config`). Print what Nextflow sees:

```nextflow title="config_debug.nf"
include { checkConfigProvided } from 'plugin/nf-core-utils'

workflow {
    log.info "Profile: ${workflow.profile}"
    log.info "Config files: ${workflow.configFiles}"
    log.info "Custom config detected: ${checkConfigProvided()}"
}
```

### 5.3. Color Support Issues

**Problem**: Colors not displaying correctly in terminal

**Solution**: run with `--monochrome_logs` so `checkProfileProvided(args, params.monochrome_logs)` prints plain labels.

## 6. Best Practices Summary

### 6.1. Validation Checklist

When implementing configuration validation in your pipeline:

- [ ] Call validation functions early, before heavy processing
- [ ] Provide helpful error messages with solutions
- [ ] Enable colors for better user experience when supported
- [ ] Test with various configuration scenarios

### 6.2. Error Prevention Strategy

| User Scenario        | Validation Response               | User Experience               |
| -------------------- | --------------------------------- | ----------------------------- |
| No custom config     | Helpful warning with suggestions  | Guided toward best practices  |
| Profile syntax error | Clear error message with examples | Quick problem resolution      |
| Positional arguments | Warning about ignored parameters  | Prevention of silent failures |
| Good configuration   | Silent validation                 | Smooth pipeline execution     |

## 7. Takeaway

Configuration validation is essential for robust nf-core pipelines:

1. **`checkConfigProvided()`** encourages custom configuration usage for optimal performance
2. **`checkProfileProvided()`** prevents common profile syntax errors and provides helpful guidance
3. **Early validation** catches problems before resource-intensive processing begins
4. **Color-coded messages** improve user experience when supported

The utility transforms potential user frustration into guided learning experiences, making your pipelines more user-friendly and reliable.

## 8. What's Next?

- Explore **[NfcoreNotificationUtils](NfcoreNotificationUtils.md)** for completion notifications and user feedback
- Learn about **[NfCore Utilities](../NfCoreUtilities.md)** for comprehensive pipeline utilities
- Check out **[NextflowPipelineExtension](../NextflowPipelineExtension.md)** for core pipeline functions
