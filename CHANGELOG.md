# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Added a manually triggered release workflow that validates the plugin, publishes to the Nextflow Plugin Registry, verifies a fresh download, creates the GitHub release, and dispatches the documentation update.
- Added full end-to-end CI validation for every supported stable Nextflow line (25.10, 26.04, and 26.10), retaining 25.10.4 as an additional startup-validation regression target.

### Fixed

- Removed observer-owned startup validation to avoid duplicate warnings when pipelines call exported or template-local checks. Pipelines relying only on the observer must explicitly call `checkConfigProvided()` and `checkProfileProvided(args)` during initialization. Profile validation now aborts with one `ERROR` label instead of two; loading the plugin alone no longer attempts validation whose errors Nextflow swallows ([#73](https://github.com/nf-core/nf-core-utils/issues/73)).
- Fixed `completionEmail()` renaming remote MultiQC attachments to generated temporary filenames. Attachments keep their original basename, and plaintext emails or runs without an email recipient no longer read/download reports or render the sendmail template. Temporary copies and their directories are cleaned up without changing summary report persistence ([#74](https://github.com/nf-core/nf-core-utils/issues/74)).

## [0.6.0] - 2026-10-17

### Added

- Added `citationsOnTheFly()` and `toolsFromVersionsTopic()` to build tool citations from the `versions` topic (the tools that actually ran), resolved from module `meta.yml` — no per-module changes or hand-maintained tool list. Short citations are publication-ready, `tool (version, Author et al. year)`, with graceful fallback to DOI/url. Tools that never emit a version (e.g. MultiQC plugins like `multiqcsav`) can be added manually via `extraTools` ([#43](https://github.com/nf-core/nf-core-utils/pull/43)).

### Changed

- Renamed the prek configuration and its CI workflow back to the standard nf-core `.pre-commit-config.yaml` and `pre-commit.yml`, with the same hooks ([#38](https://github.com/nf-core/nf-core-utils/issues/38)).
- The docs are published on nf-co.re on each release, with admonitions written in the nf-core website syntax, without emojis, and with shorter headings for the table of contents.
- Enabled Gradle build caching, managed CI caches with `setup-gradle`, and added cache warming for Amp orbs.
- Replaced Make commands with `mise run` tasks and a shared, locked Java 21, Nextflow, and nf-test toolchain for development and CI.

### Fixed

- Fixed the workflow version of `collectVersions()`, `completionEmail()` and the other version reports missing the `-g<sha>` commit suffix of runs from a repository: `getWorkflowVersion()` without arguments now adds the commit of the run, as the template helpers did ([#68](https://github.com/nf-core/nf-core-utils/issues/68)).
- Fixed the startup config checks warning on every run: the profile and config files are read from the session, where Nextflow keeps them, and the positional argument check looks at the arguments of the script instead of the command line, which starts with `nextflow` ([#66](https://github.com/nf-core/nf-core-utils/issues/66)).
- Fixed the warnings, errors and completion summary of the plugin never reaching the console: the plugin logs under `nextflow.nfcore`, as the console only shows the loggers under `nextflow` ([#67](https://github.com/nf-core/nf-core-utils/issues/67)).
- Fixed `completionEmail()` never attaching the MultiQC report with an object storage work directory (`s3://`, `gs://`, `az://`): the report is copied locally for the sendmail template, which reads it with `new File()`, and `getSingleReport()` keeps the file system of a report given as a string. A remote `outdir` no longer creates a local `s3:` directory ([#65](https://github.com/nf-core/nf-core-utils/issues/65)).
- Fixed `Nextflow: unknown` in the version reports without a Nextflow version given or `NXF_VER` set: they fall back to the version of the running Nextflow.
- Fixed `completionEmail()` ignoring `plaintext_email`: a plaintext e-mail is sent as text with `mail`, and a failure to send the e-mail is a warning instead of an error in `onComplete`.
- Fixed `completionEmail()` aborting on an unwritable, unused `.sendmail_tmp.html` file and reporting success before the mail command finished: it now supplies input through a temporary file, checks the exit status, and terminates commands after 30 seconds before trying the fallback. Reports are saved before delivery, failed attachment copies are cleaned up, and cleanup errors remain warnings ([#71](https://github.com/nf-core/nf-core-utils/pull/71)).
- Removed webhook URLs and potentially credential-bearing response bodies and exception messages from notification logs. Notification validation no longer depends on real webhook or mail delivery ([#71](https://github.com/nf-core/nf-core-utils/pull/71)).
- Fixed malformed HTML tag (`</a>` → `</span>`) in `paramsSummaryMultiqc()`.
- Fixed broken links: the README link to the `NfCorePipelineExtension` docs, deleted with `NfcorePipelineUtils`, and the links to moved pages of the nf-core website in the utility docs.

## [0.5.0] - 2026-05-22

### Added

- Added a `PipelineExecutionContext` seam and focused validation, version, and reporting adapters to keep Nextflow extension functions thin.
- Added `SoftwareVersionReport` as the canonical implementation for merging heterogeneous software-version inputs.
- Added `ReferenceSelection` for genome/reference lookup and `igenomes_base` substitution.

### Changed

- Updated reporting orchestration to use explicit execution context data, including workflow metadata for methods-description templates.
- Migrated pre-commit configuration to `prek.toml` and added validation snapshot checks.

### Fixed

- Fixed `dumpParametersToJSON()` to serialize Nextflow parameter types and copy parameter reports through Nextflow file handling for cloud/remote paths.

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

[unreleased]: https://github.com/nf-core/nf-core-utils/compare/0.6.0...HEAD
[0.6.0]: https://github.com/nf-core/nf-core-utils/compare/0.5.0...0.6.0
[0.5.0]: https://github.com/nf-core/nf-core-utils/compare/0.4.0...0.5.0
[0.3.0]: https://github.com/nf-core/nf-core-utils/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/nf-core/nf-core-utils/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/nf-core/nf-core-utils/releases/tag/v0.1.0

<!-- TODO For future releases: [1.1.1]: https://github.com/olivierlacan/keep-a-changelog/compare/v1.1.0...v1.1.1 -->
