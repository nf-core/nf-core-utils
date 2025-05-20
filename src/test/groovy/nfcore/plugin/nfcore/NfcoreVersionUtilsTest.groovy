package nfcore.plugin.nfcore

import nextflow.Session
import nextflow.config.Manifest
import spock.lang.Specification

class NfcoreVersionUtilsTest extends Specification {

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
} 