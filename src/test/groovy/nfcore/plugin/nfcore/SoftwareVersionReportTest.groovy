package nfcore.plugin.nfcore

import spock.lang.Narrative
import spock.lang.Issue
import spock.lang.See
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Title
import nextflow.script.types.Record
import org.yaml.snakeyaml.Yaml

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path

@Title("SoftwareVersionReport collects heterogeneous version inputs and renders YAML")
@Narrative("""
Core version collection behaviour extracted from NfcoreVersionUtils behind a
smaller canonical interface: addInput to collect, renderYaml to output.
NfcoreVersionUtils.collectVersions delegates here; deprecated methods remain
as thin adapters.
""")
@Issue("https://github.com/nf-core/nf-core-utils/pull/41")
@See("https://github.com/nf-core/nf-core-utils/blob/main/docs/adr/0001-prioritize-deep-modules-around-execution-context-and-reporting.md#3-software-version-report-module")
class SoftwareVersionReportTest extends Specification {

    @TempDir
    Path tempDir

    def 'renders empty report as empty string'() {
        given:
        def report = new SoftwareVersionReport()

        expect:
        report.renderYaml() == ''
    }

    def 'collects YAML string input'() {
        given:
        def report = new SoftwareVersionReport()

        when:
        report.addInput('fastqc: 0.12.1\nsamtools: 1.17')

        then:
        def yaml = report.renderYaml()
        yaml.contains('fastqc: 0.12.1')
        yaml.contains('samtools: 1.17')
    }

    def 'collects topic tuples'() {
        given:
        def report = new SoftwareVersionReport()

        when:
        report.addInput([
            ['NFCORE_FASTQC', 'fastqc', '0.12.1'],
            ['NFCORE_SAMTOOLS', 'samtools', '1.17']
        ])

        then:
        def yaml = report.renderYaml()
        yaml.contains('NFCORE_FASTQC:')
        yaml.contains('fastqc: 0.12.1')
        yaml.contains('NFCORE_SAMTOOLS:')
    }

    def 'collects Map input under Software process'() {
        given:
        def report = new SoftwareVersionReport()

        when:
        report.addInput([fastqc: '0.12.1'])

        then:
        def yaml = report.renderYaml()
        yaml.contains('Software:')
        yaml.contains('fastqc: 0.12.1')
    }

    def 'collects File input'() {
        given:
        def tempFile = File.createTempFile('versions', '.yml')
        tempFile.text = 'fastqc: 0.12.1'
        def report = new SoftwareVersionReport()

        when:
        report.addInput([tempFile])

        then:
        report.renderYaml().contains('fastqc: 0.12.1')

        cleanup:
        tempFile.delete()
    }

    @Issue("https://github.com/nf-core/nf-core-utils/pull/23")
    def 'reads #provider Path input with collected=#collected'() {
        given:
        def archive = provider == 'zip' ? FileSystems.newFileSystem(tempDir.resolve('versions.zip'), [create: 'true']) : null
        def versionsPath = archive ? archive.getPath('/versions.yml') : tempDir.resolve('versions.yml')
        Files.writeString(versionsPath, 'PIPELINE:ALIGN:\n  samtools: "1.17"\n  bwa: "0.7.17"\n')
        def report = new SoftwareVersionReport()

        when:
        report.addInput(collected ? [versionsPath, ['PIPELINE:ALIGN', 'samtools', '1.17']] : versionsPath)

        then:
        new Yaml().load(report.renderYaml()) == [ALIGN: [bwa: '0.7.17', samtools: '1.17']]

        cleanup:
        archive?.close()

        where:
        provider | collected
        'local'  | false
        'local'  | true
        'zip'    | false
        'zip'    | true
    }

    def 'reads a path string with a URI scheme through its file system'() {
        given:
        def versionsPath = tempDir.resolve('versions.yml')
        Files.writeString(versionsPath, 'fastqc: 0.12.1')
        def report = new SoftwareVersionReport()

        when:
        report.addInput(versionsPath.toUri().toString())

        then:
        report.renderYaml().contains('fastqc: 0.12.1')
    }

    def 'skips a missing Path'() {
        given:
        def report = new SoftwareVersionReport()

        when:
        report.addInput([tempDir.resolve('missing.yml'), ['FASTQC', 'fastqc', '0.12.1']])

        then:
        report.renderYaml() == 'FASTQC:\n  fastqc: 0.12.1'
    }

    def 'handles mixed inputs in a single addInput call'() {
        given:
        def report = new SoftwareVersionReport()

        when:
        report.addInput([
            'fastqc: 0.12.1',
            ['NFCORE_SAMTOOLS', 'samtools', '1.17'],
            [multiqc: '1.15']
        ])

        then:
        def yaml = report.renderYaml()
        yaml.contains('fastqc: 0.12.1')
        yaml.contains('samtools:')
        yaml.contains('multiqc:')
    }

    def 'collects a versions record like a topic tuple'() {
        given:
        def report = new SoftwareVersionReport()

        when:
        report.addInput([versionsRecord('NFCORE:PRESEQ_LCEXTRAP', 'preseq', '3.2.0')])

        then:
        report.renderYaml() == "PRESEQ_LCEXTRAP:\n  preseq: 3.2.0"
    }

    def 'collects a single versions record'() {
        given:
        def report = new SoftwareVersionReport()

        when:
        report.addInput(versionsRecord('PRESEQ_LCEXTRAP', 'preseq', '3.2.0'))

        then:
        report.renderYaml() == "PRESEQ_LCEXTRAP:\n  preseq: 3.2.0"
    }

    def 'merges tuples, records and versions.yml content together'() {
        given:
        def report = new SoftwareVersionReport()
        def versionsYml = tempDir.resolve('versions.yml')
        Files.writeString(versionsYml, "PIPELINE:LEGACY_TOOL:\n  legacy: '2.0'\n")

        when:
        report.addInput([
            ['NFCORE:FASTQC', 'fastqc', '0.12.1'],
            versionsRecord('NFCORE:PRESEQ_LCEXTRAP', 'preseq', '3.2.0'),
            versionsYml,
            [[versionsRecord('NFCORE:SAMTOOLS_SORT', 'samtools', '1.21')]]
        ])

        then:
        new Yaml().load(report.renderYaml()) == [
            FASTQC         : [fastqc: '0.12.1'],
            LEGACY_TOOL    : [legacy: '2.0'],
            PRESEQ_LCEXTRAP: [preseq: '3.2.0'],
            SAMTOOLS_SORT  : [samtools: '1.21'],
        ]
    }

    def 'a plain map with process, tool and version keys stays versions.yml content'() {
        given:
        def report = new SoftwareVersionReport()

        when:
        report.addInput([process: 'PRESEQ_LCEXTRAP', tool: 'preseq', version: '3.2.0'])

        then:
        new Yaml().load(report.renderYaml()) == [
            Software: [process: 'PRESEQ_LCEXTRAP', tool: 'preseq', version: '3.2.0']
        ]
    }

    def 'skips a record without the versions fields'() {
        given:
        def report = new SoftwareVersionReport()
        def record = new TestRecord()
        record.putAll(id: 'sample1', bam: 'sample1.bam')

        when:
        report.addInput([record, ['FASTQC', 'fastqc', '0.12.1']])

        then:
        report.renderYaml() == "FASTQC:\n  fastqc: 0.12.1"
    }

    def 'multiple addInput calls merge versions'() {
        given:
        def report = new SoftwareVersionReport()

        when:
        report.addInput('fastqc: 0.12.1')
        report.addInput([['PROCESS_A', 'samtools', '1.17']])

        then:
        def yaml = report.renderYaml()
        yaml.contains('fastqc: 0.12.1')
        yaml.contains('samtools:')
    }

    def 'sorts processes and tools alphabetically'() {
        given:
        def report = new SoftwareVersionReport()

        when:
        report.addInput([
            ['ZEBRA', 'zebra', '1.0'],
            ['ALPHA', 'zulu', '3.0'],
            ['ALPHA', 'alpha', '2.0']
        ])

        then:
        def yaml = report.renderYaml()
        def alphaIdx = yaml.indexOf('ALPHA:')
        def zebraIdx = yaml.indexOf('ZEBRA:')
        alphaIdx < zebraIdx
        // Within ALPHA, tools sorted
        def alphaSection = yaml.substring(alphaIdx, zebraIdx)
        alphaSection.indexOf('alpha:') < alphaSection.indexOf('zulu:')
    }

    def 'cleans colon-prefixed tool names'() {
        given:
        def report = new SoftwareVersionReport()

        when:
        report.addInput('tool:fastqc: 0.12.1')

        then:
        def yaml = report.renderYaml()
        yaml.contains('fastqc: 0.12.1')
        !yaml.contains('tool:fastqc')
    }

    def 'extracts process name from full path'() {
        given:
        def report = new SoftwareVersionReport()

        when:
        report.addInput([['NFCORE_RNASEQ:TRIMGALORE:FASTQC', 'fastqc', '0.12.1']])

        then:
        def yaml = report.renderYaml()
        yaml.contains('FASTQC:')
        !yaml.contains('NFCORE_RNASEQ:TRIMGALORE:FASTQC')
    }

    def 'handles null and empty input gracefully'() {
        given:
        def report = new SoftwareVersionReport()

        when:
        report.addInput(null)
        report.addInput([])
        report.addInput('')

        then:
        report.renderYaml() == ''
    }

    def 'addWorkflowVersion appends workflow section'() {
        given:
        def report = new SoftwareVersionReport()

        when:
        report.addInput('fastqc: 0.12.1')
        report.addWorkflowVersion('nf-core/rnaseq', 'v3.14.0', '25.04.0')

        then:
        def yaml = report.renderYaml()
        yaml.contains('fastqc: 0.12.1')
        yaml.contains('Workflow:')
        yaml.contains('nf-core/rnaseq: v3.14.0')
        yaml.contains('Nextflow: 25.04.0')
    }

    def 'nested YAML with process blocks is handled'() {
        given:
        def report = new SoftwareVersionReport()
        def nestedYaml = '''\
            NFCORE_FASTQC:
                fastqc: 0.12.1
            NFCORE_SAMTOOLS:
                samtools: 1.17
            '''.stripIndent()

        when:
        report.addInput(nestedYaml)

        then:
        def yaml = report.renderYaml()
        yaml.contains('NFCORE_FASTQC:')
        yaml.contains('NFCORE_SAMTOOLS:')
    }

    private static Record versionsRecord(String process, String tool, String version) {
        def record = new TestRecord()
        record.putAll(process: process, tool: tool, version: version)
        return record
    }

    /** Stands in for nextflow.util.RecordMap, which Nextflow 26.04+ defines as exactly this shape. */
    static class TestRecord extends LinkedHashMap<String, Object> implements Record {}
}
