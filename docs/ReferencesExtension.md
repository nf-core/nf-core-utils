# References Extension Guide

The References Extension resolves a pipeline's reference parameters (`fasta`, `gtf`, indices, known sites, ...) from one of two sources:

- The iGenomes-style `params.genomes` configuration, selected with `--genome`.
- A [references datasheet](https://github.com/nf-core/references-datasheets): a YAML file describing one genome, selected with `--references`, or with `--genome` plus `--igenomes_ignore`.

## 1. Overview

| Function                  | Source           | Purpose                                                                 |
| ------------------------- | ---------------- | ----------------------------------------------------------------------- |
| `getGenomeAttribute()`    | `params.genomes` | Read one attribute of the selected genome                               |
| `getReferencePath()`      | datasheet        | Choose the datasheet to load, from `--references` or from `--genome`    |
| `getReferenceMetadata()`  | datasheet        | Turn the rows returned by `samplesheetToList()` into a metadata map     |
| `getReferenceAttribute()` | both             | Read one attribute from the datasheet, falling back to `params.genomes` |

Pipelines that only support iGenomes need `getGenomeAttribute()`, as in the nf-core template. Pipelines that accept datasheets use the other three.

## 2. Genome Configuration Lookup

### 2.1. getGenomeAttribute

Returns one attribute (such as `fasta`, `gtf` or an index path) of the genome selected by `params.genome`, read from the `params.genomes` map.

```groovy
Object getGenomeAttribute(String attribute)
```

| Parameter   | Type   | Required | Description                                                  |
| ----------- | ------ | -------- | ------------------------------------------------------------ |
| `attribute` | String | Yes      | The attribute name to retrieve (e.g. 'fasta', 'gtf', 'star') |

It returns `null` when `params.genome` is unset, the genome is not in `params.genomes`, or the genome has no such attribute.

```nextflow title="genome_attribute.nf"
include { getGenomeAttribute } from 'plugin/nf-core-utils'

params.genome = 'GRCh38'
params.genomes = [
    GRCh38: [
        fasta: 's3://bucket/genome.fa',
        gtf: 's3://bucket/genes.gtf',
    ],
]

params.fasta = getGenomeAttribute('fasta') // 's3://bucket/genome.fa'
params.star = getGenomeAttribute('star')   // null

workflow {
    log.info "Selected genome FASTA: ${params.fasta}"
}
```

## 3. Reference Datasheet Resolution

A references datasheet describes exactly one genome, so its keys map directly to pipeline reference parameters. Datasheets for the iGenomes catalogue are published in [nf-core/references-datasheets](https://github.com/nf-core/references-datasheets).

All three functions take named arguments. Arguments you leave out default to `null`, except `param` for `getReferenceAttribute()` and `rows` for `getReferenceMetadata()`, which are required. An unknown argument name, such as a typo, throws an `IllegalArgumentException`.

### 3.1. getReferencePath

```groovy
String getReferencePath(Map args)
// args (all optional, default null): references, genome, igenomesIgnore, datasheetPath
```

Chooses the datasheet to load:

| Input                                                   | Result                                                     |
| ------------------------------------------------------- | ---------------------------------------------------------- |
| `references` set, `genome` unset                        | `references`, unchanged                                    |
| `genome` set, `igenomesIgnore` true                     | `<datasheetPath>/<genome>.yml`                             |
| `genome` set, `igenomesIgnore` false                    | `null`: use `getGenomeAttribute()` and the iGenomes config |
| neither set                                             | `null`                                                     |
| `references` and `genome` both set                      | throws                                                     |
| `genome` set, `igenomesIgnore` true, no `datasheetPath` | throws                                                     |

`genome` is a path under `datasheetPath`, so `--genome GATK/GRCh38` loads `<datasheetPath>/GATK/GRCh38.yml`. Trailing slashes on `datasheetPath` are dropped.

A `genome` of `null`, `''` or the string `'null'` counts as unset. A pipeline whose `genome` has a default value can then accept `--references` with `--genome null`.

### 3.2. getReferenceMetadata

```groovy
Map getReferenceMetadata(Map args)
// args: rows (required)
```

Takes the rows returned by `samplesheetToList()` and returns the genome's metadata as a map, without null or empty values. It throws when the datasheet has no records or more than one genome.

`getReferenceMetadata()` reads the meta map of the single row. Map every property in the pipeline's datasheet schema (`assets/schema_references.json`) to `meta`, or that property is dropped:

```json
"fasta": {
    "type": "string",
    "format": "file-path",
    "meta": ["fasta"]
}
```

Datasheet paths may use a `${params.references_base_path}` placeholder instead of a hardcoded root. nf-schema 3.0.0 or later substitutes it when `samplesheetToList()` reads the datasheet.

### 3.3. getReferenceAttribute

```groovy
Object getReferenceAttribute(Map args)
// args: param (required); datasheet, genomeMap, genomeKey (optional, default null)
```

Returns `datasheet[param]` when the datasheet has a value. Otherwise, it returns `param` from `genomeMap[genomeKey]`, or `null`.

### 3.4. Example

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

## 4. What's Next?

- Explore **[NfCore Utilities](NfCoreUtilities.md)** for comprehensive pipeline utilities
- Learn about **[NextflowPipelineExtension](NextflowPipelineExtension.md)** for core pipeline functions
- Check out the utility documentation in `utilities/` for specialized functions
