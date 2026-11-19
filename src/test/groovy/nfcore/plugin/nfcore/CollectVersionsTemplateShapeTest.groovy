package nfcore.plugin.nfcore

import nextflow.Session
import nextflow.config.Manifest
import nfcore.plugin.NfUtilsExtension
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Title

import java.nio.file.Files
import java.nio.file.Path

@Title("collectVersions renders the inputs the migrated nf-core template passes")
class CollectVersionsTemplateShapeTest extends Specification {

    @TempDir
    Path tempDir

    private NfUtilsExtension extension() {
        def session = Mock(Session) {
            getManifest() >> Mock(Manifest) { getName() >> 'nf-core/test'; getVersion() >> '1.0.0' }
            getConfig() >> [:]
        }
        def ext = new NfUtilsExtension()
        ext.init(session)
        return ext
    }

    def 'topic tuples, a legacy versions.yml and the workflow block render as one report'() {
        given: 'the shape of channel.topic("versions").toList(), as the template passes it'
        def legacy = tempDir.resolve('versions.yml')
        Files.writeString(legacy, "LEGACY_TOOL:\n  legacy: '2.0'\n")
        def versions = [
            ['NFCORE_X:X:FASTQC', 'fastqc', '0.12.1'],
            ['NFCORE_X:X:SAMTOOLS_SORT', 'samtools', '1.17'],
            ['NFCORE_X:X:SEQTK_SEQ', 'seqtk', '2.0'],
            legacy,
        ]

        when:
        def yaml = extension().collectVersions(versions, '25.10.4')

        then: 'process short names, sorted; numeric-looking versions stay quoted; Workflow last'
        yaml == """\
            FASTQC:
              fastqc: 0.12.1
            LEGACY_TOOL:
              legacy: '2.0'
            SAMTOOLS_SORT:
              samtools: '1.17'
            SEQTK_SEQ:
              seqtk: '2.0'
            Workflow:
                nf-core/test: v1.0.0
                Nextflow: 25.10.4""".stripIndent()
    }

    def 'the same tool from two processes is listed under each process'() {
        when:
        def yaml = extension().collectVersions([
            ['NFCORE_X:X:SAMTOOLS_SORT', 'samtools', '1.17'],
            ['NFCORE_X:X:SAMTOOLS_INDEX', 'samtools', '1.17'],
        ], '25.10.4')

        then:
        yaml.startsWith("SAMTOOLS_INDEX:\n  samtools: '1.17'\nSAMTOOLS_SORT:\n  samtools: '1.17'\nWorkflow:")
    }
}
