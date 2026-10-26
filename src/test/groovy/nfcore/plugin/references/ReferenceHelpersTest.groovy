package nfcore.plugin.references

import spock.lang.Specification

class ReferenceHelpersTest extends Specification {

    private static final Map GENOMES = [
        GRCh38: [
            fasta: 's3://bucket/genome.fa',
            gtf: 's3://bucket/genes.gtf'
        ]
    ]

    def 'getReferencePath prefers an explicit references path'() {
        expect:
        ReferenceHelpers.getReferencePath(references: 'my.yml', genome: null, igenomesIgnore: false, datasheetPath: null) == 'my.yml'
    }

    def 'getReferencePath builds the catalogue path'() {
        expect:
        ReferenceHelpers.getReferencePath(references: null, genome: 'Ensembl/GRCh38', igenomesIgnore: true, datasheetPath: 'https://host/') == 'https://host/Ensembl/GRCh38.yml'
    }

    def 'getReferencePath returns null without a source'() {
        expect:
        ReferenceHelpers.getReferencePath(references: null, genome: 'GRCh38', igenomesIgnore: false, datasheetPath: null) == null
        ReferenceHelpers.getReferencePath(references: null, genome: 'null', igenomesIgnore: true, datasheetPath: 'x') == null
    }

    def 'getReferencePath rejects references and genome together'() {
        when:
        ReferenceHelpers.getReferencePath(references: 'my.yml', genome: 'GRCh38', igenomesIgnore: false, datasheetPath: null)

        then:
        thrown(IllegalArgumentException)
    }

    def 'loadDatasheetFile unwraps a samplesheet row'() {
        expect:
        ReferenceHelpers.loadDatasheetFile(datasheet: [[[genome: 'GRCh38', fasta: 'a.fa'], null]]) == [genome: 'GRCh38', fasta: 'a.fa']
    }

    def 'loadDatasheetFile drops empty values'() {
        expect:
        ReferenceHelpers.loadDatasheetFile(datasheet: [[[genome: 'GRCh38', fasta: null], null]]) == [genome: 'GRCh38']
    }

    def 'loadDatasheetFile rejects an empty datasheet'() {
        when:
        ReferenceHelpers.loadDatasheetFile(datasheet: [])

        then:
        thrown(IllegalArgumentException)
    }

    def 'loadDatasheetFile rejects multiple genomes'() {
        when:
        ReferenceHelpers.loadDatasheetFile(datasheet: [[[genome: 'GRCh38'], null], [[genome: 'GRCh37'], null]])

        then:
        thrown(IllegalArgumentException)
    }

    def 'getReferenceAttribute prefers the datasheet value'() {
        expect:
        ReferenceHelpers.getReferenceAttribute(param: 'fasta', datasheet: [fasta: 'sheet.fa'], genomeMap: GENOMES, genomeKey: 'GRCh38') == 'sheet.fa'
    }

    def 'getReferenceAttribute falls back to the genome map'() {
        expect:
        ReferenceHelpers.getReferenceAttribute(param: 'gtf', datasheet: [fasta: 'sheet.fa'], genomeMap: GENOMES, genomeKey: 'GRCh38') == 's3://bucket/genes.gtf'
    }

    def 'getReferenceAttribute returns null without a datasheet or genome'() {
        expect:
        ReferenceHelpers.getReferenceAttribute(param: 'fasta', datasheet: null, genomeMap: null, genomeKey: null) == null
    }

    def 'getReferenceDatasheet strips trailing slashes'() {
        expect:
        ReferenceHelpers.getReferenceDatasheet(genomeKey: 'Ensembl/GRCh38', basePath: 's3://bucket/refs//') == 's3://bucket/refs/Ensembl/GRCh38.yml'
    }

    def 'getReferenceDatasheet requires a base path'() {
        when:
        ReferenceHelpers.getReferenceDatasheet(genomeKey: 'Ensembl/GRCh38', basePath: '')

        then:
        thrown(IllegalArgumentException)
    }
}
