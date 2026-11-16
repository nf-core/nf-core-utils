package nfcore.plugin

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
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

    def 'completionEmail accepts text for plaintextEmail and monochromeLogs'() {
        when:
        new NfUtilsExtension().completionEmail([:], 'a@example.invalid', null, 'true', './results', 'false', [])

        then: 'the call dispatches and reaches the notification code, which has no session here'
        noExceptionThrown()
        appender.list.any { it.formattedMessage.contains('session is null') }
    }
}
