include { getReferenceNames } from 'plugin/nf-core-utils'
include { getReferences     } from 'plugin/nf-core-utils'
include { samplesheetToList } from 'plugin/nf-schema'

workflow {
    def schema = "${projectDir}/schema_references.json"
    def rows = params.datasheet ? samplesheetToList("${projectDir}/references.yml", schema, unrecognisedHeaders: 'skip') : null
    def references = getReferences(
        schema: schema,
        rows: rows,
        params: params.subMap(getReferenceNames(schema: schema)),
        genome: params.genome,
        genomes: params.genomes,
    )
    println("references: ${references}")
}
