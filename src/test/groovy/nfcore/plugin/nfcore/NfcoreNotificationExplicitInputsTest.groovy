package nfcore.plugin.nfcore

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.slf4j.LoggerFactory
import spock.lang.Narrative
import spock.lang.Issue
import spock.lang.See
import spock.lang.Specification
import spock.lang.Title

import java.nio.file.Path

@Title("Notification helpers accept explicit inputs instead of global Session")
@Narrative("""
completionSummary and getSingleReport previously read nextflow.Nextflow.session
directly. Explicit-input overloads make hidden interfaces visible and testable
without mocking global state.
""")
@Issue("https://github.com/nf-core/nf-core-utils/pull/41")
@See("https://github.com/nf-core/nf-core-utils/blob/main/docs/adr/0001-prioritize-deep-modules-around-execution-context-and-reporting.md#2-explicit-report-inputs")
class NfcoreNotificationExplicitInputsTest extends Specification {

    // --- completionSummary with explicit inputs ---

    def 'completionSummary logs success for clean run'() {
        given:
        def appender = captureLogsFor(NfcoreNotificationUtils)

        when:
        NfcoreNotificationUtils.completionSummary('nf-core/rnaseq', true, 0, true)

        then:
        logMessages(appender).any { it.contains('Pipeline completed successfully') }

        cleanup:
        detachAppender(appender)
    }

    def 'completionSummary logs success with errored processes when ignoredCount > 0'() {
        given:
        def appender = captureLogsFor(NfcoreNotificationUtils)

        when:
        NfcoreNotificationUtils.completionSummary('nf-core/rnaseq', true, 3, true)

        then:
        logMessages(appender).any { it.contains('errored process(es)') }

        cleanup:
        detachAppender(appender)
    }

    def 'completionSummary logs failure when success is false'() {
        given:
        def appender = captureLogsFor(NfcoreNotificationUtils)

        when:
        NfcoreNotificationUtils.completionSummary('nf-core/rnaseq', false, 0, true)

        then:
        logMessages(appender).any { it.contains('Pipeline completed with errors') }

        cleanup:
        detachAppender(appender)
    }

    def 'completionSummary includes workflow name in log message'() {
        given:
        def appender = captureLogsFor(NfcoreNotificationUtils)

        when:
        NfcoreNotificationUtils.completionSummary('nf-core/sarek', true, 0, true)

        then:
        logMessages(appender).any { it.contains('nf-core/sarek') }

        cleanup:
        detachAppender(appender)
    }

    def 'completionSummary uses color codes when monochrome is false'() {
        given:
        def appender = captureLogsFor(NfcoreNotificationUtils)

        when:
        NfcoreNotificationUtils.completionSummary('pipe', true, 0, false)

        then:
        logMessages(appender).any { it.contains('\033[') }

        cleanup:
        detachAppender(appender)
    }

    // --- getSingleReport with explicit workflowName ---

    def 'getSingleReport returns Path directly when given a Path'() {
        given:
        def p = Path.of('/tmp/report.html')

        expect:
        NfcoreNotificationUtils.getSingleReport(p, 'pipe') == p
    }

    def 'getSingleReport converts String to Path'() {
        expect:
        NfcoreNotificationUtils.getSingleReport('/tmp/report.html', 'pipe') == Path.of('/tmp/report.html')
    }

    def 'getSingleReport returns first element from single-element list'() {
        given:
        def p = Path.of('/tmp/report.html')

        expect:
        NfcoreNotificationUtils.getSingleReport([p], 'pipe') == p
    }

    def 'getSingleReport warns and returns first from multi-element list'() {
        given:
        def appender = captureLogsFor(NfcoreNotificationUtils)
        def p1 = Path.of('/tmp/report1.html')
        def p2 = Path.of('/tmp/report2.html')

        when:
        def result = NfcoreNotificationUtils.getSingleReport([p1, p2], 'pipe')

        then:
        result == p1
        logMessages(appender).any { it.contains('Found multiple reports') }

        cleanup:
        detachAppender(appender)
    }

    def 'getSingleReport returns null for empty list and warns'() {
        given:
        def appender = captureLogsFor(NfcoreNotificationUtils)

        when:
        def result = NfcoreNotificationUtils.getSingleReport([], 'pipe')

        then:
        result == null
        logMessages(appender).any { it.contains('No reports found') }

        cleanup:
        detachAppender(appender)
    }

    def 'getSingleReport returns null for null input'() {
        expect:
        NfcoreNotificationUtils.getSingleReport(null, 'pipe') == null
    }

    // --- helpers ---

    private ListAppender<ILoggingEvent> captureLogsFor(Class clazz) {
        def appender = new ListAppender<ILoggingEvent>()
        def logger = (Logger) LoggerFactory.getLogger(clazz)
        appender.start()
        logger.addAppender(appender)
        return appender
    }

    private List<String> logMessages(ListAppender<ILoggingEvent> appender) {
        appender.list.collect { it.formattedMessage }
    }

    private void detachAppender(ListAppender<ILoggingEvent> appender) {
        def logger = (Logger) LoggerFactory.getLogger(NfcoreNotificationUtils)
        logger.detachAppender(appender)
    }
}
