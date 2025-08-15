# nf-core-utils Plugin Validation Test Suite

This directory contains end-to-end validation tests for the nf-core-utils plugin, demonstrating various migration patterns and functionality.

## Test Structure

Each validation test is organized in its own subdirectory with:
- `main.nf` - Nextflow workflow demonstrating the functionality
- `nextflow.config` - Plugin configuration and test settings
- `README.md` - Specific test documentation

## Available Validation Tests

### 🔄 version-topic-channels/
**Migration Pattern: Version Utilities**

Demonstrates the migration from local pipeline utility functions to plugin-based utilities while preserving channel logic in the pipeline.

**Key validations:**
- Plugin function imports (`getWorkflowVersion`, `processVersionsFromFile`)
- Channel orchestration remaining visible (`.unique()`, `.map()`, `.mix()`, `.collectFile()`)
- Functional equivalence to original fetchngs implementation
- Architectural compliance with Ben's philosophy of keeping channel logic in pipelines

**Usage:**
```bash
# Run individual test
nextflow run validation/version-topic-channels/ -plugins nf-core-utils@0.2.0

# Or use the old validation script
./validation/validate.sh
```

### 🚧 Future Validation Tests

Additional validation tests can be added following the same pattern:

```
validation/
├── citation-utilities/          # Citation extraction and formatting
├── config-validation/           # Configuration validation utilities  
├── notification-system/         # Email, Slack, Teams notifications
├── references-extension/        # Reference file handling
└── reporting-orchestrator/      # Comprehensive reporting features
```

## Running All Validations

### Quick Validation (All Tests)
```bash
./validation/validate-all.sh
```

### Individual Test Execution
```bash
# Version utilities migration test
nextflow run validation/version-topic-channels/ -plugins nf-core-utils@0.2.0

# Future tests (when created)
# nextflow run validation/citation-utilities/ -plugins nf-core-utils@0.2.0
# nextflow run validation/config-validation/ -plugins nf-core-utils@0.2.0
```

### With Different Profiles
```bash
# Test profile (faster execution)
nextflow run validation/version-topic-channels/ -plugins nf-core-utils@0.2.0 -profile test

# Debug profile (enhanced logging)  
nextflow run validation/version-topic-channels/ -plugins nf-core-utils@0.2.0 -profile debug
```

## Prerequisites

1. **Build the plugin**: `make assemble`
2. **Install locally**: `make install` 
3. **Nextflow**: Ensure Nextflow >=23.04.0 is installed

## Migration Guidelines

These validation tests serve as templates for migrating nf-core pipelines to use the plugin:

### 1. Identify Local Utility Functions
Look for functions like:
- `getWorkflowVersion()`
- `processVersionsFromYAML()`
- `softwareVersionsToYAML()`
- Custom notification helpers
- Configuration validators

### 2. Replace with Plugin Imports
```groovy
// Before
def getWorkflowVersion() { ... }

// After  
include { getWorkflowVersion } from 'plugin/nf-core-utils'
```

### 3. Preserve Channel Logic
Keep all channel operations in the pipeline:
```groovy
// ✅ Good - Channel logic visible
ch_versions
    .unique()
    .map { file -> processVersionsFromFile([file]) }
    .mix(Channel.of(getWorkflowVersion()).map { ... })

// ❌ Bad - Channel logic hidden in plugin
// softwareVersionsChannelToYAML(ch_versions)  // This doesn't exist
```

### 4. Validate Migration
Use these validation tests to ensure:
- Plugin functions work correctly
- Channel logic remains transparent
- Output format is preserved
- Performance is maintained

## Adding New Validation Tests

To add a new validation test:

1. **Create test directory**:
   ```bash
   mkdir validation/your-test-name
   ```

2. **Create test files**:
   ```
   validation/your-test-name/
   ├── main.nf              # Test workflow
   ├── nextflow.config      # Plugin configuration
   └── README.md            # Test documentation
   ```

3. **Update validation runner**:
   Add your test to `validate-all.sh`:
   ```bash
   run_validation_test "Your Test Name" "your-test-name"
   ```

4. **Document the test**:
   Update this main README with test description

## Best Practices

- **Focused tests**: Each validation should test specific functionality
- **Self-contained**: Tests should not depend on external resources
- **Clear documentation**: Explain what the test validates and why
- **Realistic scenarios**: Mirror real pipeline usage patterns  
- **Comprehensive validation**: Check inputs, outputs, and behavior
- **Clean structure**: Follow the established directory organization

## Benefits of Validation Tests

✅ **Confidence**: Ensure plugin functions work as expected  
✅ **Migration guidance**: Provide clear patterns for pipeline migration  
✅ **Regression prevention**: Catch issues during plugin development  
✅ **Documentation**: Living examples of proper plugin usage  
✅ **Quality assurance**: Validate architectural decisions and best practices