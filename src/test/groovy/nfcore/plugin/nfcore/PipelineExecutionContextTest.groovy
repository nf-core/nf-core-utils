package nfcore.plugin.nfcore

import nextflow.Session
import nextflow.config.Manifest
import spock.lang.Narrative
import spock.lang.Issue
import spock.lang.See
import spock.lang.Specification
import spock.lang.Title

@Title("PipelineExecutionContext concentrates Nextflow runtime facts behind a testable seam")
@Narrative("""
Multiple modules reach into nextflow.Session for workflow name, version, profile,
config files, and manifest data. PipelineExecutionContext exposes these as plain
domain properties so callers and tests never depend on Session internals directly.
""")
@Issue("https://github.com/nf-core/nf-core-utils/pull/41")
@See("https://github.com/nf-core/nf-core-utils/blob/main/docs/adr/0001-prioritize-deep-modules-around-execution-context-and-reporting.md#1-pipeline-execution-context-module")
class PipelineExecutionContextTest extends Specification {

    // --- fromSession adapter ---

    def 'fromSession extracts workflow name from manifest'() {
        given:
        def manifest = Mock(Manifest) { getName() >> 'nf-core/rnaseq' }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [:]
        }

        when:
        def ctx = PipelineExecutionContext.fromSession(session)

        then:
        ctx.workflowName == 'nf-core/rnaseq'
    }

    def 'fromSession extracts workflow version from manifest'() {
        given:
        def manifest = Mock(Manifest) { getVersion() >> '3.14.0' }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [:]
        }

        when:
        def ctx = PipelineExecutionContext.fromSession(session)

        then:
        ctx.workflowVersion == '3.14.0'
    }

    def 'fromSession extracts nextflow version from config'() {
        given:
        def session = Mock(Session) {
            getManifest() >> Mock(Manifest)
            getConfig() >> [nextflow: [version: '25.04.0']]
        }

        when:
        def ctx = PipelineExecutionContext.fromSession(session)

        then:
        ctx.nextflowVersion == '25.04.0'
    }

    def 'fromSession extracts profile from session'() {
        given:
        def session = Mock(Session) {
            getManifest() >> Mock(Manifest)
            getConfig() >> [profile: 'docker,test']
            getProfile() >> 'docker,test'
        }

        when:
        def ctx = PipelineExecutionContext.fromSession(session)

        then:
        ctx.profile == 'docker,test'
    }

    def 'fromSession extracts configFiles from config map'() {
        given:
        def files = ['nextflow.config', 'custom.config']
        def session = Mock(Session) {
            getManifest() >> Mock(Manifest)
            getConfig() >> [configFiles: files]
        }

        when:
        def ctx = PipelineExecutionContext.fromSession(session)

        then:
        ctx.configFiles == files
    }

    def 'fromSession extracts projectName from workflowMetadata when available'() {
        given:
        // Spock enforces return types on Session.getWorkflowMetadata();
        // use the property shorthand which bypasses type checking
        def session = Mock(Session) {
            getManifest() >> Mock(Manifest)
            getConfig() >> [:]
            workflowMetadata >> [projectName: 'nf-core/rnaseq']
        }

        when:
        def ctx = PipelineExecutionContext.fromSession(session)

        then:
        ctx.projectName == 'nf-core/rnaseq'
    }

    def 'fromSession defaults gracefully when session fields are null'() {
        given:
        def session = Mock(Session) {
            getManifest() >> null
            getConfig() >> null
            getWorkflowMetadata() >> null
        }

        when:
        def ctx = PipelineExecutionContext.fromSession(session)

        then:
        ctx.workflowName == 'unknown'
        ctx.workflowVersion == null
        ctx.projectName == null
        ctx.profile == null
        ctx.configFiles == []
        ctx.config == [:]
        ctx.nextflowVersion == null
    }

    def 'fromSession handles null session'() {
        when:
        def ctx = PipelineExecutionContext.fromSession(null)

        then:
        ctx.workflowName == 'unknown'
        ctx.config == [:]
    }

    // --- manual construction for tests ---

    def 'constructor builds context from map properties'() {
        when:
        def ctx = new PipelineExecutionContext(
            workflowName: 'nf-core/sarek',
            workflowVersion: '3.4.0',
            nextflowVersion: '24.10.0',
            profile: 'test,docker',
            configFiles: ['main.config'],
            config: [params: [outdir: '/results']],
            projectName: 'nf-core/sarek'
        )

        then:
        ctx.workflowName == 'nf-core/sarek'
        ctx.workflowVersion == '3.4.0'
        ctx.nextflowVersion == '24.10.0'
        ctx.profile == 'test,docker'
        ctx.configFiles == ['main.config']
        ctx.config.params.outdir == '/results'
        ctx.projectName == 'nf-core/sarek'
    }

    def 'default constructor produces safe defaults'() {
        when:
        def ctx = new PipelineExecutionContext()

        then:
        ctx.workflowName == 'unknown'
        ctx.workflowVersion == null
        ctx.nextflowVersion == null
        ctx.profile == null
        ctx.configFiles == []
        ctx.config == [:]
        ctx.projectName == null
    }

    // --- manifestMap ---

    def 'fromSession populates manifestMap from manifest.toMap()'() {
        given:
        def manifest = Mock(Manifest) {
            toMap() >> [name: 'nf-core/rnaseq', version: '3.14.0', author: 'nf-core']
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [:]
        }

        when:
        def ctx = PipelineExecutionContext.fromSession(session)

        then:
        ctx.manifestMap.name == 'nf-core/rnaseq'
        ctx.manifestMap.version == '3.14.0'
        ctx.manifestMap.author == 'nf-core'
    }

    def 'fromSession tolerates manifest.toMap failures from partial validation manifests'() {
        given:
        def manifest = Mock(Manifest) {
            getName() >> 'nf-core/validation'
            toMap() >> { throw new NullPointerException('contributors') }
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [:]
        }

        when:
        def ctx = PipelineExecutionContext.fromSession(session)

        then:
        ctx.workflowName == 'nf-core/validation'
        ctx.manifestMap == [:]
    }
}
