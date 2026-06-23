# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.5.0]

### Added

- Added `citationsOnTheFly()` and `toolsFromVersionsTopic()` to build tool citations from the `versions` topic (the tools that actually ran), resolved from module `meta.yml` — no per-module changes or hand-maintained tool list. Short citations are publication-ready, `tool (version, Author et al. year)`, with graceful fallback to DOI/url. Tools that never emit a version (e.g. MultiQC plugins like `multiqcsav`) can be added manually via `extraTools` ([#43](https://github.com/nf-core/nf-core-utils/pull/43)).
- Added a `PipelineExecutionContext` seam and focused validation, version, and reporting adapters to keep Nextflow extension functions thin.
- Added `SoftwareVersionReport` as the canonical implementation for merging heterogeneous software-version inputs.
- Added `ReferenceSelection` for genome/reference lookup and `igenomes_base` substitution.

### Changed

- Updated reporting orchestration to use explicit execution context data, including workflow metadata for methods-description templates.
- Migrated pre-commit configuration to `prek.toml` and added validation snapshot checks.
- Improved citation formatting to follow [nf-core/modules Harvard style recommendation](https://github.com/nf-core/modules/blob/master/modules/meta-schema.json): short citations use `Author (year)` format (e.g. `Andrews (2010)`), italic `<em>et al.</em>`, DOI/homepage linking, and cleaner bibliography format ([#51](https://github.com/nf-core/nf-core-utils/pull/51)).

### Fixed

- Fixed `dumpParametersToJSON()` to serialize Nextflow parameter types and copy parameter reports through Nextflow file handling for cloud/remote paths.
- Fixed malformed HTML tag (`</a>` → `</span>`) in `paramsSummaryMultiqc()`.

## [0.4.0] - 2025-10-31

### Added

- Updated Nextflow Gradle plugin from version 1.0.0-beta.9 to 1.0.0-beta.12 for improved plugin development support
- New `getGenomeAttribute()` function to retrieve genome attributes from `params.genomes` for the selected genome

### Changed

- Changed pipeline start/complete messages from `log` to `TRACE` level to reduce console noise in normal operation
- Simplify and improve validation tests by using nft-utils for snapshots
- Enhanced `softwareVersionsToYAML()` to support mixed input sources including YAML strings, file paths, topic tuples, and maps ([#24](https://github.com/nf-core/nf-core-utils/pull/24))
- Improved Nextflow version detection in `workflowVersionToYAML()` with fallback to `NXF_VER` environment variable

### Fixed

- Fix versions needed for build and tests

## [0.3.1] - 2025-09-12

### Changed

- Updated Nextflow Gradle plugin from version 0.0.1-alpha3 to 1.0.0-beta.9 for improved plugin development support
- Simplified publishing configuration to use standard Nextflow plugin registry format

## [0.3.0] - 2025-08-27

### Added

- Topic channel citation management system for automatic citation collection ([#4](https://github.com/nf-core/nf-core-utils/issues/4), [#8](https://github.com/nf-core/nf-core-utils/pull/8))
- Comprehensive citation management with topic channel support ([#8](https://github.com/nf-core/nf-core-utils/pull/8))
- Topic-based version utilities for progressive migration ([#7](https://github.com/nf-core/nf-core-utils/pull/7))
- Color formatting for profile validation error messages ([#5](https://github.com/nf-core/nf-core-utils/issues/5), [#14](https://github.com/nf-core/nf-core-utils/pull/14))
- Comprehensive validation test suites with nf-test and snapshot testing

### Changed

- Standardized parameter naming from snake_case to camelCase
- Restructured documentation with modular organization ([#15](https://github.com/nf-core/nf-core-utils/pull/15))
- Optimized NfcoreNotificationUtils for better performance and code quality

### Fixed

- Notification system null pointer exceptions
- Pipeline utilities implementation issues
- Validation race conditions and improved YAML serialization

## [0.2.0] - 2025-06-23

### Changed

- Rename `nf-utils` to `nf-core-utils` ([#127](https://github.com/nextflow-io/plugins/pull/127))

## [0.1.0] - 2025-05-21

### Added

- Port Nextflow Pipeline Utils functions
- Port nf-core Pipeline Utils functions
- Port references Utils functions

[unreleased]: https://github.com/nf-core/nf-core-utils/compare/v0.3.0...HEAD
[0.3.0]: https://github.com/nf-core/nf-core-utils/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/nf-core/nf-core-utils/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/nf-core/nf-core-utils/releases/tag/v0.1.0

<!-- TODO For future releases: [1.1.1]: https://github.com/olivierlacan/keep-a-changelog/compare/v1.1.0...v1.1.1 -->
