package nfcore.plugin

import spock.lang.Specification

class ReferencesUtilsTest extends Specification {
    def extension

    def setup() {
        extension = ReferencesUtils
    }

    def "test getGenomeAttribute with valid params"() {
        given:
        def params = [
            genome: 'GRCh38',
            genomes: [
                GRCh38: [
                    fasta: 's3://bucket/genome.fa',
                    gtf: 's3://bucket/genes.gtf',
                    star: 's3://bucket/star_index/'
                ],
                GRCh37: [
                    fasta: 's3://bucket/genome37.fa'
                ]
            ]
        ]

        when:
        def fasta = ReferencesUtils.getGenomeAttribute(params, 'fasta')
        def gtf = ReferencesUtils.getGenomeAttribute(params, 'gtf')
        def star = ReferencesUtils.getGenomeAttribute(params, 'star')

        then:
        fasta == 's3://bucket/genome.fa'
        gtf == 's3://bucket/genes.gtf'
        star == 's3://bucket/star_index/'
    }

    def "test getGenomeAttribute with missing attribute"() {
        given:
        def params = [
            genome: 'GRCh38',
            genomes: [
                GRCh38: [
                    fasta: 's3://bucket/genome.fa'
                ]
            ]
        ]

        when:
        def result = ReferencesUtils.getGenomeAttribute(params, 'gtf')

        then:
        result == null
    }

    def "test getGenomeAttribute with missing genome"() {
        given:
        def params = [
            genome: 'GRCh99',
            genomes: [
                GRCh38: [
                    fasta: 's3://bucket/genome.fa'
                ]
            ]
        ]

        when:
        def result = ReferencesUtils.getGenomeAttribute(params, 'fasta')

        then:
        result == null
    }

    def "test getGenomeAttribute with null params"() {
        when:
        def result = ReferencesUtils.getGenomeAttribute(null, 'fasta')

        then:
        result == null
    }

    def "test getGenomeAttribute with missing genomes map"() {
        given:
        def params = [
            genome: 'GRCh38'
        ]

        when:
        def result = ReferencesUtils.getGenomeAttribute(params, 'fasta')

        then:
        result == null
    }

    def "test getGenomeAttribute with missing genome key"() {
        given:
        def params = [
            genomes: [
                GRCh38: [
                    fasta: 's3://bucket/genome.fa'
                ]
            ]
        ]

        when:
        def result = ReferencesUtils.getGenomeAttribute(params, 'fasta')

        then:
        result == null
    }

    def "test getGenomeAttribute with different value types"() {
        given:
        def params = [
            genome: 'test',
            genomes: [
                test: [
                    string_value: 'some_string',
                    list_value: ['item1', 'item2'],
                    map_value: [key: 'value'],
                    number_value: 42,
                    boolean_value: true
                ]
            ]
        ]

        when:
        def string_result = ReferencesUtils.getGenomeAttribute(params, 'string_value')
        def list_result = ReferencesUtils.getGenomeAttribute(params, 'list_value')
        def map_result = ReferencesUtils.getGenomeAttribute(params, 'map_value')
        def number_result = ReferencesUtils.getGenomeAttribute(params, 'number_value')
        def boolean_result = ReferencesUtils.getGenomeAttribute(params, 'boolean_value')

        then:
        string_result == 'some_string'
        list_result == ['item1', 'item2']
        map_result == [key: 'value']
        number_result == 42
        boolean_result == true
    }

    def "genomeExistsError aborts for an unknown genome and lists the available keys"() {
        when:
        ReferencesUtils.genomeExistsError([genome: 'hg99', genomes: [GRCh38: [:], GRCm39: [:]]])

        then:
        def e = thrown(nextflow.exception.AbortOperationException)
        e.message.contains("Genome 'hg99' not found in any config files provided to the pipeline.")
        e.message.contains('  GRCh38, GRCm39\n')
    }

    def "genomeExistsError passes for a known genome"() {
        when:
        ReferencesUtils.genomeExistsError([genome: 'GRCh38', genomes: [GRCh38: [:]]])

        then:
        noExceptionThrown()
    }

    def "genomeExistsError passes when genome is unset or genomes is missing or empty"() {
        when:
        ReferencesUtils.genomeExistsError(params)

        then:
        noExceptionThrown()

        where:
        params << [
            [genome: null, genomes: [GRCh38: [:]]],
            [genomes: [GRCh38: [:]]],
            [genome: 'hg99', genomes: [:]],
            [genome: 'hg99'],
            [:],
            null,
        ]
    }
}
