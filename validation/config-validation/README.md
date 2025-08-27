# Configuration Validation Test

This validation test demonstrates and validates the configuration validation functions from the nf-core-utils plugin that ensure proper pipeline setup and execution environment.

## Functions Tested

### `checkConfigProvided()`
**Purpose**: Validate that the pipeline has appropriate configuration setup.

**Validation scenarios**:
- Default configuration validation
- Custom configuration detection
- Project name validation
- Warning generation for missing configurations

**Usage in fetchngs**:
```groovy
include { checkConfigProvided } from 'plugin/nf-core-utils'

// In PIPELINE_INITIALISATION workflow
if (!checkConfigProvided()) {
    log.warn "Consider using a custom config file or profile for optimal performance"
}
```

### `checkProfileProvided(args)`
**Purpose**: Validate that appropriate execution profiles are specified in the command line.

**Validation scenarios**:
- Valid profile detection (`-profile docker`, `-profile test,conda`)
- Missing profile warnings
- Profile argument parsing from command line
- Multiple profile combinations
- Edge cases (empty args, malformed profiles)

**Usage in fetchngs**:
```groovy
include { checkProfileProvided } from 'plugin/nf-core-utils'

// In PIPELINE_INITIALISATION workflow  
checkProfileProvided(nextflow_cli_args)
```

### `checkCondaChannels()`
**Purpose**: Validate conda channel configuration for bioinformatics tools.

**Validation scenarios**:
- Conda channel order validation
- Required bioinformatics channels (conda-forge, bioconda)
- Channel priority verification
- Conda environment setup validation

**Usage in fetchngs**:
```groovy
include { checkCondaChannels } from 'plugin/nf-core-utils'

// In PIPELINE_INITIALISATION workflow
if (!checkCondaChannels()) {
    log.warn "Conda channels may not be optimally configured for bioinformatics"
}
```

## What This Test Validates

### 1. Configuration Detection
- **Project identification**: Proper project name detection and validation
- **Custom config detection**: Identification of custom configuration files
- **Profile validation**: Verification of execution profile specifications

### 2. Command Line Parsing
- **Argument processing**: Correct parsing of Nextflow command line arguments
- **Profile extraction**: Accurate extraction of `-profile` values
- **Edge case handling**: Robust handling of malformed or missing arguments

### 3. Environment Validation
- **Conda setup**: Validation of conda channel configuration
- **Channel order**: Verification of proper channel priority
- **Bioinformatics tools**: Ensuring access to required tool repositories

### 4. Warning Generation
- **User guidance**: Appropriate warnings for suboptimal configurations
- **Best practices**: Guidance toward nf-core recommended setups
- **Error prevention**: Early detection of configuration issues

## Running the Test

### Individual Execution
```bash
# Basic validation
nextflow run validation/config-validation/ -plugins nf-core-utils@0.3.0

# With different profiles to test profile validation
nextflow run validation/config-validation/ -plugins nf-core-utils@0.3.0 -profile test
nextflow run validation/config-validation/ -plugins nf-core-utils@0.3.0 -profile docker
nextflow run validation/config-validation/ -plugins nf-core-utils@0.3.0 -profile test,conda

# With debug logging
nextflow run validation/config-validation/ -plugins nf-core-utils@0.3.0 -profile debug
```

### Via Validation Suite
```bash
# Run all validation tests
./validation/validate-all.sh

# Run just configuration validation
nextflow run validation/config-validation/ -plugins nf-core-utils@0.3.0
```

## Expected Outputs

### Console Logging
The test provides detailed validation logging:
```
=== Testing checkConfigProvided() function ===
Configuration check result: true
✅ checkConfigProvided() executed successfully

=== Testing checkProfileProvided() function ===
Testing args 1: [nextflow, run, main.nf, -profile, docker]
✅ Profile check completed for case 1
...

=== Testing checkCondaChannels() function ===
Conda channels check result: true
✅ checkCondaChannels() executed successfully
✅ Conda channels are configured correctly
```

### Configuration Validation Results
- ✅ Configuration status (valid/needs attention)
- ✅ Profile detection and validation
- ✅ Conda channel verification
- ✅ Integration scenario testing
- ✅ Edge case handling

### Warning Examples
The functions may generate warnings for:
- Missing execution profiles
- Suboptimal conda channel configurations  
- Lack of custom configuration files
- Invalid command line arguments

## Integration with fetchngs

This test demonstrates the exact usage patterns found in fetchngs:

### PIPELINE_INITIALISATION Context
```groovy
// From fetchngs PIPELINE_INITIALISATION workflow
include { checkConfigProvided  } from 'plugin/nf-core-utils'
include { checkProfileProvided } from 'plugin/nf-core-utils' 
include { checkCondaChannels   } from 'plugin/nf-core-utils'

workflow PIPELINE_INITIALISATION {
    take:
    version           // boolean: Display version and exit
    help              // boolean: Display help and exit
    validate_params   // boolean: Boolean whether to validate parameters against the schema at runtime
    monochrome_logs   // boolean: Do not use coloured log outputs
    nextflow_cli_args //   array: List of positional nextflow CLI args
    outdir            //  string: The output directory for the pipeline
    input             //  string: Path to comma-separated file containing information about the samples in the experiment.

    main:
    
    // Print version and exit if required
    if (version) {
        println getWorkflowVersion(workflow.manifest.version, workflow.commitId)
        System.exit(0)
    }
    
    // Print help message and exit if required  
    if (help) {
        // ... help display logic ...
        System.exit(0)
    }
    
    // Check conda channels are set up correctly
    if (!checkCondaChannels()) {
        log.warn "Conda channels not optimally configured for bioinformatics tools"
    }
    
    // Check if a custom config file or profile has been provided
    if (!checkConfigProvided()) {
        log.info "Consider using a custom config file or profile for optimal performance"
    }
    
    // Check command line arguments for profile specification
    checkProfileProvided(nextflow_cli_args)
    
    // ... continue with other initialization tasks ...
}
```

### Command Line Argument Processing
The validation functions process actual Nextflow command line arguments:
```bash
# These commands will trigger different validation scenarios:
nextflow run nf-core/fetchngs -profile docker          # ✅ Valid profile
nextflow run nf-core/fetchngs -profile test,conda      # ✅ Multiple profiles
nextflow run nf-core/fetchngs                          # ⚠️ Missing profile warning
nextflow run nf-core/fetchngs -c custom.config         # ✅ Custom config detected
```

## Benefits Demonstrated

✅ **Early validation**: Configuration issues caught before pipeline execution  
✅ **User guidance**: Clear warnings and recommendations for optimal setup  
✅ **Environment verification**: Conda and execution environment validation  
✅ **Robust parsing**: Reliable command line argument processing  
✅ **Integration ready**: Direct compatibility with fetchngs initialization workflow  

This validation ensures the configuration validation functions provide reliable pipeline setup verification and user guidance for optimal execution environments.