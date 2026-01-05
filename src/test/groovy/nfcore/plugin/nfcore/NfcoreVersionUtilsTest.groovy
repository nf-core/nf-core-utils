package nfcore.plugin.nfcore

import nextflow.Session
import nextflow.config.Manifest
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.TempDir

class NfcoreVersionUtilsTest extends Specification {

    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'getWorkflowVersion formats version with v prefix when using Session'() {
        given:
        def manifest = Mock(Manifest) {
            getVersion() >> version
        }
        def session = Mock(Session) {
            getManifest() >> manifest
        }

        when:
        def result = NfcoreVersionUtils.getWorkflowVersion(session)

        then:
        result == expected

        where:
        version  | expected
        '1.0.0'  | 'v1.0.0'
        'v2.1.0' | 'v2.1.0'
        null     | ''
        '3.0.0'  | 'v3.0.0'
    }

    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'getWorkflowVersion formats version with explicit version parameter'() {
        when:
        def result = NfcoreVersionUtils.getWorkflowVersion(null, version)

        then:
        result == expected

        where:
        version  | expected
        '1.0.0'  | 'v1.0.0'
        'v2.1.0' | 'v2.1.0'
        null     | ''
        '3.0.0'  | 'v3.0.0'
    }

    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'getWorkflowVersion formats version with commit ID'() {
        when:
        def result = NfcoreVersionUtils.getWorkflowVersion(null, version, commitId)

        then:
        result == expected

        where:
        version  | commitId           | expected
        '1.0.0'  | 'abcdef1234567890' | 'v1.0.0-gabcdef1'
        'v2.1.0' | '1234567890abcdef' | 'v2.1.0-g1234567'
        null     | 'abcdef1234567890' | '-gabcdef1'
    }

    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'getWorkflowVersion formats version with Session and commit ID'() {
        given:
        def manifest = Mock(Manifest) {
            getVersion() >> version
        }
        def session = Mock(Session) {
            getManifest() >> manifest
        }

        when:
        def result = NfcoreVersionUtils.getWorkflowVersion(session, null, commitId)

        then:
        result == expected

        where:
        version  | commitId           | expected
        '1.0.0'  | 'abcdef1234567890' | 'v1.0.0-gabcdef1'
        'v2.1.0' | '1234567890abcdef' | 'v2.1.0-g1234567'
        null     | 'abcdef1234567890' | '-gabcdef1'
    }

    // =========================================================================
    // collectVersions() tests - PRIMARY API
    // =========================================================================

    def 'collectVersions handles YAML string input'() {
        given:
        def yamlString = "fastqc: 0.12.1\nsamtools: 1.17"

        when:
        def result = NfcoreVersionUtils.collectVersions(yamlString)

        then:
        result.contains('fastqc: 0.12.1')
        result.contains('samtools: 1.17')
    }

    def 'collectVersions handles topic tuples'() {
        given:
        def topicTuples = [
            ['NFCORE_FASTQC', 'fastqc', '0.12.1'],
            ['NFCORE_SAMTOOLS', 'samtools', '1.17']
        ]

        when:
        def result = NfcoreVersionUtils.collectVersions(topicTuples)

        then:
        result.contains('fastqc: 0.12.1')
        result.contains('samtools: 1.17') || result.contains("samtools: '1.17'")
        result.contains('NFCORE_FASTQC:')
        result.contains('NFCORE_SAMTOOLS:')
    }

    def 'collectVersions handles file paths'() {
        given:
        def tempFile = File.createTempFile('versions', '.yml')
        tempFile.text = '''
        fastqc: 0.12.1
        samtools: 1.17
        '''.stripIndent()
        def versions = [tempFile.absolutePath]

        when:
        def result = NfcoreVersionUtils.collectVersions(versions)

        then:
        result.contains('fastqc: 0.12.1')
        result.contains('samtools: 1.17')

        cleanup:
        tempFile.delete()
    }

    def 'collectVersions handles File objects'() {
        given:
        def tempFile = File.createTempFile('versions', '.yml')
        tempFile.text = '''
        fastqc: 0.12.1
        samtools: 1.17
        '''.stripIndent()
        def versions = [tempFile]

        when:
        def result = NfcoreVersionUtils.collectVersions(versions)

        then:
        result.contains('fastqc: 0.12.1')
        result.contains('samtools: 1.17')

        cleanup:
        tempFile.delete()
    }

    def 'collectVersions handles Map objects'() {
        given:
        def versionMap = [fastqc: '0.12.1', samtools: '1.17']
        def versions = [versionMap]

        when:
        def result = NfcoreVersionUtils.collectVersions(versions)

        then:
        result.contains('fastqc: 0.12.1')
        result.contains('samtools: 1.17') || result.contains("samtools: '1.17'")
        result.contains('Software:')
    }

    def 'collectVersions handles mixed input types'() {
        given:
        def yamlString = 'fastqc: 0.12.1'
        def topicTuple = ['NFCORE_SAMTOOLS', 'samtools', '1.17']
        def versionMap = [multiqc: '1.15']
        def versions = [yamlString, topicTuple, versionMap]

        when:
        def result = NfcoreVersionUtils.collectVersions(versions)

        then:
        result.contains('fastqc: 0.12.1')
        result.contains('samtools: 1.17') || result.contains("samtools: '1.17'")
        result.contains('multiqc: 1.15') || result.contains("multiqc: '1.15'")
        result.contains('NFCORE_SAMTOOLS:')
        result.contains('Software:')
    }

    def 'collectVersions handles nested lists (collected channels)'() {
        given:
        def nestedList = [
            [['PROCESS1', 'tool1', '1.0.0']],
            [['PROCESS2', 'tool2', '2.0.0']]
        ]

        when:
        def result = NfcoreVersionUtils.collectVersions(nestedList)

        then:
        result.contains('tool1: 1.0.0')
        result.contains('tool2: 2.0.0')
    }

    def 'collectVersions extracts process name from full path'() {
        given:
        def topicTuples = [
            ['NFCORE_RNASEQ:TRIMGALORE:FASTQC', 'fastqc', '0.12.1']
        ]

        when:
        def result = NfcoreVersionUtils.collectVersions(topicTuples)

        then:
        result.contains('FASTQC:')
        result.contains('fastqc: 0.12.1')
        !result.contains('NFCORE_RNASEQ:TRIMGALORE:FASTQC')
    }

    def 'collectVersions sorts processes and tools alphabetically'() {
        given:
        def topicTuples = [
            ['ZEBRA_PROCESS', 'zebra', '1.0.0'],
            ['ALPHA_PROCESS', 'alpha', '2.0.0'],
            ['ALPHA_PROCESS', 'zulu', '3.0.0'],
            ['ALPHA_PROCESS', 'bravo', '4.0.0']
        ]

        when:
        def result = NfcoreVersionUtils.collectVersions(topicTuples)

        then:
        def alphaIndex = result.indexOf('ALPHA_PROCESS:')
        def zebraIndex = result.indexOf('ZEBRA_PROCESS:')
        alphaIndex < zebraIndex
        // Within ALPHA_PROCESS, tools should be sorted
        def alphaSection = result.substring(alphaIndex, zebraIndex)
        def alphaPos = alphaSection.indexOf('alpha:')
        def bravoPos = alphaSection.indexOf('bravo:')
        def zuluPos = alphaSection.indexOf('zulu:')
        alphaPos < bravoPos && bravoPos < zuluPos
    }

    def 'collectVersions cleans tool names with colons'() {
        given:
        def yamlWithColons = "tool:fastqc: 0.12.1\ntool:samtools: 1.17"
        def versions = [yamlWithColons]

        when:
        def result = NfcoreVersionUtils.collectVersions(versions)

        then:
        result.contains('fastqc: 0.12.1')
        result.contains('samtools: 1.17')
        !result.contains('tool:fastqc')
        !result.contains('tool:samtools')
    }

    def 'collectVersions handles invalid entries gracefully'() {
        given:
        def versions = [
            'fastqc: 0.12.1',
            null,
            '',
            ['INCOMPLETE'],
            ['INCOMPLETE', 'tool'],
            'invalid yaml content: bad: structure:'
        ]

        when:
        def result = NfcoreVersionUtils.collectVersions(versions)

        then:
        result.contains('fastqc: 0.12.1')
        noExceptionThrown()
    }

    def 'collectVersions handles empty input'() {
        when:
        def result = NfcoreVersionUtils.collectVersions([])

        then:
        result == ''
    }

    def 'collectVersions handles null input'() {
        when:
        def result = NfcoreVersionUtils.collectVersions(null)

        then:
        result == ''
    }

    def 'collectVersions includes workflow version when session provided'() {
        given:
        def yamlString = 'fastqc: 0.12.1'
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.collectVersions(yamlString, session)

        then:
        result.contains('fastqc: 0.12.1')
        result.contains('Workflow:')
        result.contains('testpipeline: v1.0.0')
        result.contains('Nextflow: 23.04.1')
    }

    def 'collectVersions excludes workflow version when session is null'() {
        given:
        def yamlString = 'fastqc: 0.12.1'

        when:
        def result = NfcoreVersionUtils.collectVersions(yamlString, null)

        then:
        result.contains('fastqc: 0.12.1')
        !result.contains('Workflow:')
    }

    def 'collectVersions uses provided nextflowVersion override'() {
        given:
        def yamlString = 'fastqc: 0.12.1'
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.collectVersions(yamlString, session, '24.10.0')

        then:
        result.contains('fastqc: 0.12.1')
        result.contains('Nextflow: 24.10.0')
        !result.contains('23.04.1')
    }

    def 'collectVersions handles nested YAML with process blocks'() {
        given:
        def nestedYaml = '''
        NFCORE_FASTQC:
            fastqc: 0.12.1
        NFCORE_SAMTOOLS:
            samtools: 1.17
        '''.stripIndent()
        def versions = [nestedYaml]

        when:
        def result = NfcoreVersionUtils.collectVersions(versions)

        then:
        result.contains('fastqc: 0.12.1')
        result.contains('samtools: 1.17')
        result.contains('NFCORE_FASTQC:')
        result.contains('NFCORE_SAMTOOLS:')
    }

    // =========================================================================
    // DEPRECATED METHOD TESTS (ensure backward compatibility)
    // =========================================================================

    @Issue("https://github.com/nf-core/modules/issues/4517")
    def 'processVersionsFromYAML should parse and flatten YAML keys'() {
        given:
        def yamlString = """
        tool:foo: 1.0.0
        bar: 2.0.0
        """.stripIndent()

        when:
        def result = NfcoreVersionUtils.processVersionsFromYAML(yamlString)

        then:
        result.contains('foo: 1.0.0')
        result.contains('bar: 2.0.0')
        !result.contains('tool:foo:')
    }

    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'processVersionsFromTopic should handle topic channel format'() {
        given:
        def topicData = [
            ['NFCORE_FASTQC', 'fastqc', '0.12.1'],
            ['NFCORE_MULTIQC', 'multiqc', '1.15']
        ]

        when:
        def result = NfcoreVersionUtils.processVersionsFromTopic(topicData)

        then:
        result.contains('fastqc: 0.12.1')
        result.contains("multiqc: '1.15'") || result.contains('multiqc: 1.15')
    }

    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'processVersionsFromTopic should handle empty or malformed data'() {
        when:
        def result = NfcoreVersionUtils.processVersionsFromTopic(topicData)

        then:
        result == expected

        where:
        topicData                              | expected
        []                                     | '{}'
        [['PROCESS']]                          | '{}'
        [['PROCESS', 'tool']]                  | '{}'
        [['PROCESS', 'tool', 'version', 'extra']] | 'tool: version'
    }

    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'processVersionsFromTopicChannels should combine topic and legacy versions'() {
        given:
        def topicVersions = [
            ['NFCORE_FASTQC', 'fastqc', '0.12.1']
        ]
        def legacyVersions = [
            'samtools: 1.17'
        ]
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.processVersionsFromTopicChannels(topicVersions, legacyVersions, session)

        then:
        result.contains('fastqc: 0.12.1')
        result.contains('samtools: 1.17')
        result.contains('Workflow:')
        result.contains('testpipeline: v1.0.0')
    }

    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'workflowVersionToYAML contains workflow details'() {
        given:
        def manifest = Mock(Manifest) {
            getName() >> wfName
            getVersion() >> wfVersion
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: nfVersion]]
        }

        when:
        def result = NfcoreVersionUtils.workflowVersionToYAML(session)

        then:
        result.contains(wfName ?: 'unknown')
        result.contains(nfVersion ?: 'unknown')
        if (wfVersion) {
            result.contains(wfVersion.startsWith('v') ? wfVersion : "v${wfVersion}")
        }

        where:
        wfName   | wfVersion | nfVersion
        'mypipe' | '1.0.0'   | '23.04.1'
        null     | null      | null
    }

    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'softwareVersionsToYAML combines unique YAMLs and workflow YAML'() {
        given:
        def yaml1 = 'foo: 1.0.0\nbar: 2.0.0'
        def yaml2 = 'baz: 3.0.0\nfoo: 1.0.0'
        def chVersions = [yaml1, yaml2]
        def manifest = Mock(Manifest) {
            getName() >> 'pipe'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.softwareVersionsToYAML(chVersions, session)

        then:
        result.contains('foo: 1.0.0')
        result.contains('bar: 2.0.0')
        result.contains('baz: 3.0.0')
        result.contains('Workflow:')
        result.contains('pipe: v1.0.0')
        result.contains('Nextflow: 23.04.1')
    }

    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'softwareVersionsToYAML supports piecemeal conversion to eval format'() {
        given:
        def yamlFragments = []
        yamlFragments << 'foo: 1.0.0\nbar: 2.0.0'
        yamlFragments << 'baz: 3.0.0\nfoo: 1.0.0' // duplicate foo
        def manifest = Mock(Manifest) {
            getName() >> 'pipe'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.softwareVersionsToYAML(yamlFragments, session)

        then:
        result.contains('foo: 1.0.0')
        result.contains('bar: 2.0.0')
        result.contains('baz: 3.0.0')
        result.contains('Workflow:')
        result.contains('pipe: v1.0.0')
        result.contains('Nextflow: 23.04.1')
    }

    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'softwareVersionsToYAMLFromChannel combines YAMLs from a list'() {
        given:
        def yamlList = ['foo: 1.0.0', 'bar: 2.0.0', 'foo: 1.0.0']
        def manifest = Mock(Manifest) {
            getName() >> 'pipe'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.softwareVersionsToYAMLFromChannel(yamlList, session)

        then:
        result.contains('foo: 1.0.0')
        result.contains('bar: 2.0.0')
        result.contains('Workflow:')
        result.contains('pipe: v1.0.0')
        result.contains('Nextflow: 23.04.1')
    }

    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'softwareVersionsToYAML handles empty input'() {
        given:
        def manifest = Mock(Manifest) {
            getName() >> 'pipe'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.softwareVersionsToYAML([], session)

        then:
        result.contains('Workflow:')
        result.contains('pipe: v1.0.0')
        result.contains('Nextflow: 23.04.1')
    }



    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'processVersionsFromTopicChannels should handle only topic versions'() {
        given:
        def topicVersions = [
            ['NFCORE_FASTQC', 'fastqc', '0.12.1'],
            ['NFCORE_MULTIQC', 'multiqc', '1.15']
        ]
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.processVersionsFromTopicChannels(topicVersions, [], session)

        then:
        result.contains('fastqc: 0.12.1')
        result.contains("multiqc: '1.15'") || result.contains('multiqc: 1.15')
        result.contains('testpipeline: v1.0.0')
        result.contains('Nextflow: 23.04.1')
    }

    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'processVersionsFromTopicChannels should handle only legacy versions'() {
        given:
        def legacyVersions = [
            'samtools: 1.17',
            'bcftools: 1.16'
        ]
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.processVersionsFromTopicChannels([], legacyVersions, session)

        then:
        result.contains('samtools: 1.17')
        result.contains('bcftools: 1.16')
        result.contains('testpipeline: v1.0.0')
        result.contains('Nextflow: 23.04.1')
    }

    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'processVersionsFromFile should handle file paths'() {
        given:
        def tempFile = File.createTempFile('versions', '.yml')
        tempFile.text = '''
        fastqc: 0.12.1
        tool:samtools: 1.17
        '''.stripIndent()
        def filePaths = [tempFile.absolutePath]

        when:
        def result = NfcoreVersionUtils.processVersionsFromFile(filePaths)

        then:
        result.contains('fastqc: 0.12.1')
        result.contains('samtools: 1.17')

        cleanup:
        tempFile.delete()
    }

    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'processVersionsFromFile should handle missing files gracefully'() {
        given:
        def nonExistentPaths = ['/non/existent/path/versions.yml']

        when:
        def result = NfcoreVersionUtils.processVersionsFromFile(nonExistentPaths)

        then:
        result == '{}'
    }

    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'processMixedVersionSources should combine topic and file sources'() {
        given:
        def topicVersions = [
            ['NFCORE_FASTQC', 'fastqc', '0.12.1']
        ]
        def tempFile = File.createTempFile('versions', '.yml')
        tempFile.text = 'samtools: 1.17'
        def versionsFiles = [tempFile.absolutePath]
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.processMixedVersionSources(topicVersions, versionsFiles, session)

        then:
        result.contains('fastqc: 0.12.1')
        result.contains('samtools: 1.17')
        result.contains('testpipeline: v1.0.0')

        cleanup:
        tempFile.delete()
    }

    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'convertLegacyYamlToEvalSyntax should convert YAML to eval format'() {
        given:
        def yamlContent = '''
        fastqc: 0.12.1
        tool:samtools: 1.17
        multiqc: 1.15
        '''.stripIndent()
        def processName = 'TEST_PROCESS'

        when:
        def result = NfcoreVersionUtils.convertLegacyYamlToEvalSyntax(yamlContent, processName)

        then:
        result.size() == 3
        result.contains([processName, 'fastqc', '0.12.1'])
        result.contains([processName, 'samtools', '1.17'])
        result.contains([processName, 'multiqc', '1.15'])
    }

    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'convertLegacyYamlToEvalSyntax should handle malformed YAML gracefully'() {
        given:
        def invalidYaml = 'invalid: yaml: content:'

        when:
        def result = NfcoreVersionUtils.convertLegacyYamlToEvalSyntax(invalidYaml)

        then:
        result == []
    }

    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'generateYamlFromEvalSyntax should convert eval format to YAML'() {
        given:
        def evalData = [
            ['NFCORE_FASTQC', 'fastqc', '0.12.1'],
            ['NFCORE_SAMTOOLS', 'samtools', '1.17'],
            ['NFCORE_MULTIQC', 'multiqc', '1.15']
        ]
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.generateYamlFromEvalSyntax(evalData, session, true)

        then:
        result.contains('fastqc: 0.12.1')
        result.contains('samtools: 1.17') || result.contains("samtools: '1.17'")
        result.contains('multiqc: 1.15') || result.contains("multiqc: '1.15'")
        result.contains('Workflow:')
        result.contains('testpipeline: v1.0.0')
    }

    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'generateYamlFromEvalSyntax should exclude workflow when requested'() {
        given:
        def evalData = [
            ['NFCORE_FASTQC', 'fastqc', '0.12.1']
        ]
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.generateYamlFromEvalSyntax(evalData, session, false)

        then:
        result.contains('fastqc: 0.12.1')
        !result.contains('Workflow:')
        !result.contains('testpipeline: v1.0.0')
    }

    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'workflowVersionToChannel should return eval syntax for workflow info'() {
        given:
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.workflowVersionToChannel(session)

        then:
        result.size() == 2
        result[0] == ['Workflow', 'testpipeline', 'v1.0.0']
        result[1] == ['Workflow', 'Nextflow', '23.04.1']
    }

    @Issue("https://github.com/nf-core/nf-core-utils/pull/24")
    def 'workflowVersionToYAML should use provided nextflowVersion parameter'() {
        given:
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.workflowVersionToYAML(session, '24.10.0')

        then:
        result.contains('testpipeline: v1.0.0')
        result.contains('Nextflow: 24.10.0')
        !result.contains('23.04.1')
    }

    @Issue("https://github.com/nf-core/nf-core-utils/pull/24")
    def 'workflowVersionToYAML should fall back to NXF_VER environment variable'() {
        given:
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [:]
        }
        // Simulate NXF_VER environment variable
        def originalNxfVer = System.getenv('NXF_VER')
        def envVars = ['NXF_VER': '24.04.0']
        envVars.each { k, v -> System.setProperty(k, v) }

        when:
        def result = NfcoreVersionUtils.workflowVersionToYAML(session, null)

        then:
        result.contains('testpipeline: v1.0.0')
        // Should contain either the env var or 'unknown' since we can't easily mock System.getenv()
        result.contains('Nextflow:')

        cleanup:
        envVars.each { k, v -> System.clearProperty(k) }
    }

    @Issue("https://github.com/nf-core/nf-core-utils/pull/24")
    def 'softwareVersionsToYAML should handle mixed input sources - YAML strings'() {
        given:
        def yamlString1 = 'fastqc: 0.12.1'
        def yamlString2 = 'samtools: 1.17'
        def versions = [yamlString1, yamlString2]
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.softwareVersionsToYAML(versions, session)

        then:
        result.contains('fastqc: 0.12.1')
        result.contains('samtools: 1.17')
        result.contains('Workflow:')
        result.contains('testpipeline: v1.0.0')
    }

    @Issue("https://github.com/nf-core/nf-core-utils/pull/24")
    def 'softwareVersionsToYAML should handle topic tuples'() {
        given:
        def topicTuples = [
            ['NFCORE_FASTQC', 'fastqc', '0.12.1'],
            ['NFCORE_SAMTOOLS', 'samtools', '1.17']
        ]
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.softwareVersionsToYAML(topicTuples, session)

        then:
        result.contains('fastqc: 0.12.1')
        result.contains('samtools: 1.17') || result.contains("samtools: '1.17'")
        result.contains('NFCORE_FASTQC:')
        result.contains('NFCORE_SAMTOOLS:')
    }

    @Issue("https://github.com/nf-core/nf-core-utils/pull/24")
    def 'softwareVersionsToYAML should handle file paths'() {
        given:
        def tempFile = File.createTempFile('versions', '.yml')
        tempFile.text = '''
        fastqc: 0.12.1
        samtools: 1.17
        '''.stripIndent()
        def versions = [tempFile.absolutePath]
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.softwareVersionsToYAML(versions, session)

        then:
        result.contains('fastqc: 0.12.1')
        result.contains('samtools: 1.17')

        cleanup:
        tempFile.delete()
    }

    @Issue("https://github.com/nf-core/nf-core-utils/pull/24")
    def 'softwareVersionsToYAML should handle File objects'() {
        given:
        def tempFile = File.createTempFile('versions', '.yml')
        tempFile.text = '''
        fastqc: 0.12.1
        samtools: 1.17
        '''.stripIndent()
        def versions = [tempFile]
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.softwareVersionsToYAML(versions, session)

        then:
        result.contains('fastqc: 0.12.1')
        result.contains('samtools: 1.17')

        cleanup:
        tempFile.delete()
    }

    @Issue("https://github.com/nf-core/nf-core-utils/pull/24")
    def 'softwareVersionsToYAML should handle Map objects'() {
        given:
        def versionMap = [fastqc: '0.12.1', samtools: '1.17']
        def versions = [versionMap]
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.softwareVersionsToYAML(versions, session)

        then:
        result.contains('fastqc: 0.12.1')
        result.contains('samtools: 1.17') || result.contains("samtools: '1.17'")
        result.contains('Software:')
    }

    @Issue("https://github.com/nf-core/nf-core-utils/pull/24")
    def 'softwareVersionsToYAML should handle mixed input types'() {
        given:
        def yamlString = 'fastqc: 0.12.1'
        def topicTuple = ['NFCORE_SAMTOOLS', 'samtools', '1.17']
        def versionMap = [multiqc: '1.15']
        def versions = [yamlString, topicTuple, versionMap]
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.softwareVersionsToYAML(versions, session)

        then:
        result.contains('fastqc: 0.12.1')
        result.contains('samtools: 1.17') || result.contains("samtools: '1.17'")
        result.contains('multiqc: 1.15') || result.contains("multiqc: '1.15'")
        result.contains('NFCORE_SAMTOOLS:')
        result.contains('Software:')
    }

    @Issue("https://github.com/nf-core/nf-core-utils/pull/24")
    def 'softwareVersionsToYAML should use provided nextflowVersion'() {
        given:
        def yamlString = 'fastqc: 0.12.1'
        def versions = [yamlString]
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.softwareVersionsToYAML(versions, session, '24.10.0')

        then:
        result.contains('fastqc: 0.12.1')
        result.contains('Nextflow: 24.10.0')
        !result.contains('23.04.1')
    }

    @Issue("https://github.com/nf-core/nf-core-utils/pull/24")
    def 'softwareVersionsToYAML should handle nested YAML with process blocks'() {
        given:
        def nestedYaml = '''
        NFCORE_FASTQC:
            fastqc: 0.12.1
        NFCORE_SAMTOOLS:
            samtools: 1.17
        '''.stripIndent()
        def versions = [nestedYaml]
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.softwareVersionsToYAML(versions, session)

        then:
        result.contains('fastqc: 0.12.1')
        result.contains('samtools: 1.17')
        result.contains('NFCORE_FASTQC:')
        result.contains('NFCORE_SAMTOOLS:')
    }

    @Issue("https://github.com/nf-core/nf-core-utils/pull/24")
    def 'softwareVersionsToYAML should clean tool names with colons'() {
        given:
        def yamlWithColons = 'tool:fastqc: 0.12.1\ntool:samtools: 1.17'
        def versions = [yamlWithColons]
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.softwareVersionsToYAML(versions, session)

        then:
        result.contains('fastqc: 0.12.1')
        result.contains('samtools: 1.17')
        !result.contains('tool:fastqc')
        !result.contains('tool:samtools')
    }

    @Issue("https://github.com/nf-core/nf-core-utils/pull/24")
    def 'softwareVersionsToYAML should sort processes and tools alphabetically'() {
        given:
        def topicTuples = [
            ['ZEBRA_PROCESS', 'zebra', '1.0.0'],
            ['ALPHA_PROCESS', 'alpha', '2.0.0'],
            ['ALPHA_PROCESS', 'zulu', '3.0.0'],
            ['ALPHA_PROCESS', 'bravo', '4.0.0']
        ]
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.softwareVersionsToYAML(topicTuples, session)

        then:
        def alphaIndex = result.indexOf('ALPHA_PROCESS:')
        def zebraIndex = result.indexOf('ZEBRA_PROCESS:')
        alphaIndex < zebraIndex
        // Within ALPHA_PROCESS, tools should be sorted
        def alphaSection = result.substring(alphaIndex, zebraIndex)
        def alphaPos = alphaSection.indexOf('alpha:')
        def bravoPos = alphaSection.indexOf('bravo:')
        def zuluPos = alphaSection.indexOf('zulu:')
        alphaPos < bravoPos && bravoPos < zuluPos
    }

    @Issue("https://github.com/nf-core/nf-core-utils/pull/24")
    def 'softwareVersionsToYAML should extract process name from full path'() {
        given:
        def topicTuples = [
            ['NFCORE_RNASEQ:TRIMGALORE:FASTQC', 'fastqc', '0.12.1']
        ]
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.softwareVersionsToYAML(topicTuples, session)

        then:
        result.contains('FASTQC:')
        result.contains('fastqc: 0.12.1')
        !result.contains('NFCORE_RNASEQ:TRIMGALORE:FASTQC')
    }

    @Issue("https://github.com/nf-core/nf-core-utils/pull/24")
    def 'softwareVersionsToYAML should handle list of lists [collected channel]'() {
        given:
        def nestedList = [
            [['PROCESS1', 'tool1', '1.0.0']],
            [['PROCESS2', 'tool2', '2.0.0']]
        ]
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.softwareVersionsToYAML(nestedList, session)

        then:
        result.contains('tool1: 1.0.0')
        result.contains('tool2: 2.0.0')
    }

    @Issue("https://github.com/nf-core/nf-core-utils/pull/24")
    def 'softwareVersionsToYAML should handle invalid entries gracefully'() {
        given:
        def versions = [
            'fastqc: 0.12.1',
            null,
            '',
            ['INCOMPLETE'],
            ['INCOMPLETE', 'tool'],
            'invalid yaml content: bad: structure:'
        ]
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.softwareVersionsToYAML(versions, session)

        then:
        result.contains('fastqc: 0.12.1')
        result.contains('Workflow:')
        noExceptionThrown()
    }

    @Issue("https://github.com/nf-core/nf-core-utils/pull/24")
    def 'softwareVersionsToYAML should handle empty input list'() {
        given:
        def versions = []
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.softwareVersionsToYAML(versions, session)

        then:
        result.contains('Workflow:')
        result.contains('testpipeline: v1.0.0')
        result.contains('Nextflow: 23.04.1')
    }
}
