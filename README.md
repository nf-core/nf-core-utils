# nf-core-utils plugin

The nf-core-utils plugin provides utility functions used by nf-core pipelines.

## Features

- [NextflowPipelineExtension](docs/NextflowPipelineExtension.md)
- [NfCorePipelineExtension](docs/NfCorePipelineExtension.md)
- [ReferencesExtension](docs/ReferencesExtension.md)
- [NfCore Utilities](docs/NfCoreUtilities.md)

## Building

To build the plugin:

```bash
make assemble
```

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
