package nfcore.plugin

import spock.lang.Specification
import spock.lang.Unroll
import nfcore.plugin.nfcore.NfcoreConfigValidator
import spock.lang.PendingFeature

class NfcorePipelineObserverTest extends Specification {
    // This file is now reserved for observer-specific logic tests.
    // All config, version, and factory tests have been moved to dedicated files.

    @PendingFeature()
    def 'onFlowCreate should call configValidator methods with correct arguments'() {
        given:
        def validator = Spy(NfcoreConfigValidator)
        def observer = new NfcorePipelineObserver(validator)
        def session = Mock(nextflow.Session) {
            workflowMetadata >> [projectName: 'test-pipeline']
            config >> [profile: 'standard', configFiles: ['main.config']]
            profile >> 'standard'
            commandLine >> '--foo bar'
        }

        when:
        observer.onFlowCreate(session)

        then:
        1 * validator.checkConfigProvided('test-pipeline', { it.profile == 'standard' && it.configFiles == ['main.config'] })
        1 * validator.checkProfileProvided('standard', '--foo bar')
    }

    def 'onFlowCreate should print the start message'() {
        given:
        def observer = new NfcorePipelineObserver()
        def session = Mock(nextflow.Session) {
            workflowMetadata >> [projectName: 'test-pipeline']
            config >> [:]
            profile >> 'standard'
            commandLine >> ''
        }
        def out = new ByteArrayOutputStream()
        def oldOut = System.out
        System.out = new PrintStream(out)

        when:
        observer.onFlowCreate(session)
        System.out.flush()

        then:
        out.toString().contains('Pipeline is starting! 🚀')

        cleanup:
        System.out = oldOut
    }

    def 'onFlowComplete should print the complete message'() {
        given:
        def observer = new NfcorePipelineObserver()
        def out = new ByteArrayOutputStream()
        def oldOut = System.out
        System.out = new PrintStream(out)

        when:
        observer.onFlowComplete()
        System.out.flush()

        then:
        out.toString().contains('Pipeline complete! 👋')

        cleanup:
        System.out = oldOut
    }

    @PendingFeature()
    def 'onFlowCreate handles missing metadata gracefully'() {
        given:
        def validator = Spy(NfcoreConfigValidator)
        def observer = new NfcorePipelineObserver(validator)
        def session = Mock(nextflow.Session) {
            workflowMetadata >> null
            config >> [profile: 'standard', configFiles: []]
            profile >> 'standard'
            commandLine >> ''
        }

        when:
        observer.onFlowCreate(session)

        then:
        1 * validator.checkConfigProvided(null, { it.profile == 'standard' && it.configFiles == [] })
        1 * validator.checkProfileProvided('standard', '')
    }
} 