package nfcore.plugin

import spock.lang.Specification

class NfUtilsExtensionTest extends Specification {

    private final NfUtilsExtension extension = new NfUtilsExtension()

    private static final List REFERENCES = [
        [[id: 'sample1', fasta: '${params.igenomes_base}/genome.fa', species: 'Homo sapiens'], null]
    ]

    def 'getReferencesFile delegates to ReferencesUtils'() {
        expect:
        extension.getReferencesFile(REFERENCES, null, 'fasta', '/igenomes') == [[[id: 'sample1'], '/igenomes/genome.fa']]
    }

    def 'getReferencesValue delegates to ReferencesUtils'() {
        expect:
        extension.getReferencesValue(REFERENCES, null, 'species') == [[[id: 'sample1'], 'Homo sapiens']]
    }
}
