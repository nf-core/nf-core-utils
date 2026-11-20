package nfcore.plugin.references

import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

class ReferenceDatasheetTest extends Specification {

    def 'getReferencePath prefers an explicit references path'() {
        expect:
        ReferenceDatasheet.getReferencePath(references: 'my.yml', genome: null, igenomesIgnore: false, datasheetPath: null) == 'my.yml'
    }

    def 'getReferencePath builds the catalogue path'() {
        expect:
        ReferenceDatasheet.getReferencePath(references: null, genome: 'Ensembl/GRCh38', igenomesIgnore: true, datasheetPath: 'https://host/') == 'https://host/Ensembl/GRCh38.yml'
        ReferenceDatasheet.getReferencePath(references: null, genome: 'Ensembl/GRCh38', igenomesIgnore: true, datasheetPath: 's3://bucket/refs//') == 's3://bucket/refs/Ensembl/GRCh38.yml'
    }

    def 'getReferencePath requires a datasheet base path'() {
        when:
        ReferenceDatasheet.getReferencePath(references: null, genome: 'Ensembl/GRCh38', igenomesIgnore: true, datasheetPath: '')

        then:
        thrown(IllegalArgumentException)
    }

    def 'getReferencePath returns null without a source'() {
        expect:
        ReferenceDatasheet.getReferencePath(references: null, genome: 'GRCh38', igenomesIgnore: false, datasheetPath: null) == null
        ReferenceDatasheet.getReferencePath(references: null, genome: 'null', igenomesIgnore: true, datasheetPath: 'x') == null
    }

    def 'getReferencePath rejects references and genome together'() {
        when:
        ReferenceDatasheet.getReferencePath(references: 'my.yml', genome: 'GRCh38', igenomesIgnore: false, datasheetPath: null)

        then:
        thrown(IllegalArgumentException)
    }

    def 'getReferencePath rejects unknown named arguments'() {
        when:
        ReferenceDatasheet.getReferencePath(references: null, igenomeIgnore: true)

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains('igenomeIgnore')
    }

    def 'getReferencePath and getReferenceNames check their named arguments'() {
        expect:
        ReferenceDatasheet.getReferencePath(references: 'my.yml') == 'my.yml'
        ReferenceDatasheet.getReferencePath([:]) == null

        when:
        ReferenceDatasheet.getReferenceNames([:])

        then:
        thrown(IllegalArgumentException)
    }

    @TempDir
    Path tempDir

    private String schema() {
        def file = tempDir.resolve('schema_references.json')
        file.text = '''{"items": {"properties": {
            "genome": {"meta": ["genome", "id"]},
            "fasta": {"meta": ["fasta"]},
            "fasta_dict": {"meta": ["dict"]}
        }}}'''
        return file.toString()
    }

    def 'getReferenceNames skips the meta names in ignore, genome and id by default'() {
        expect:
        ReferenceDatasheet.getReferenceNames(schema: schema()) == ['fasta', 'dict']
        ReferenceDatasheet.getReferenceNames(schema: schema(), ignore: []) == ['genome', 'id', 'fasta', 'dict']
        ReferenceDatasheet.getReferenceNames(schema: schema(), ignore: ['genome', 'id', 'dict']) == ['fasta']
    }

    def 'getReferences reads the genomes config without datasheet rows'() {
        expect:
        ReferenceDatasheet.getReferences(schema: schema(), genome: 'GRCh38', genomes: [GRCh38: [fasta: 'g.fa', dict: 'g.dict', gtf: 'g.gtf']]) ==
            [fasta: 'g.fa', dict: 'g.dict']
    }

    def 'getReferences reads only the datasheet when rows are given'() {
        expect:
        ReferenceDatasheet.getReferences(schema: schema(), rows: [[[genome: 'GRCh38', fasta: 'd.fa']]],
            genome: 'GRCh38', genomes: [GRCh38: [fasta: 'g.fa', dict: 'g.dict']]) == [fasta: 'd.fa', dict: null]
    }

    def 'getReferences reads null as unset and false as off, also as command-line text'() {
        expect:
        ReferenceDatasheet.getReferences(schema: schema(), rows: [[[genome: 'GRCh38', fasta: 'd.fa', dict: 'd.dict']]],
            params: [fasta: 'p.fa', dict: value, genome: 'GRCh38']) == [fasta: 'p.fa', dict: expected]

        where:
        value   | expected
        null    | 'd.dict'
        'null'  | 'd.dict'
        false   | null
        'false' | null
    }

    def 'getReferences resolves only the names getReferenceNames returns for the same ignore'() {
        expect:
        ReferenceDatasheet.getReferences(schema: schema(), rows: [[[genome: 'GRCh38', fasta: 'd.fa', dict: 'd.dict']]],
            params: [dict: 'p.dict'], ignore: ['genome', 'id', 'dict']) == [fasta: 'd.fa']
    }

    def 'getReferences aborts for an unknown genome without datasheet rows'() {
        when:
        ReferenceDatasheet.getReferences(schema: schema(), genome: 'hg99', genomes: [GRCh38: [:]])

        then:
        def e = thrown(nextflow.exception.AbortOperationException)
        e.message.contains("Genome 'hg99' not found in any config files provided to the pipeline.")
    }

    def 'getReferences skips the genome key check with datasheet rows'() {
        expect:
        ReferenceDatasheet.getReferences(schema: schema(), rows: [[[genome: 'Ensembl/GRCh38', fasta: 'd.fa']]],
            genome: 'Ensembl/GRCh38', genomes: [GRCh38: [:]]) == [fasta: 'd.fa', dict: null]
    }

    def 'getReferences rejects a schema without items.properties'() {
        given:
        def file = tempDir.resolve('bad.json')
        file.text = '{"type": "array"}'

        when:
        ReferenceDatasheet.getReferences(schema: file.toString())

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains('has no items.properties')
    }
}
