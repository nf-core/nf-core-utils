# Migration guides

These guides cover moving a pipeline between nf-core-utils releases, and moving a pipeline onto the plugin.

## Releases

- [Migrating to 1.0](1.0.md): new features, enhancements and deprecations for pipelines that already load a 0.x release.

## Moving onto the plugin

- [Migrating from the template utilities](template-utilities.md): replace the helpers that the nf-core pipeline template copies into a pipeline, and the `utils_nextflow_pipeline` and `utils_nfcore_pipeline` subworkflows, with imports from the plugin.
