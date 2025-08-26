# NfcoreConfigValidator

Configuration validation utilities for nf-core pipelines.

## Overview

The `NfcoreConfigValidator` utility provides essential validation functions to ensure proper pipeline configuration and command-line usage. These functions help maintain nf-core community standards and provide helpful warnings to users about configuration best practices.

## Available Functions

### `checkConfigProvided()`

**Description:**  
Validates that a custom configuration or profile has been provided for the pipeline run. This function checks for institutional configs, custom profiles, or other non-default configurations to encourage best practices in nf-core pipeline usage.

**Function Signature:**
```nextflow
boolean checkConfigProvided()
```

**Parameters:**
- None (uses session context automatically)

**Returns:**
- `boolean`: `true` if custom configuration is detected, `false` if using default configuration

**Usage Example:**
```nextflow
#!/usr/bin/env nextflow

include { checkConfigProvided } from 'plugin/nf-core-utils'

// Check configuration early in pipeline initialization
def configOk = checkConfigProvided()
if (!configOk) {
    log.warn """
    No custom configuration provided!
    Consider using an institutional config or custom profile.
    See: https://nf-co.re/docs/usage/configuration
    """
}

workflow {
    // Pipeline logic here
}
```

**Integration in nf-core Pipelines:**
```nextflow
// Typical usage at the top of main.nf
include { checkConfigProvided; checkProfileProvided } from 'plugin/nf-core-utils'

// Validate configuration and profile setup
checkConfigProvided()
checkProfileProvided(args, params.monochrome_logs)

// Continue with pipeline initialization
include { PIPELINE_INITIALISATION } from './subworkflows/local/utils_nfcore_pipeline'
```

---

### `checkProfileProvided(List args, boolean monochromeLogs = true)`

**Description:**  
Validates command-line arguments for proper profile usage and warns about common mistakes like positional arguments or trailing commas in profile specifications. Error messages include color formatting when colors are enabled.

**Function Signature:**
```nextflow
void checkProfileProvided(List args, boolean monochromeLogs = true)
```

**Parameters:**
- `args` (List): Command-line arguments passed to the pipeline (typically the built-in `args` variable)
- `monochromeLogs` (Boolean, default: `true`): If true, disables color codes in error messages

**Usage Examples:**

**Basic Usage (monochrome output):**
```nextflow
#!/usr/bin/env nextflow

include { checkProfileProvided } from 'plugin/nf-core-utils'

// Validate profile arguments with default monochrome output
checkProfileProvided(args)

workflow {
    // Pipeline logic here
}
```

**Color-enabled Usage:**
```nextflow
#!/usr/bin/env nextflow

include { checkProfileProvided } from 'plugin/nf-core-utils'

// Enable colors in error messages
checkProfileProvided(args, params.monochrome_logs)

// Or explicitly enable colors
checkProfileProvided(args, false)

workflow {
    // Pipeline logic here
}
```

---

## Color Formatting

As of version 0.2.0, profile validation error messages support color formatting to improve user experience:

**With Colors Disabled (default):**
```
ERROR ~ The `-profile` option cannot end with a trailing comma, please remove it and re-run the pipeline!
HINT: A common mistake is to provide multiple values separated by spaces e.g. `-profile test, docker`.
```

**With Colors Enabled:**
```
[31mERROR[0m ~ The `-profile` option cannot end with a trailing comma, please remove it and re-run the pipeline!
[33mHINT[0m: A common mistake is to provide multiple values separated by spaces e.g. `-profile test, docker`.
```

**Configuration:**
- Colors are **disabled by default** (`monochromeLogs = true`) for backward compatibility
- Enable colors by setting `monochromeLogs = false` or using `params.monochrome_logs = false`
- Colors respect the existing `NfcoreNotificationUtils.logColours()` utility

---

**Common Validation Scenarios:**

1. **Valid Profile Usage:**
```bash
# These will pass validation
nextflow run pipeline.nf -profile docker
nextflow run pipeline.nf -profile singularity,test
nextflow run pipeline.nf -profile conda --input samples.csv
```

2. **Problematic Usage (triggers warnings):**
```bash
# Trailing comma warning
nextflow run pipeline.nf -profile docker,

# Positional argument warning  
nextflow run pipeline.nf some-positional-arg --input samples.csv

# Multiple issues
nextflow run pipeline.nf -profile test, positional-arg
```

**Error Handling Examples:**

**Basic Error Handling:**
```nextflow
include { checkProfileProvided } from 'plugin/nf-core-utils'

try {
    checkProfileProvided(args, params.monochrome_logs)
} catch (Exception e) {
    log.error "Profile validation failed: ${e.message}"
    System.exit(1)
}
```

**Advanced Error Handling with Color Context:**
```nextflow
include { checkProfileProvided } from 'plugin/nf-core-utils'

try {
    // Enable colors for better error visibility in CI/CD
    def useColors = !params.monochrome_logs && System.getenv('CI') == null
    checkProfileProvided(args, !useColors)
} catch (IllegalArgumentException e) {
    log.error """
    Profile validation failed!
    ${e.message}
    
    For help with profiles, see: https://nf-co.re/docs/usage/configuration#basic-configuration-profiles
    """
    System.exit(1)
}
```

---

## Implementation Details

### Configuration Detection Logic

The `checkConfigProvided()` function examines several aspects of the Nextflow configuration:

1. **Institutional Configs**: Detects configs from nf-core/configs
2. **Custom Profiles**: Identifies user-provided custom profiles
3. **Custom Config Files**: Finds `-c custom.config` specifications
4. **Container Settings**: Validates container/conda/module configurations

### Profile Validation Logic

The `checkProfileProvided()` function performs:

1. **Profile Parsing**: Extracts `-profile` arguments from command line
2. **Syntax Validation**: Checks for trailing commas and formatting issues
3. **Positional Argument Detection**: Warns about likely positional argument mistakes
4. **Best Practice Guidance**: Provides helpful suggestions for common issues

### Session Integration

Both functions integrate with the Nextflow session context:
- Access workflow metadata through `session.getWorkflowMetadata()`
- Read configuration through `session.config`
- Provide context-aware warnings and suggestions

---

## Best Practices

### 1. Early Validation

Call validation functions early in your pipeline:

```nextflow
#!/usr/bin/env nextflow

// Validate configuration before any heavy processing
include { checkConfigProvided; checkProfileProvided } from 'plugin/nf-core-utils'

checkConfigProvided()
checkProfileProvided(args)

// Then proceed with pipeline logic
workflow {
    // Main workflow here
}
```

### 2. Informative Logging

Provide helpful context when validation fails:

```nextflow
if (!checkConfigProvided()) {
    log.warn """
    ==========================================
    No custom configuration detected!
    
    For optimal performance and reproducibility, consider:
    - Using institutional configs: -profile yourInstitution
    - Providing custom profiles: -profile docker,custom
    - Specifying config files: -c your_custom.config
    
    See documentation: https://nf-co.re/docs/usage/configuration
    ==========================================
    """
}
```

### 3. Testing Different Scenarios

Test your pipeline with various configuration scenarios:

```groovy
// Test with different profile combinations
testList = [
    [profile: 'test'],
    [profile: 'test,docker'],
    [profile: 'conda'],
    [profile: ''] // Test default behavior
]

testList.each { testCase ->
    // Test pipeline with different profiles
}
```

---

## Common Issues and Solutions

### Issue: Trailing Comma in Profile

**Problem:**
```bash
nextflow run pipeline.nf -profile docker,
```

**Error Message (with colors enabled):**
```
[31mERROR[0m ~ The `-profile` option cannot end with a trailing comma, please remove it and re-run the pipeline!
[33mHINT[0m: A common mistake is to provide multiple values separated by spaces e.g. `-profile test, docker`.
```

**Solution:**
Remove trailing comma:
```bash
nextflow run pipeline.nf -profile docker
```

### Issue: Positional Arguments

**Problem:**
```bash
nextflow run pipeline.nf input.csv --outdir results
```

**Warning:** `input.csv` appears to be a positional argument

**Solution:**
Use proper parameter syntax:
```bash
nextflow run pipeline.nf --input input.csv --outdir results
```

### Issue: Default Configuration Warning

**Problem:**
Using default configuration without institutional or custom settings

**Solution:**
```bash
# Use institutional config
nextflow run pipeline.nf -profile myInstitution

# Or provide custom config
nextflow run pipeline.nf -c custom.config -profile docker
```

### Issue: Color Formatting Not Working

**Problem:**
Error messages appear without color formatting even when colors are expected.

**Troubleshooting:**

1. **Check monochrome_logs parameter:**
```nextflow
// Make sure you're passing the right value
checkProfileProvided(args, false)  // Enables colors
checkProfileProvided(args, params.monochrome_logs)  // Uses pipeline parameter
```

2. **Verify terminal support:**
```bash
# Test if your terminal supports colors
echo -e "\033[31mRed Text\033[0m Normal Text"
```

3. **Check pipeline parameters:**
```nextflow
// In nextflow.config
params {
    monochrome_logs = false  // Enable colors globally
}
```

**Solution:**
Ensure color parameter is correctly configured and your terminal supports ANSI colors.

---

## Integration with nf-core Infrastructure

These validation functions integrate with broader nf-core infrastructure:

- **nf-core/configs**: Detects institutional configurations
- **nf-core template**: Standard integration patterns
- **Community guidelines**: Enforces best practices
- **Container systems**: Validates containerization setup

For more information on nf-core configuration standards, see the [nf-core documentation](https://nf-co.re/docs/usage/configuration).