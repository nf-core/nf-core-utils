# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the nf-core-utils Nextflow plugin, which provides utility functions used by nf-core pipelines. The plugin extends Nextflow with custom functions for configuration validation, notification, reporting, versioning, and reference handling.

## Development Commands

### Building and Testing

```bash
# Install the locked development toolchain
mise install --locked

# Build the plugin
mise run assemble
# or
mise exec -- ./gradlew assemble

# Run unit tests
mise run test
# or
mise exec -- ./gradlew test

# Clean build artifacts
mise run clean

# Install plugin locally for testing
mise run install
# or
mise exec -- ./gradlew install

# Run end-to-end validation (installs the plugin first)
mise run validate

# Test with Nextflow
mise exec -- nextflow run hello -plugins nf-core-utils@0.1.0
```

### Release and Publishing

```bash
# Publish the plugin (requires GitHub credentials in ~/.gradle/gradle.properties)
mise run release
# or
mise exec -- ./gradlew releasePlugin
```

## Architecture Overview

### Plugin Structure

The plugin follows Nextflow's PF4J-based plugin architecture:

- **NfUtilsPlugin.groovy**: Main plugin entry point extending BasePlugin
- **NfUtilsExtension.groovy**: Primary extension point providing @Function-annotated methods
- **NfUtilsFactory**: Factory class for plugin initialization

### Core Utility Classes

Located in `src/main/groovy/nfcore/plugin/nfcore/`:

- **NfcoreConfigValidator**: Validates pipeline configurations and profiles
- **NfcoreNotificationUtils**: Handles emails, Slack/Teams notifications, and terminal output formatting
- **NfcoreReportingUtils**: Manages MultiQC reporting and pipeline summaries
- **NfcoreVersionUtils**: Version aggregation and formatting (YAML, topic channels)
- **NfcoreCitationUtils**: Citation extraction and formatting from module meta.yml files
- **NfcoreReportingOrchestrator**: Orchestrates comprehensive reporting (versions + citations + bibliography + methods)

### Extension Points

The plugin provides three main extension categories accessible via @Function annotations:

1. **Configuration & Validation**: `checkConfigProvided()`, `checkProfileProvided()`
2. **Notification & Logging**: `completionEmail()`, `imNotification()`, `completionSummary()`
3. **Reporting & Versioning**: `getWorkflowVersion()`, comprehensive reporting via orchestrator classes

### References Extension

- **ReferencesUtils**: Handles igenomes and custom reference file resolution
- Functions: `getReferencesFile()`, `getReferencesValue()`

### Nextflow Pipeline Extension

- **NextflowPipelineUtils**: Core pipeline utilities
- Functions: `dumpParametersToJSON()`, `checkCondaChannels()`

## Key Configuration Files

### build.gradle

- Uses `io.nextflow.nextflow-plugin` version `0.0.1-alpha3`
- Plugin version: `0.3.0`
- Nextflow compatibility: `25.10.4+` (see `build.gradle`)
- Extension points: `NfUtilsExtension` and `NfUtilsFactory`
- Dependencies: SnakeYAML for YAML processing

### mise.toml and mise.lock

Define common development tasks and lock Java 21, Nextflow, and nf-test for local
development and CI. Use `mise tasks ls` to list commands. Gradle itself is managed
by the Gradle wrapper.

`monorepo_root = true` enables the workspace project graph. The single root
Gradle project is declared as `[monorepo.projects."gradle::"]` because
`settings.gradle` has no literal `include` directives for mise to parse. Use
`mise tasks graph` to inspect projects and `mise run --affected <task>` to run
tasks only for changed projects. If multi-project Gradle `include`s are added
later, remove the `gradle::` override so the Gradle provider can infer membership.

`gradle.properties` includes `org.gradle.java.installations.fromEnv=JAVA_HOME`
so Gradle toolchains pick up the JDK selected by mise. Verify with
`mise exec -- ./gradlew -q javaToolchains`.

## Testing Strategy

### Test Structure

- Unit tests in `src/test/groovy/nfcore/plugin/`
- Organized by functionality: nextflow/, nfcore/, references/
- Uses Spock framework for testing
- Test resources in `src/test/resources/`

### Key Test Files

- **NfcoreReportingOrchestratorTest**: Tests comprehensive reporting functionality
- **NfcoreVersionUtilsTest**: Tests version processing and formatting
- **NextflowPipelineTest**: Tests core pipeline utilities
- Observer tests for pipeline monitoring functionality

## Plugin Usage Patterns

### Import Pattern in Nextflow Scripts

```nextflow
include { checkConfigProvided; completionEmail; getWorkflowVersion } from 'plugin/nf-core-utils'
```

### Typical Integration Points

1. **Pipeline initialization**: Config/profile validation
2. **Version reporting**: Workflow version strings and comprehensive reporting
3. **Completion handlers**: Email notifications, Slack/Teams integration
4. **MultiQC integration**: Parameter summaries and method descriptions

## Observer Pattern

The plugin implements trace observers for pipeline monitoring:

- **NfcorePipelineObserver**: Monitors workflow execution
- **NfcorePipelineObserverFactory**: Creates observers based on session configuration

## Development Notes

### Plugin Compatibility

- Built for Nextflow 25.10.4+; CI tests the minimum and every supported stable release line in `.github/workflows/validation.yml`
- Uses modern Nextflow plugin development patterns
- Follows nf-core community standards and conventions

### Code Organization

- Utility classes are stateless and use static methods where possible
- Extension methods delegate to utility classes for testability
- Clear separation between Nextflow extension points and core business logic

### Documentation

Comprehensive documentation exists in `docs/` directory:

- `NextflowPipelineExtension.md`
- `NfCoreUtilities.md`
- `ReferencesExtension.md`
