include { getReferenceNames } from 'plugin/nf-core-utils'
include { getReferences     } from 'plugin/nf-core-utils'

workflow {
    def schema = "${projectDir}/schema_references.json"
    // Rows as samplesheetToList returns them for a one-genome datasheet
    def rows = params.datasheet ? [[[genome: 'GRCh38', fasta: 'datasheet.fa']]] : null
    def references = getReferences(
        schema: schema,
        rows: rows,
        params: params.subMap(getReferenceNames(schema: schema)),
        genome: params.genome,
        genomes: params.genomes,
    )
    println("references: ${references}")
}
