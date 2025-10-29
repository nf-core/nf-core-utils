# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- New `getGenomeAttribute()` function to retrieve genome attributes from `params.genomes` for the selected genome

### Changed

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
