package nfcore.plugin.nfcore

import nextflow.Session
import nextflow.config.Manifest
import spock.lang.Issue
import spock.lang.Specification

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
    def 'generateComprehensiveVersionReport should create complete report'() {
        given:
        def topicVersions = [
            ['NFCORE_FASTQC', 'fastqc', '0.12.1'],
            ['NFCORE_MULTIQC', 'multiqc', '1.15']
        ]
        def legacyVersions = ['samtools: 1.17']
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.generateComprehensiveVersionReport(
            topicVersions, legacyVersions, [], null, session
        )

        then:
        result.containsKey('versions_yaml')
        result.containsKey('tool_citations')
        result.containsKey('tool_bibliography')
        result.containsKey('methods_description')
        result.containsKey('citations_map')
        
        result.versions_yaml.contains('fastqc: 0.12.1')
        result.versions_yaml.contains("multiqc: '1.15'") || result.versions_yaml.contains('multiqc: 1.15')
        result.versions_yaml.contains('samtools: 1.17')
        result.versions_yaml.contains('testpipeline: v1.0.0')
    }

    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'generateComprehensiveVersionReport should handle citations when meta files provided'() {
        given:
        def topicVersions = [
            ['NFCORE_FASTQC', 'fastqc', '0.12.1']
        ]
        def metaFilePath = 'src/test/resources/example_meta.yml'
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.generateComprehensiveVersionReport(
            topicVersions, [], [metaFilePath], null, session
        )

        then:
        result.tool_citations.contains('fastqc')
        result.tool_bibliography.contains('<li>')
        result.citations_map.size() > 0
    }

    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'generateComprehensiveVersionReport should handle methods description template'() {
        given:
        def topicVersions = [
            ['NFCORE_FASTQC', 'fastqc', '0.12.1']
        ]
        def metaFilePath = 'src/test/resources/example_meta.yml'
        def mqcMethodsFile = new File('src/test/resources/multiqc_methods_description.yml')
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
            toMap() >> [name: 'testpipeline', version: '1.0.0']
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.generateComprehensiveVersionReport(
            topicVersions, [], [metaFilePath], mqcMethodsFile, session
        )

        then:
        result.containsKey('methods_description')
        result.tool_citations.contains('fastqc')
        result.tool_bibliography.contains('<li>')
    }

    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'generateComprehensiveVersionReport should handle missing meta files gracefully'() {
        given:
        def topicVersions = [
            ['NFCORE_FASTQC', 'fastqc', '0.12.1']
        ]
        def nonExistentMetaPath = 'path/to/nonexistent/meta.yml'
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreVersionUtils.generateComprehensiveVersionReport(
            topicVersions, [], [nonExistentMetaPath], null, session
        )

        then:
        result.versions_yaml.contains('fastqc: 0.12.1')
        result.tool_citations == 'No tools used in the workflow.'
        result.tool_bibliography == 'No bibliography entries found.'
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
} 