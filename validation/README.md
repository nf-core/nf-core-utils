# nf-core-utils Plugin Validation Test Suite

This directory contains end-to-end validation tests for the nf-core-utils plugin, demonstrating various migration patterns and functionality.

## Test Structure

Each validation test is organized in its own subdirectory with:
- `main.nf` - Nextflow workflow demonstrating the functionality
- `main.nf.test` - nf-test suite with snapshot testing for consistency validation
- `nextflow.config` - Plugin configuration and test settings
- `README.md` - Specific test documentation

## Testing Framework

This validation suite uses **nf-test** for automated testing with snapshot functionality to ensure consistent behavior across plugin versions.

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
nextflow run validation/version-topic-channels/ -plugins nf-core-utils@0.3.0

# Or use the old validation script
./validation/validate.sh
```

### ⚙️ config-validation/
**Configuration Validation Functions**

Tests the configuration validation functions used in pipeline initialization:
- `checkConfigProvided()` - Pipeline configuration validation
- `checkProfileProvided()` - Execution profile validation  
- `checkCondaChannels()` - Conda channel configuration validation

### 📧 notification-system/
**Notification System Functions**

Tests the notification functions used in pipeline completion:
- `completionEmail()` - Email notifications
- `completionSummary()` - Terminal completion summaries
- `imNotification()` - Slack/Teams webhook notifications

### 🔧 pipeline-utilities/
**Core Pipeline Utilities**

Tests essential pipeline utility functions:
- `getWorkflowVersion()` - Version string generation
- `dumpParametersToJSON()` - Parameter file creation

### 🚧 Future Validation Tests

Additional validation tests can be added following the same pattern:

```
validation/
├── citation-utilities/          # Citation extraction and formatting
├── references-extension/        # Reference file handling
└── reporting-orchestrator/      # Comprehensive reporting features
```

## Running Tests

### nf-test Execution (Recommended)

Run all validation tests with snapshot verification:
```bash
# Run all validation tests
nf-test test

# Run specific test with snapshots
nf-test test validation/config-validation/main.nf.test
nf-test test validation/version-topic-channels/main.nf.test
nf-test test validation/notification-system/main.nf.test
nf-test test validation/pipeline-utilities/main.nf.test

# Update snapshots after plugin changes
nf-test test --update-snapshot

# Clean obsolete snapshots
nf-test test --clean-snapshot
```

### Direct Nextflow Execution (Legacy)

Run tests directly with Nextflow:
```bash
# Individual test execution
nextflow run validation/version-topic-channels/ -plugins nf-core-utils@0.3.0
nextflow run validation/config-validation/ -plugins nf-core-utils@0.3.0
nextflow run validation/notification-system/ -plugins nf-core-utils@0.3.0
nextflow run validation/pipeline-utilities/ -plugins nf-core-utils@0.3.0

# With different profiles
nextflow run validation/version-topic-channels/ -plugins nf-core-utils@0.3.0 -profile test
```

### Quick Validation (All Tests)
```bash
# Legacy validation script (still available)
./validation/validate-all.sh
```

## Prerequisites

1. **Build the plugin**: `make assemble`
2. **Install locally**: `make install` 
3. **Nextflow**: Ensure Nextflow >=25.04.0 is installed
4. **nf-test**: Install nf-test for automated testing:
   ```bash
   # Install nf-test (latest version)
   wget -qO- https://get.nf-test.com | bash
   sudo mv nf-test /usr/local/bin/
   
   # Or use conda/mamba
   conda install -c bioconda nf-test
   ```

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
- **Use nf-test snapshots** to verify consistent behavior across plugin versions

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
   ├── main.nf.test         # nf-test suite with snapshots
   ├── nextflow.config      # Plugin configuration
   └── README.md            # Test documentation
   ```

3. **Create nf-test suite**:
   ```bash
   # Generate initial test structure
   nf-test generate pipeline validation/your-test-name/main.nf
   
   # Add snapshot assertions and test cases
   # See existing .nf.test files as examples
   ```

4. **Update validation runner**:
   Add your test to `validate-all.sh`:
   ```bash
   run_validation_test "Your Test Name" "your-test-name"
   ```

5. **Document the test**:
   Update this main README with test description

## Best Practices

### Test Design
- **Focused tests**: Each validation should test specific functionality
- **Self-contained**: Tests should not depend on external resources
- **Clear documentation**: Explain what the test validates and why
- **Realistic scenarios**: Mirror real pipeline usage patterns  
- **Comprehensive validation**: Check inputs, outputs, and behavior
- **Clean structure**: Follow the established directory organization

### nf-test Specific
- **Use snapshots**: Capture outputs for consistency validation across versions
- **Multiple test cases**: Include success scenarios, edge cases, and error handling
- **Descriptive test names**: Use meaningful names that explain what is being tested
- **Snapshot management**: Keep snapshots up-to-date with `--update-snapshot`
- **CI integration**: Run nf-tests in continuous integration workflows

## Benefits of Validation Tests

### Functional Benefits
✅ **Confidence**: Ensure plugin functions work as expected  
✅ **Migration guidance**: Provide clear patterns for pipeline migration  
✅ **Documentation**: Living examples of proper plugin usage  
✅ **Quality assurance**: Validate architectural decisions and best practices

### Testing Benefits (nf-test)
✅ **Regression prevention**: Catch issues during plugin development with snapshots
✅ **Consistency validation**: Ensure identical behavior across plugin versions
✅ **Automated testing**: Integration with CI/CD pipelines for continuous validation
✅ **Snapshot management**: Track expected outputs and detect unexpected changes