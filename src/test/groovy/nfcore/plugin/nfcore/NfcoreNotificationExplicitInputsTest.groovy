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

    // --- reports on a remote file system (#65): a zip file system stands for object storage ---

    @Issue("https://github.com/nf-core/nf-core-utils/issues/65")
    def 'getSingleReport keeps the file system of a report given as a string'() {
        given:
        def zip = zipWithReport('<html>report</html>')

        when:
        def report = NfcoreNotificationUtils.getSingleReport(["jar:${zip.toUri()}!/multiqc_report.html".toString()], 'pipe')

        then:
        !NfcoreNotificationUtils.isLocal(report)
        report.text == '<html>report</html>'

        cleanup:
        report?.fileSystem?.close()
    }

    @Issue("https://github.com/nf-core/nf-core-utils/issues/65")
    def 'localReport copies a remote report to a local file'() {
        given:
        def zip = zipWithReport('<html>report</html>')
        def fs = java.nio.file.FileSystems.newFileSystem(zip)
        def remote = fs.getPath('/multiqc_report.html')

        when:
        def local = NfcoreNotificationUtils.localReport(remote, 1024)

        then:
        NfcoreNotificationUtils.isLocal(local)
        local.toFile().text == '<html>report</html>'

        cleanup:
        java.nio.file.Files.deleteIfExists(local)
        fs.close()
    }

    @Issue("https://github.com/nf-core/nf-core-utils/issues/65")
    def 'localReport does not download a remote report too large to be attached'() {
        given:
        def zip = zipWithReport('<html>report</html>')
        def fs = java.nio.file.FileSystems.newFileSystem(zip)

        expect:
        NfcoreNotificationUtils.localReport(fs.getPath('/multiqc_report.html'), 10) == null

        cleanup:
        fs.close()
    }

    @Issue("https://github.com/nf-core/nf-core-utils/issues/65")
    def 'localReport keeps a local report, and warns about a missing remote one'() {
        given:
        def local = java.nio.file.Files.createTempFile('report', '.html')
        def fs = java.nio.file.FileSystems.newFileSystem(zipWithReport('x'))
        def appender = captureLogsFor(NfcoreNotificationUtils)

        expect:
        NfcoreNotificationUtils.localReport(local, 0) == local
        NfcoreNotificationUtils.localReport(null, 1024) == null
        NfcoreNotificationUtils.localReport(fs.getPath('/missing.html'), 1024) == null
        logMessages(appender).any { it.contains('Could not read the MultiQC report') }

        cleanup:
        detachAppender(appender)
        java.nio.file.Files.deleteIfExists(local)
        fs.close()
    }

    private Path zipWithReport(String content) {
        def zip = java.nio.file.Files.createTempFile('work', '.zip')
        new java.util.zip.ZipOutputStream(java.nio.file.Files.newOutputStream(zip)).withCloseable { out ->
            out.putNextEntry(new java.util.zip.ZipEntry('multiqc_report.html'))
            out.write(content.bytes)
            out.closeEntry()
        }
        zip.toFile().deleteOnExit()
        return zip
    }

    // --- helpers ---

    private ListAppender<ILoggingEvent> captureLogsFor(Class clazz) {
        def appender = new ListAppender<ILoggingEvent>()
        def logger = (Logger) LoggerFactory.getLogger("nextflow.nfcore.${clazz.simpleName}")
        appender.start()
        logger.addAppender(appender)
        return appender
    }

    private List<String> logMessages(ListAppender<ILoggingEvent> appender) {
        appender.list.collect { it.formattedMessage }
    }

    private void detachAppender(ListAppender<ILoggingEvent> appender) {
        def logger = (Logger) LoggerFactory.getLogger("nextflow.nfcore.NfcoreNotificationUtils")
        logger.detachAppender(appender)
    }
}
