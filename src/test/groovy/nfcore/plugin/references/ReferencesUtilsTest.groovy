package nfcore.plugin

import spock.lang.Specification

class ReferencesUtilsTest extends Specification {
    def extension

    def setup() {
        extension = ReferencesUtils
    }

    def "test getReferencesFile with param and getReferencesValue without param"() {
        given:
        // Simulate references list data (List of [meta, _readme])
        def referencesList = [
                [[id: 'test', fasta: '/path/to/genome.fasta', species: 'Homo sapiens'], null],
                [[id: 'test2', fasta: '/path/to/genome2.fasta', species: 'Mus musculus'], null]
        ]
        def param_file = '/override/genome.fasta'
        def param_value = null
        def attribute_file = 'fasta'
        def attribute_value = 'species'
        def basepath = '/base/path'

        when:
        def result_file = ReferencesUtils.getReferencesFile(referencesList, param_file, attribute_file, basepath)
        def result_value = ReferencesUtils.getReferencesValue(referencesList, param_value, attribute_value)

        then:
        // getReferencesFile should use param_file for all
        result_file.size() == 2
        result_file.every { it[1].toString() == param_file }
        // getReferencesValue should use meta[attribute_value]
        result_value.size() == 2
        result_value[0][1] == 'Homo sapiens'
        result_value[1][1] == 'Mus musculus'
    }

    def "test getReferencesFile and getReferencesValue with nulls"() {
        given:
        def referencesList = [
                [[id: 'test', fasta: null, species: null], null]
        ]
        def param_file = null
        def param_value = null
        def attribute_file = 'fasta'
        def attribute_value = 'species'
        def basepath = '/base/path'

        when:
        def result_file = ReferencesUtils.getReferencesFile(referencesList, param_file, attribute_file, basepath)
        def result_value = ReferencesUtils.getReferencesValue(referencesList, param_value, attribute_value)

        then:
        result_file == [null]
        result_value == [null]
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
}
