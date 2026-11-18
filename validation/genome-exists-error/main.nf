include { genomeExistsError } from 'plugin/nf-core-utils'

workflow {
    genomeExistsError()
    println("genomeExistsError OK")
}
