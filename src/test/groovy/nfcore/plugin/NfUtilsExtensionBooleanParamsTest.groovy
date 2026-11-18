package nfcore.plugin

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import nextflow.Session
import nextflow.exception.AbortOperationException
import nfcore.plugin.nfcore.PipelineExecutionContext
import nfcore.plugin.nfcore.ValidationAdapter
import org.slf4j.LoggerFactory
import spock.lang.Issue
import spock.lang.Specification

/** Command-line flags reach the plugin as Strings, so these functions must dispatch on text. */
@Issue("https://github.com/nf-core/nf-core-utils/issues/94")
class NfUtilsExtensionBooleanParamsTest extends Specification {

    ListAppender<ILoggingEvent> appender = new ListAppender<ILoggingEvent>()
    Logger notificationLog = (Logger) LoggerFactory.getLogger('nextflow.nfcore.NfcoreNotificationUtils')

    def setup() {
        appender.start()
        notificationLog.addAppender(appender)
    }

    def cleanup() {
        notificationLog.detachAppender(appender)
    }

    def 'completionSummary(#flag) colours the log only when the flag means false'() {
        when:
        new NfUtilsExtension().completionSummary(flag)

        then:
        appender.list.any { it.formattedMessage.contains('\033[') } == coloured

        where:
        flag    || coloured
        'true'  || false
        'false' || true
        false   || true
        true    || false
    }

    def 'checkProfileProvided(args, #flag) colours the error only when the flag means false'() {
        given:
        def extension = new NfUtilsExtension()
        extension.validationAdapter = new ValidationAdapter(new PipelineExecutionContext(profile: 'test,'))

        when:
        extension.checkProfileProvided([], flag)

        then:
        def e = thrown(AbortOperationException)
        e.message.contains('\033[') == coloured

        where:
        flag    || coloured
        'true'  || false
        'false' || true
    }

    def 'checkProfileProvided(args) still defaults to monochrome'() {
        given:
        def extension = new NfUtilsExtension()
        extension.validationAdapter = new ValidationAdapter(new PipelineExecutionContext(profile: 'test,'))

        when:
        extension.checkProfileProvided([])

        then:
        def e = thrown(AbortOperationException)
        !e.message.contains('\033[')
    }

    def 'generateYamlFromEvalSyntax(evalData, #flag) adds the workflow entry only when the flag means true'() {
        given:
        def extension = new NfUtilsExtension()
        extension.session = Mock(Session)

        when:
        def yaml = extension.generateYamlFromEvalSyntax([['FASTQC', 'fastqc', '0.12.1']], flag)

        then:
        yaml.contains('fastqc: 0.12.1')
        yaml.contains('Workflow:') == included

        where:
        flag    || included
        'true'  || true
        'false' || false
        true    || true
        false   || false
    }

    def 'generateYamlFromEvalSyntax(evalData) still adds the workflow entry'() {
        given:
        def extension = new NfUtilsExtension()
        extension.session = Mock(Session)

        expect:
        extension.generateYamlFromEvalSyntax([['FASTQC', 'fastqc', '0.12.1']]).contains('Workflow:')
    }

    def 'logColours(#flag) returns escape codes only when the flag means false'() {
        when:
        def colours = new NfUtilsExtension().logColours(flag)

        then:
        colours.red == red
        colours.reset == reset
        colours.values().every { it.startsWith('\033[') } == coloured

        where:
        flag    || red          | reset     | coloured
        'true'  || ''           | ''        | false
        'false' || '\033[0;31m' | '\033[0m' | true
        true    || ''           | ''        | false
        false   || '\033[0;31m' | '\033[0m' | true
    }

    def 'completionEmail accepts text for plaintextEmail and monochromeLogs'() {
        when:
        new NfUtilsExtension().completionEmail([:], 'a@example.invalid', null, 'true', './results', 'false', [])

        then: 'the call dispatches and reaches the notification code, which has no session here'
        noExceptionThrown()
        appender.list.any { it.formattedMessage.contains('session is null') }
    }
}
