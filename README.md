# nf-core-utils plugin

The nf-core-utils plugin provides utility functions used by nf-core pipelines.

## Features

- [NextflowPipelineExtension](docs/NextflowPipelineExtension.md)
- [ReferencesExtension](docs/ReferencesExtension.md)
- [NfCore Utilities](docs/NfCoreUtilities.md)

## Development setup

Install [mise](https://mise.jdx.dev/getting-started.html), then from the repository root:

```bash
mise install --locked
mise tasks ls
```

`mise.toml` defines the development commands and tools: Temurin Java 21 (required
by the Gradle plugin), Nextflow 26.10.2, and nf-test 0.9.3. `mise.lock` records exact
versions and checksums for Linux and macOS on x64 and ARM64. CI uses the same
configuration via `jdx/mise-action`.

Make is no longer used: replace `make <target>` with `mise run <target>`.
Available tasks are `assemble`, `test`, `clean`, `install`, `validate`,
`update-snapshots`, and `release`. The Gradle wrapper still manages Gradle itself.

Use `mise exec -- <command>` for tools outside a task (for example,
`mise exec -- nextflow -version`), or optionally
[activate mise in your shell](https://mise.jdx.dev/getting-started.html#activate-mise).
Put personal overrides in the ignored `mise.local.toml`. After changing tool
versions, run `mise lock --platform linux-x64,linux-arm64,macos-x64,macos-arm64`
and commit both configuration and lockfile.

## Building

To build the plugin:

```bash
mise run assemble
```

Run unit tests with `mise run test` and end-to-end tests with `mise run validate`.
Validation installs the plugin first. Regenerate snapshots only for intended
behavior changes with `mise run update-snapshots`.

### Build caching

Gradle's local build cache is enabled by default for builds and unit tests. Unchanged,
cacheable tasks can restore their outputs even after `./gradlew clean`; Gradle
invalidates entries when their declared inputs change. Use `--no-build-cache` to
disable reuse for a run, or `--rerun-tasks` to force task execution.

The build and validation workflows use `gradle/actions/setup-gradle` to cache
Gradle distributions, dependencies, and build outputs between GitHub Actions runs.
Only runs on the default branch write caches; pull requests restore them read-only.
The action's job summary reports cache usage. Configuration caching is not enabled.

For Amp orbs, `.agents/setup` installs JDK 21 and warms the Gradle cache by building
and testing the plugin, so future orb snapshots can reuse those dependencies and outputs.

## Testing with Nextflow

The plugin can be tested without a local Nextflow installation:

1. Build and install the plugin to your local Nextflow installation: `mise run install`
2. Run a pipeline with the plugin: `mise exec -- nextflow run hello -plugins nf-core-utils@0.1.0`

## Publish a release

1. Set `version` in `build.gradle` and update the plugin pins in `validation/`.
2. Add a dated release section to the [changelog](./CHANGELOG.md).
3. Set `npr.apiKey` in `$HOME/.gradle/gradle.properties` to a Nextflow Plugin Registry token authorized to publish `nf-core-utils` for `nf-core`.
   The Gradle plugin also accepts `NPR_API_KEY`. Do not commit credentials.
4. Build and test the release:

   ```bash
   mise run assemble
   mise run test
   mise run validate
   ```

5. Commit and push the release changes, then publish to the Nextflow Plugin Registry:

   ```bash
   mise run release
   ```

6. Verify that `nextflow plugin install nf-core-utils@<version>` downloads the release into a fresh `NXF_PLUGINS_DIR`.
7. Create a Git tag and GitHub release for the same commit, using the version number without a `v` prefix.
   The GitHub release triggers the documentation update on nf-co.re.

`releasePlugin` uploads the plugin to `https://registry.nextflow.io/api`. It does not run tests, create Git tags, or create GitHub releases.
No pull request to the legacy plugin index is needed.
