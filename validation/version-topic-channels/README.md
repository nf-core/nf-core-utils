# nf-core-utils Plugin E2E Validation

This validation test demonstrates the successful migration from local pipeline utility functions to the nf-core-utils plugin, following the architectural guidance that "channel/operator logic should stay in the pipeline."

## What This Test Validates

### 1. Plugin Function Migration

- **Local functions replaced**: `getWorkflowVersion()`, `processVersionsFromYAML()`, `workflowVersionToYAML()`, `softwareVersionsToYAML()`
- **Plugin functions used**: `getWorkflowVersion()`, `processVersionsFromFile()`
- **Import pattern**: `include { getWorkflowVersion; processVersionsFromFile } from 'plugin/nf-core-utils'`

### 2. Channel Logic Preservation

The test demonstrates that all channel orchestration remains visible in the pipeline:

```groovy
ch_versions
    .unique()                                    // ✅ Channel logic in pipeline
    .map { version_file ->
        processVersionsFromFile([version_file])  // ✅ Plugin utility function
    }
    .unique()
    .mix(                                        // ✅ Channel orchestration visible
        Channel.of(getWorkflowVersion())         // ✅ Plugin utility function
            .map { workflow_version -> ... }     // ✅ Transformation logic visible
    )
    .collectFile(...)                            // ✅ Output handling in pipeline
```

### 3. Functional Equivalence

- Same output format as original fetchngs implementation
- Same YAML structure for MultiQC compatibility
- Same file naming and storage location
- Identical workflow version string generation

### 4. Architectural Compliance

- **Utilities in plugin**: Data transformation functions moved to plugin
- **Orchestration in pipeline**: Channel operations, mixing, and file collection remain in pipeline
- **Visibility maintained**: All data flow logic is transparent and modifiable
- **Separation of concerns**: Clear boundary between utilities and workflow logic

## Running the Validation

### Prerequisites

1. Build the plugin: `make assemble`
2. Install locally: `make install`

### Run the Test

```bash
# Basic validation
nextflow run validation/ -plugins nf-core-utils@0.7.0

# With test profile (faster)
nextflow run validation/ -plugins nf-core-utils@0.7.0 -profile test

# With debug logging
nextflow run validation/ -plugins nf-core-utils@0.7.0 -profile debug
```

### Expected Output

The test will:

1. ✅ Load the plugin successfully
2. ✅ Generate workflow version strings using `getWorkflowVersion()`
3. ✅ Process mock version files using `processVersionsFromFile()`
4. ✅ Demonstrate channel orchestration remaining in pipeline
5. ✅ Create final versions YAML file matching fetchngs format
6. ✅ Validate content contains expected tools and workflow info

### Output Files

- `validation_results/pipeline_info/nf_core_utils_software_mqc_versions.yml` - Final versions file
- `validation_results/execution_*.html` - Nextflow execution reports
- `validation_results/pipeline_dag.svg` - Workflow diagram

## Migration Pattern Demonstrated

This test serves as a template for migrating other nf-core pipelines:

### Before (Local Functions)

```groovy
// Local utility functions in main.nf or lib/Utils.groovy
def getWorkflowVersion() { ... }
def processVersionsFromYAML(yaml_file) { ... }
def softwareVersionsToYAML(ch_versions) { ... }

// Usage with hidden channel logic
softwareVersionsToYAML(ch_versions)
    .collectFile(...)
```

### After (Plugin Migration)

```groovy
// Import plugin utilities
include { getWorkflowVersion; processVersionsFromFile } from 'plugin/nf-core-utils'

// Explicit channel orchestration with plugin utilities
ch_versions
    .unique()
    .map { version_file -> processVersionsFromFile([version_file]) }
    .unique()
    .mix(Channel.of(getWorkflowVersion()).map { ... })
    .collectFile(...)
```

## Benefits Demonstrated

1. **Standardization**: Consistent version handling across nf-core pipelines
2. **Maintainability**: Centralized utility functions with shared bug fixes and improvements
3. **Transparency**: All workflow logic remains visible and customizable
4. **Flexibility**: Pipeline retains full control over data flow and processing
5. **Future-ready**: Prepared for topic channel migration while maintaining backward compatibility

This validation proves the migration approach successfully balances utility standardization with workflow transparency.
