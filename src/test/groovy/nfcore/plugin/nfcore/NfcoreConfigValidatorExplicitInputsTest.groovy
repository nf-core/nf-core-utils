package nfcore.plugin.nfcore

import nextflow.exception.AbortOperationException
import spock.lang.Narrative
import spock.lang.Issue
import spock.lang.See
import spock.lang.Specification
import spock.lang.Title

@Title("Config validation accepts explicit inputs instead of global Session")
@Narrative("""
validateConfig previously read nextflow.Nextflow.session directly for profile,
commandLine, projectName, and config. The explicit-input overload makes the
hidden interface visible and testable without global state.
""")
@Issue("https://github.com/nf-core/nf-core-utils/pull/41")
@See("https://github.com/nf-core/nf-core-utils/blob/main/docs/adr/0001-prioritize-deep-modules-around-execution-context-and-reporting.md#2-explicit-report-inputs")
class NfcoreConfigValidatorExplicitInputsTest extends Specification {

    @Issue('https://github.com/nf-core/nf-core-utils/issues/73')
    def 'validateConfig with explicit inputs detects trailing comma in profile'() {
        when:
        NfcoreConfigValidator.validateConfig('test,', '', 'pipe', [:])

        then:
        thrown(AbortOperationException)
    }

    def 'validateConfig with explicit inputs accepts valid profile'() {
        when:
        NfcoreConfigValidator.validateConfig('test,docker', '', 'pipe', [profile: 'test,docker', configFiles: ['a', 'b']])

        then:
        noExceptionThrown()
    }

    def 'validateConfig with explicit inputs checks config'() {
        when:
        def result = NfcoreConfigValidator.validateConfig(
            'standard', '', 'pipe',
            [profile: 'standard', configFiles: ['main.config']]
        )

        then:
        // standard profile with <=1 configFile should warn (return false from checkConfigProvided)
        noExceptionThrown()
    }

    def 'validateConfig with PipelineExecutionContext delegates correctly'() {
        given:
        def ctx = new PipelineExecutionContext(
            profile: 'test',
            projectName: 'nf-core/rnaseq',
            config: [profile: 'test', configFiles: ['a.config', 'b.config']]
        )

        when:
        NfcoreConfigValidator.validateConfig(ctx)

        then:
        noExceptionThrown()
    }

    @Issue('https://github.com/nf-core/nf-core-utils/issues/73')
    def 'validateConfig with PipelineExecutionContext detects trailing comma'() {
        given:
        def ctx = new PipelineExecutionContext(
            profile: 'test,',
            projectName: 'pipe',
            config: [:]
        )

        when:
        NfcoreConfigValidator.validateConfig(ctx)

        then:
        thrown(AbortOperationException)
    }
}
