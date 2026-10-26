# References Extension Guide

The References Extension simplifies genome reference and annotation file management in Nextflow pipelines. It provides intelligent parameter resolution that seamlessly handles both user-provided files and standardized reference collections like iGenomes, making your pipelines more flexible and user-friendly.

## 1. Overview

Managing reference files is a common challenge in bioinformatics pipelines. Users might provide:

- Custom reference files via parameters
- References from standardized collections (iGenomes)
- Mixed approaches depending on the analysis

The References Extension solves this by providing a unified interface that automatically resolves the appropriate reference source based on user input and pipeline configuration.

### 1.1. Core Capabilities

The extension provides two families of functions:

- **Datasheet resolution** (for new pipelines, see [3.3](#2-reference-datasheet-resolution)): `getReferencePath()`, `getReferenceMetadata()`, and `getReferenceAttribute()` resolve references from a single-genome references datasheet, falling back to the `params.genomes` configuration.
- **Genome configuration lookup** (see [1.2](#12-migration-from-legacy-systems)): `getGenomeAttribute()` reads one attribute of the selected genome from `params.genomes`, as used in the nf-core template.

### 1.2. Migration from Legacy Systems

This function is used to retrieve genome attributes in the nf-core TEMPLATE.

It retrieves a specific attribute (such as `fasta`, `gtf`, or index paths) for the selected genome from the `params.genomes` map. It is useful for pipelines that support multiple genomes and need to access reference files or metadata for the currently selected genome.

**Function Signature**

```groovy
Object getGenomeAttribute(String attribute)
```

or (static utility):

```groovy
Object ReferencesUtils.getGenomeAttribute(Map params, String attribute)
```

**Parameters**

| Parameter   | Type   | Required | Description                                                        |
| ----------- | ------ | -------- | ------------------------------------------------------------------ |
| `attribute` | String | Yes      | The attribute name to retrieve (e.g. 'fasta', 'gtf', 'star')       |
| `params`    | Map    | Yes      | (static) The Nextflow params map containing `genome` and `genomes` |

**Return Value**

Returns the value of the requested attribute for the selected genome, or `null` if not found.

**Practical Example**

```groovy
// Example params structure
def params = [
    genome: 'GRCh38',
    genomes: [
        GRCh38: [
            fasta: 's3://bucket/genome.fa',
            gtf: 's3://bucket/genes.gtf',
        ],
        GRCh37: [
            fasta: 's3://bucket/genome37.fa'
            star: 's3://bucket/star_index/'
        ]
    ]
]

// Retrieve the FASTA file for the selected genome
def fasta = ReferencesUtils.getGenomeAttribute(params, 'fasta')
// Returns: 's3://bucket/genome.fa'

// Retrieve the GTF file
def gtf = ReferencesUtils.getGenomeAttribute(params, 'gtf')
// Returns: 's3://bucket/genes.gtf'

// If the attribute or genome is missing, returns null
def missing = ReferencesUtils.getGenomeAttribute(params, 'star')
// Returns: null
```

**Usage in Nextflow**

```nextflow
include { getGenomeAttribute } from 'plugin/nf-core-utils'

workflow {
    // Example: get the FASTA file for the selected genome
    genome_fasta = getGenomeAttribute('fasta')
    log.info "Selected genome FASTA: ${genome_fasta}"
}
```

## 2. Reference Datasheet Resolution

These functions resolve reference parameters from a references datasheet. A datasheet describes exactly one genome, so its keys map directly to pipeline reference parameters.

Resolution goes from the datasheet to the `params.genomes` configuration: a value found in the datasheet wins, otherwise the genome configuration is used.

| Function                  | Purpose                                                                 |
| ------------------------- | ----------------------------------------------------------------------- |
| `getReferencePath()`      | Choose the datasheet to load, from `--references` or from `--genome`    |
| `getReferenceMetadata()`  | Turn the rows returned by `samplesheetToList()` into a metadata map     |
| `getReferenceAttribute()` | Read one attribute from the datasheet, falling back to `params.genomes` |

All three take named arguments.

```groovy
String getReferencePath(Map args)
// args: references, genome, igenomesIgnore, datasheetPath

Map getReferenceMetadata(Map args)
// args: rows

Object getReferenceAttribute(Map args)
// args: param, datasheet, genomeMap, genomeKey
```

`getReferencePath()` throws when `--references` and `--genome` are both set. It returns the `--references` path as-is, or builds `<datasheetPath>/<genome>.yml` when `--genome` is set together with `--igenomes_ignore`. It returns `null` when there is nothing to load.

`getReferenceMetadata()` throws when the rows contain no records or more than one genome.

```nextflow title="reference_datasheet.nf"
include { getReferencePath       } from 'plugin/nf-core-utils'
include { getReferenceMetadata   } from 'plugin/nf-core-utils'
include { getReferenceAttribute  } from 'plugin/nf-core-utils'
include { samplesheetToList      } from 'plugin/nf-schema'

workflow {
    reference_path = getReferencePath(
        references: params.references,
        genome: params.genome,
        igenomesIgnore: params.igenomes_ignore,
        datasheetPath: params.references_datasheets_path,
    )
    datasheet = reference_path
        ? getReferenceMetadata(rows: samplesheetToList(reference_path, "${projectDir}/assets/schema_references.json"))
        : null

    params.fasta = getReferenceAttribute(
        param: 'fasta',
        datasheet: datasheet,
        genomeMap: params.genomes,
        genomeKey: params.genome,
    )
}
```

Load the datasheet once, during pipeline initialisation, then pass the metadata map to the workflow that needs it. Passing a Nextflow value channel requires `.getVal()` at the call site.

## 3. What's Next?

- Explore **[NfCore Utilities](NfCoreUtilities.md)** for comprehensive pipeline utilities
- Learn about **[NextflowPipelineExtension](NextflowPipelineExtension.md)** for core pipeline functions
- Check out the utility documentation in `utilities/` for specialized functions
