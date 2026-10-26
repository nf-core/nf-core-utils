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

    // --- getReferencePath / getReferenceMetadata / getReferenceAttribute ---

    def 'getReferencePath prefers an explicit references path'() {
        expect:
        ReferenceSelection.getReferencePath(references: 'my.yml', genome: null, igenomesIgnore: false, datasheetPath: null) == 'my.yml'
    }

    def 'getReferencePath builds the catalogue path'() {
        expect:
        ReferenceSelection.getReferencePath(references: null, genome: 'Ensembl/GRCh38', igenomesIgnore: true, datasheetPath: 'https://host/') == 'https://host/Ensembl/GRCh38.yml'
        ReferenceSelection.getReferencePath(references: null, genome: 'Ensembl/GRCh38', igenomesIgnore: true, datasheetPath: 's3://bucket/refs//') == 's3://bucket/refs/Ensembl/GRCh38.yml'
    }

    def 'getReferencePath requires a datasheet base path'() {
        when:
        ReferenceSelection.getReferencePath(references: null, genome: 'Ensembl/GRCh38', igenomesIgnore: true, datasheetPath: '')

        then:
        thrown(IllegalArgumentException)
    }

    def 'getReferencePath returns null without a source'() {
        expect:
        ReferenceSelection.getReferencePath(references: null, genome: 'GRCh38', igenomesIgnore: false, datasheetPath: null) == null
        ReferenceSelection.getReferencePath(references: null, genome: 'null', igenomesIgnore: true, datasheetPath: 'x') == null
    }

    def 'getReferencePath rejects references and genome together'() {
        when:
        ReferenceSelection.getReferencePath(references: 'my.yml', genome: 'GRCh38', igenomesIgnore: false, datasheetPath: null)

        then:
        thrown(IllegalArgumentException)
    }

    def 'getReferenceMetadata unwraps a samplesheet row'() {
        expect:
        ReferenceSelection.getReferenceMetadata(rows: [[[genome: 'GRCh38', fasta: 'a.fa'], null]]) == [genome: 'GRCh38', fasta: 'a.fa']
    }

    def 'getReferenceMetadata drops empty values'() {
        expect:
        ReferenceSelection.getReferenceMetadata(rows: [[[genome: 'GRCh38', fasta: null], null]]) == [genome: 'GRCh38']
    }

    def 'getReferenceMetadata rejects an empty datasheet'() {
        when:
        ReferenceSelection.getReferenceMetadata(rows: [])

        then:
        thrown(IllegalArgumentException)
    }

    def 'getReferenceMetadata rejects multiple genomes'() {
        when:
        ReferenceSelection.getReferenceMetadata(rows: [[[genome: 'GRCh38'], null], [[genome: 'GRCh37'], null]])

        then:
        thrown(IllegalArgumentException)
    }

    def 'getReferenceAttribute prefers the datasheet value'() {
        expect:
        ReferenceSelection.getReferenceAttribute(param: 'fasta', datasheet: [fasta: 'sheet.fa'], genomeMap: GENOMES, genomeKey: 'GRCh38') == 'sheet.fa'
    }

    def 'getReferenceAttribute falls back to the genome map'() {
        expect:
        ReferenceSelection.getReferenceAttribute(param: 'gtf', datasheet: [fasta: 'sheet.fa'], genomeMap: GENOMES, genomeKey: 'GRCh38') == 's3://bucket/genes.gtf'
    }

    def 'getReferenceAttribute returns null without a datasheet or genome'() {
        expect:
        ReferenceSelection.getReferenceAttribute(param: 'fasta', datasheet: null, genomeMap: null, genomeKey: null) == null
    }

    def 'reference functions reject missing named arguments'() {
        when:
        ReferenceSelection.getReferencePath(references: null, genome: null)

        then:
        thrown(IllegalArgumentException)
    }
}
