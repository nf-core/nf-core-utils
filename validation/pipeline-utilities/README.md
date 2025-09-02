# Pipeline Utilities Validation Test

This validation test demonstrates and validates the core pipeline utility functions from the nf-core-utils plugin that are used in pipeline initialization workflows.

## Functions Tested

### `getWorkflowVersion(version, commitId)`

**Purpose**: Generate consistent workflow version strings for pipeline reporting and logging.

**Validation scenarios**:

- Default version (from session/manifest)
- Explicit version string
- Version with commit ID integration
- Version prefix handling (`v` prefix management)
- Edge cases (null values, various formats)

**Usage in fetchngs**:

```groovy
include { getWorkflowVersion } from 'plugin/nf-core-utils'

// In workflow initialization
def workflow_version = getWorkflowVersion(workflow.manifest.version, workflow.commitId)
```

### `dumpParametersToJSON(outdir, params)`

**Purpose**: Serialize pipeline parameters to a JSON file for provenance tracking and debugging.

**Validation scenarios**:

- Standard parameter sets (strings, numbers, booleans)
- Complex nested parameters (maps, lists)
- Edge cases (null values, empty parameters)
- File creation and content validation
- Output directory handling

**Usage in fetchngs**:

```groovy
include { dumpParametersToJSON } from 'plugin/nf-core-utils'

// In PIPELINE_INITIALISATION workflow
dumpParametersToJSON(params.outdir, params)
```

## What This Test Validates

### 1. Function Correctness

- **Version string formatting**: Proper `v` prefix handling, commit ID integration
- **JSON serialization**: Correct parameter serialization and file creation
- **Edge case handling**: Null parameters, empty values, missing directories

### 2. Output Validation

- **File creation**: JSON parameter files created in correct locations
- **Content validation**: JSON structure and parameter preservation
- **Format consistency**: Version strings follow expected patterns

### 3. Integration Readiness

- **Real-world parameters**: Uses parameter sets similar to actual fetchngs pipeline
- **Error handling**: Graceful handling of edge cases and invalid inputs
- **Performance**: Efficient execution suitable for pipeline initialization

## Running the Test

### Individual Execution

```bash
# Basic validation
nextflow run validation/pipeline-utilities/ -plugins nf-core-utils@0.3.0

# With test profile (faster)
nextflow run validation/pipeline-utilities/ -plugins nf-core-utils@0.3.0 -profile test

# With debug logging
nextflow run validation/pipeline-utilities/ -plugins nf-core-utils@0.3.0 -profile debug
```

### Via Validation Suite

```bash
# Run all validation tests including this one
./validation/validate-all.sh

# Run individual test via suite
./validation/validate.sh pipeline-utilities
```

## Expected Outputs

### Console Output

The test provides detailed logging of all validation steps:

```
=== Testing getWorkflowVersion() function ===
Default workflow version: v1.12.0
Explicit version (2.1.0): v2.1.0
Version with commit (2.1.0, abc123): v2.1.0-gabc123d
...

=== Testing dumpParametersToJSON() function ===
Dumping parameters to JSON in: /path/to/validation_output
✅ Null output directory handled correctly
✅ Empty parameters handled correctly
...
```

### Generated Files

- `validation_output/params.json` - Serialized pipeline parameters
- `validation_results/execution_*.html` - Nextflow execution reports
- Log files with detailed validation results

### Validation Checks

- ✅ Version string format validation (prefixes, commit integration)
- ✅ JSON file creation and content validation
- ✅ Parameter serialization accuracy
- ✅ Edge case handling (null values, empty parameters)
- ✅ Error handling for invalid inputs

## Integration with fetchngs

This test demonstrates the exact usage patterns found in the fetchngs pipeline:

### Pipeline Initialization Context

```groovy
// From fetchngs PIPELINE_INITIALISATION workflow
include { dumpParametersToJSON } from 'plugin/nf-core-utils'
include { getWorkflowVersion   } from 'plugin/nf-core-utils'

workflow PIPELINE_INITIALISATION {
    // ... other initialization steps ...

    // Dump parameters for provenance
    dumpParametersToJSON(params.outdir, params)

    // Generate version string for reporting
    def version = getWorkflowVersion(workflow.manifest.version, workflow.commitId)

    // ... continue with pipeline setup ...
}
```

### Parameter Sets

The test uses realistic parameter sets matching fetchngs:

- SRA/ENA download configuration
- Reference genome settings
- Resource limitations
- Notification settings
- nf-core boilerplate parameters

## Benefits Demonstrated

✅ **Standardization**: Consistent parameter handling across nf-core pipelines  
✅ **Provenance**: Reliable parameter dumping for workflow reproducibility  
✅ **Version tracking**: Proper version string generation with commit integration  
✅ **Error resilience**: Graceful handling of edge cases and invalid inputs  
✅ **Performance**: Efficient execution suitable for pipeline initialization

This validation ensures the pipeline utility functions are robust, reliable, and ready for production use in fetchngs and other nf-core pipelines.
