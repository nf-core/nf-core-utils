include { genomeExistsError } from 'plugin/nf-core-utils'

workflow {
    genomeExistsError(genome: params.genome, genomes: params.genomes)
    println("genomeExistsError OK")
}
