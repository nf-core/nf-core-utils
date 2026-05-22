package nfcore.plugin.references

import spock.lang.Narrative
import spock.lang.Issue
import spock.lang.See
import spock.lang.Specification
import spock.lang.Title

@Title("ReferenceSelection concentrates genome lookup and basepath substitution")
@Narrative("""
Deepens reference handling around the concept of a selected reference:
genome lookup, attribute extraction, basepath substitution, and null
handling are owned by one module instead of spread across callers.
""")
@Issue("https://github.com/nf-core/nf-core-utils/pull/41")
@See("https://github.com/nf-core/nf-core-utils/blob/main/docs/adr/0001-prioritize-deep-modules-around-execution-context-and-reporting.md#6-reference-selection-module")
class ReferenceSelectionTest extends Specification {

    private static final Map GENOMES = [
        GRCh38: [
            fasta: 's3://bucket/genome.fa',
            gtf: 's3://bucket/genes.gtf',
            star: 's3://bucket/star_index/'
        ],
        GRCh37: [
            fasta: 's3://bucket/genome37.fa'
        ]
    ]

    // --- getAttribute ---

    def 'getAttribute returns value for valid genome and attribute'() {
        given:
        def sel = new ReferenceSelection('GRCh38', GENOMES)

        expect:
        sel.getAttribute('fasta') == 's3://bucket/genome.fa'
        sel.getAttribute('gtf') == 's3://bucket/genes.gtf'
    }

    def 'getAttribute returns null for missing attribute'() {
        given:
        def sel = new ReferenceSelection('GRCh38', GENOMES)

        expect:
        sel.getAttribute('bowtie2') == null
    }

    def 'getAttribute returns null for missing genome'() {
        given:
        def sel = new ReferenceSelection('GRCh99', GENOMES)

        expect:
        sel.getAttribute('fasta') == null
    }

    def 'getAttribute returns null when genomes map is null'() {
        given:
        def sel = new ReferenceSelection('GRCh38', null)

        expect:
        sel.getAttribute('fasta') == null
    }

    def 'getAttribute returns null when genome key is null'() {
        given:
        def sel = new ReferenceSelection(null, GENOMES)

        expect:
        sel.getAttribute('fasta') == null
    }

    // --- fromParams factory ---

    def 'fromParams extracts genome and genomes from params map'() {
        given:
        def params = [genome: 'GRCh38', genomes: GENOMES]

        when:
        def sel = ReferenceSelection.fromParams(params)

        then:
        sel.getAttribute('fasta') == 's3://bucket/genome.fa'
    }

    def 'fromParams returns safe selection for null params'() {
        when:
        def sel = ReferenceSelection.fromParams(null)

        then:
        sel.getAttribute('fasta') == null
    }

    // --- resolveFile with basepath substitution ---

    def 'resolveFile substitutes igenomes_base in attribute value'() {
        given:
        def genomes = [test: [fasta: '${params.igenomes_base}/genome.fa']]
        def sel = new ReferenceSelection('test', genomes)

        when:
        def result = sel.resolveFile('fasta', '/data/igenomes')

        then:
        result == '/data/igenomes/genome.fa'
    }

    def 'resolveFile returns raw value when no basepath needed'() {
        given:
        def sel = new ReferenceSelection('GRCh38', GENOMES)

        when:
        def result = sel.resolveFile('fasta', '/data/igenomes')

        then:
        result == 's3://bucket/genome.fa'
    }

    def 'resolveFile returns null when attribute missing'() {
        given:
        def sel = new ReferenceSelection('GRCh38', GENOMES)

        expect:
        sel.resolveFile('bowtie2', '/base') == null
    }

    // --- getReferencesFile / getReferencesValue delegation ---

    def 'getReferencesFile uses param when provided'() {
        given:
        def sel = new ReferenceSelection('GRCh38', GENOMES)
        def refList = [
            [[id: 'sample1', fasta: '/igenomes/genome.fa'], null]
        ]

        when:
        def result = sel.getReferencesFile(refList, '/override.fa', 'fasta', '/base')

        then:
        result[0][1] == '/override.fa'
    }

    def 'getReferencesFile falls back to meta attribute with basepath substitution'() {
        given:
        def sel = new ReferenceSelection('GRCh38', GENOMES)
        def refList = [
            [[id: 'sample1', fasta: '${params.igenomes_base}/genome.fa'], null]
        ]

        when:
        def result = sel.getReferencesFile(refList, null, 'fasta', '/data/igenomes')

        then:
        result[0][1] == '/data/igenomes/genome.fa'
    }

    def 'getReferencesValue uses param when provided'() {
        given:
        def sel = new ReferenceSelection('GRCh38', GENOMES)
        def refList = [
            [[id: 'sample1', species: 'Homo sapiens'], null]
        ]

        when:
        def result = sel.getReferencesValue(refList, 'override', 'species')

        then:
        result[0][1] == 'override'
    }

    def 'getReferencesValue falls back to meta attribute'() {
        given:
        def sel = new ReferenceSelection('GRCh38', GENOMES)
        def refList = [
            [[id: 'sample1', species: 'Homo sapiens'], null]
        ]

        when:
        def result = sel.getReferencesValue(refList, null, 'species')

        then:
        result[0][1] == 'Homo sapiens'
    }
}
