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

<!-- TODO ## Publishing -->

<!-- Plugins can be published to a central plugin registry to make them accessible to the Nextflow community.


Follow these steps to publish the plugin to the Nextflow Plugin Registry:

1. Create a file named `$HOME/.gradle/gradle.properties`, where $HOME is your home directory. Add the following properties:

    * `pluginRegistry.accessToken`: Your Nextflow Plugin Registry access token.

2. Use the following command to package and create a release for your plugin on GitHub: `mise run release`.


> [!NOTE]
> The Nextflow Plugin registry is currently available as private beta technology. Contact info@nextflow.io to learn how to get access to it.
>  -->

## Package, Upload, and Publish

Following these step to package, upload and publish the plugin:

1. In `build.gradle` make sure that:
   - `version` matches the desired release version,
   - `github.repository` matches the repository of the plugin,
   - `github.indexUrl` points to your fork of the plugins index repository.

2. Create a file named `$HOME/.gradle/gradle.properties`, where `$HOME` is your home directory. Add the following properties:
   - `github_username`: The GitHub username granting access to the plugin repository.
   - `github_access_token`: The GitHub access token required to upload and commit changes to the plugin repository.
   - `github_commit_email`: The email address associated with your GitHub account.

3. Update the [changelog](./CHANGELOG.md).

4. Build and publish the plugin to your GitHub repository:

   ```bash
   mise run release
   ```

5. Create a pull request against the [nextflow-io/plugins](https://github.com/nextflow-io/plugins/blob/main/plugins.json) repository to make the plugin publicly accessible.
