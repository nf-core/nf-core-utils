package nfcore.plugin

import spock.lang.Specification

class ReferencesExtensionTest extends Specification {
    def extension = new ReferencesExtension()

    def setup() {
        extension.init(null) // No session needed for these tests
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
        def result_file = extension.getReferencesFile(referencesList, param_file, attribute_file, basepath)
        def result_value = extension.getReferencesValue(referencesList, param_value, attribute_value)

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
        def result_file = extension.getReferencesFile(referencesList, param_file, attribute_file, basepath)
        def result_value = extension.getReferencesValue(referencesList, param_value, attribute_value)

        then:
        result_file == [null]
        result_value == [null]
    }
} 