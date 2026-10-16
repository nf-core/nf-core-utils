# nf-core-utils plugin

The nf-core-utils plugin provides utility functions used by nf-core pipelines.

## Features

- [NextflowPipelineExtension](docs/NextflowPipelineExtension.md)
- [ReferencesExtension](docs/ReferencesExtension.md)
- [NfCore Utilities](docs/NfCoreUtilities.md)

## Building

To build the plugin:

```bash
make assemble
```

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

1. Build and install the plugin to your local Nextflow installation: `make install`
2. Run a pipeline with the plugin: `nextflow run hello -plugins nf-core-utils@0.1.0`

<!-- TODO ## Publishing -->

<!-- Plugins can be published to a central plugin registry to make them accessible to the Nextflow community.


Follow these steps to publish the plugin to the Nextflow Plugin Registry:

1. Create a file named `$HOME/.gradle/gradle.properties`, where $HOME is your home directory. Add the following properties:

    * `pluginRegistry.accessToken`: Your Nextflow Plugin Registry access token.

2. Use the following command to package and create a release for your plugin on GitHub: `make release`.


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
   make release
   ```

5. Create a pull request against the [nextflow-io/plugins](https://github.com/nextflow-io/plugins/blob/main/plugins.json) repository to make the plugin publicly accessible.
