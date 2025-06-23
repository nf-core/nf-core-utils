# ReferencesExtension

This extension provides utility functions for handling reference files and values in Nextflow pipelines that were previously available in the `utils_references` subworkflow:

```nextflow
// Import utilities
include { getReferencesFile; getReferencesValue } from 'plugin/nf-core-utils'

// Get reference files from a references YAML file or parameters
references = Channel.fromList(samplesheetToList(yaml_reference, "${projectDir}/assets/schema_references.json"))
references_file = getReferencesFile(references, param_file, 'attribute_file', basepath)

// Get reference values from a references YAML file or parameters
references_value = getReferencesValue(references, param_value, 'attribute_value')
```

## Functions

### getReferencesFile

Get reference files from a references channel based on parameters or metadata attributes.

**Parameters:**
- `references`: A channel containing reference metadata and readme
- `param`: The file parameter that would override the metadata attribute
- `attribute`: The attribute to look for in metadata
- `basepath`: The base path for igenomes paths

**Returns:**
- A channel of reference files with metadata

### getReferencesValue

Get reference values from a references channel based on parameters or metadata attributes.

**Parameters:**
- `references`: A channel containing reference metadata and readme
- `param`: The value parameter that would override the metadata attribute
- `attribute`: The attribute to look for in metadata

**Returns:**
- A channel of reference values with metadata 