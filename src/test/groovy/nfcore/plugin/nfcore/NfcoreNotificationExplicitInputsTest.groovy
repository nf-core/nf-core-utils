package nfcore.plugin.nfcore

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.sun.net.httpserver.HttpServer
import nextflow.Nextflow
import nextflow.Session
import nextflow.config.Manifest
import nextflow.script.WorkflowMetadata
import org.slf4j.LoggerFactory
import spock.util.mop.ConfineMetaClassChanges
import spock.lang.Narrative
import spock.lang.Issue
import spock.lang.See
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Title

import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.FileSystems

@Title("Notification helpers accept explicit inputs instead of global Session")
@Narrative("""
completionSummary and getSingleReport previously read nextflow.Nextflow.session
directly. Explicit-input overloads make hidden interfaces visible and testable
without mocking global state.
""")
@Issue("https://github.com/nf-core/nf-core-utils/pull/41")
@See("https://github.com/nf-core/nf-core-utils/blob/main/docs/adr/0001-prioritize-deep-modules-around-execution-context-and-reporting.md#2-explicit-report-inputs")
class NfcoreNotificationExplicitInputsTest extends Specification {

    @TempDir
    Path tempDir

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

    // --- sending the completion e-mail ---

    @Issue(["https://github.com/nf-core/nf-core-utils/pull/71", "https://github.com/nf-core/nf-core-utils/issues/74"])
    @ConfineMetaClassChanges(Nextflow)
    def 'completionEmail preserves attachments and output when plaintext=#plaintext, sendmail=#sendmailExit, mail=#mailExit, cleanupFails=#cleanupFails, renderFails=#renderFails'() {
        given:
        def assets = tempDir.resolve('assets').toFile()
        assets.mkdirs()
        new File(assets, 'email_template.txt').text = 'Plain summary'
        new File(assets, 'email_template.html').text = '<p>HTML summary</p>'
        new File(assets, 'sendmail_template.txt').text = '''<%
def report = new File("$mqcFile")
out << 'Content-Disposition: attachment; filename="' << report.name << '"' << System.lineSeparator()
out << report.bytes.encodeBase64().toString()
new File("$projectDir/attachment-path").text = report.path
%>'''
        if (renderFails) {
            new File(assets, 'sendmail_template.txt') << '<% throw new IllegalStateException("broken template") %>'
        }
        if (cleanupFails) {
            new File(assets, 'sendmail_template.txt') << '''<%
report.delete()
report.mkdir()
new File(report, 'keep').text = 'prevent deletion'
%>'''
        }
        // This unused path must not prevent sending or saving the reports.
        tempDir.resolve('.sendmail_tmp.html').toFile().mkdir()
        def zip = zipWithReport('<html>report</html>')
        def fs = FileSystems.newFileSystem(URI.create("jar:${zip.toUri()}"), [:])
        def outdir = fs.getPath('/results')
        def metadata = Mock(WorkflowMetadata) {
            getLaunchDir() >> tempDir
            getProjectDir() >> tempDir
            getSuccess() >> true
        }
        def session = Stub(Session) {
            getManifest() >> Stub(Manifest) { getName() >> 'review'; getVersion() >> '1.0' }
            getConfig() >> [params: [max_multiqc_email_size: '1 MB']]
            getWorkflowMetadata() >> metadata
        }
        Nextflow.metaClass.static.getSession = { -> session }
        [sendmail: sendmailExit, mail: mailExit].each { name, status ->
            tempDir.resolve(name).toFile().text = """#!/bin/sh
cat > "\$0.body"
echo finished > "\$0.finished"
exit ${status}
"""
        }
        GroovySpy(NfcoreNotificationUtils, global: true)
        NfcoreNotificationUtils.emailCommands(_, _, _, _, _, _) >> {
            assert Files.readString(outdir.resolve('pipeline_info/pipeline_report.html')) == '<p>HTML summary</p>'
            assert Files.readString(outdir.resolve('pipeline_info/pipeline_report.txt')) == 'Plain summary'
            callRealMethod().collect { entry ->
                entry + [command: ['sh', tempDir.resolve(entry.name).toString()] + entry.command.drop(1)]
            }
        }
        def appender = captureLogsFor(NfcoreNotificationUtils)

        when:
        NfcoreNotificationUtils.completionEmail([:], 'test@example.invalid', null, plaintext, outdir.toUri().toString(), true, [fs.getPath('/multiqc_report.html')])

        then:
        noExceptionThrown()
        tempDir.resolve('sendmail.finished').toFile().exists() == !plaintext
        tempDir.resolve('mail.finished').toFile().exists() == (plaintext || sendmailExit != 0)
        plaintext || tempDir.resolve('sendmail.body').toFile().text == (renderFails ? '<p>HTML summary</p>' : 'Content-Disposition: attachment; filename="multiqc_report.html"\nPGh0bWw+cmVwb3J0PC9odG1sPg==')
        !(plaintext || sendmailExit != 0) || tempDir.resolve('mail.body').toFile().text == (plaintext ? 'Plain summary' : '<p>HTML summary</p>')
        logMessages(appender).count { it.contains('Sent summary e-mail') } == (deliveredBy ? 1 : 0)
        !deliveredBy || logMessages(appender).any { it.contains("(${deliveredBy})-") }
        logMessages(appender).any { it.contains('[review] Could not send the summary e-mail') } == !deliveredBy
        Files.readString(outdir.resolve('pipeline_info/pipeline_report.html')) == '<p>HTML summary</p>'
        Files.readString(outdir.resolve('pipeline_info/pipeline_report.txt')) == 'Plain summary'
        Files.readString(fs.getPath('/multiqc_report.html')) == '<html>report</html>'
        tempDir.resolve('attachment-path').toFile().exists() == !plaintext
        if (!plaintext) {
            def attachment = new File(tempDir.resolve('attachment-path').toFile().text)
            assert attachment.exists() == cleanupFails
            assert attachment.parentFile.exists() == cleanupFails
        }
        logMessages(appender).any { it.contains('Could not remove temporary file') } == cleanupFails
        logMessages(appender).any { it.contains('Failed to render sendmail template') } == renderFails

        cleanup:
        detachAppender(appender)
        fs?.close()
        if (tempDir.resolve('attachment-path').toFile().exists()) {
            def attachment = new File(tempDir.resolve('attachment-path').toFile().text)
            attachment.deleteDir()
            // Do not remove the system temp directory when running against the old implementation.
            if (attachment.parentFile.name.startsWith('multiqc_report')) attachment.parentFile.deleteDir()
        }

        where:
        plaintext | sendmailExit | mailExit | cleanupFails | renderFails || deliveredBy
        false     | 0            | 0        | false        | false       || 'sendmail'
        false     | 75           | 0        | false        | false       || 'mail'
        false     | 75           | 1        | false        | false       || null
        true      | 0            | 0        | false        | false       || 'mail'
        false     | 0            | 0        | true         | false       || 'sendmail'
        false     | 0            | 0        | false        | true        || 'sendmail'
    }

    @Issue("https://github.com/nf-core/nf-core-utils/issues/74")
    @ConfineMetaClassChanges(Nextflow)
    def 'completionEmail skips attachment access and sendmail rendering for #scenario'() {
        given:
        def assets = Files.createDirectories(tempDir.resolve('assets'))
        assets.resolve('email_template.txt').text = 'Plain summary'
        assets.resolve('email_template.html').text = '<p>HTML summary</p>'
        assets.resolve('sendmail_template.txt').text = '<% throw new IllegalStateException("sendmail must not render") %>'
        def fs = FileSystems.newFileSystem(zipWithReport('<html>report</html>'))
        def report = fs.getPath('/multiqc_report.html')
        fs.close() // Any size check or download now fails visibly, as in the issue reproducer.
        def metadata = Mock(WorkflowMetadata) {
            getLaunchDir() >> tempDir
            getProjectDir() >> tempDir
            getSuccess() >> success
        }
        def session = Stub(Session) {
            getManifest() >> Stub(Manifest) { getName() >> 'review'; getVersion() >> '1.0' }
            getConfig() >> [params: [max_multiqc_email_size: '1 MB']]
            getWorkflowMetadata() >> metadata
        }
        Nextflow.metaClass.static.getSession = { -> session }
        def script = tempDir.resolve('mail.sh')
        script.text = 'cat > "$0.body"'
        GroovySpy(NfcoreNotificationUtils, global: true)
        NfcoreNotificationUtils.emailCommands(_, _, _, _, _, _) >> {
            callRealMethod().collect { entry -> entry + [command: ['sh', script.toString()]] }
        }
        def appender = captureLogsFor(NfcoreNotificationUtils)

        when:
        NfcoreNotificationUtils.completionEmail([:], email, emailOnFail, plaintext, tempDir.resolve('results').toString(), true, [report])

        then:
        !appender.list.any { it.level == Level.WARN || it.level == Level.ERROR }
        tempDir.resolve('mail.sh.body').toFile().exists() == plaintext
        !plaintext || tempDir.resolve('mail.sh.body').text == 'Plain summary'
        tempDir.resolve('results/pipeline_info/pipeline_report.html').text == '<p>HTML summary</p>'
        tempDir.resolve('results/pipeline_info/pipeline_report.txt').text == 'Plain summary'

        cleanup:
        detachAppender(appender)

        where:
        scenario                  | success | email                  | emailOnFail            | plaintext
        'success, failure only'   | true    | null                   | 'fail@example.invalid' | false
        'no recipient'            | false   | null                   | null                   | false
        'plaintext success'       | true    | 'test@example.invalid' | null                   | true
        'plaintext failure only'  | false   | null                   | 'fail@example.invalid' | true
    }

    @Issue("https://github.com/nf-core/nf-core-utils/pull/71")
    def 'mail command deadline terminates a process that stalls with consumeInput=#consumeInput'() {
        given:
        def pidFile = tempDir.resolve('pid').toFile()
        def script = tempDir.resolve('stall.sh').toFile()
        script.text = """#!/bin/sh
echo \$\$ > '${pidFile}'
${consumeInput ? 'cat > /dev/null' : ''}
exec sleep 60
"""
        def started = System.nanoTime()

        when:
        NfcoreNotificationUtils.runEmailCommand(['sh', script.toString()], 'x' * 1_048_576, 1)

        then:
        def error = thrown(IOException)
        error.message.contains('timed out')
        (System.nanoTime() - started) / 1e9 < 10
        !ProcessHandle.of(pidFile.text.trim().toLong()).map { it.isAlive() }.orElse(false)

        cleanup:
        if (pidFile.exists()) {
            ProcessHandle.of(pidFile.text.trim().toLong()).ifPresent { it.destroyForcibly() }
        }

        where:
        consumeInput << [true, false]
    }

    @Issue("https://github.com/nf-core/nf-core-utils/pull/71")
    @ConfineMetaClassChanges(Nextflow)
    def 'IM notification logs never include webhook credentials for response #status'() {
        given:
        def secret = 'synthetic-webhook-token'
        def server = HttpServer.create(new InetSocketAddress('127.0.0.1', 0), 0)
        server.createContext('/') { exchange ->
            exchange.requestBody.close()
            def body = "Rejected token: ${secret}".getBytes('UTF-8')
            exchange.sendResponseHeaders(status, body.length)
            exchange.responseBody.withCloseable { it.write(body) }
        }
        server.start()
        def assets = tempDir.resolve('assets').toFile()
        assets.mkdirs()
        new File(assets, 'adaptivecard.json').text = '{"text":"test"}'
        def metadata = Mock(WorkflowMetadata) { getProjectDir() >> tempDir }
        def session = Stub(Session) {
            getManifest() >> Stub(Manifest) { getVersion() >> '1.0' }
            getWorkflowMetadata() >> metadata
        }
        Nextflow.metaClass.static.getSession = { -> session }
        def appender = captureLogsFor(NfcoreNotificationUtils)

        when:
        NfcoreNotificationUtils.imNotification([:], "http://127.0.0.1:${server.address.port}/services/${secret}?token=${secret}")
        // Malformed URLs can be echoed in exception messages too.
        NfcoreNotificationUtils.imNotification([:], "${secret}://invalid")

        then:
        !logMessages(appender).any { it.contains(secret) }
        logMessages(appender).contains(status == 200 ? 'IM notification sent successfully' : "IM notification failed with response code ${status}".toString())
        logMessages(appender).any { it.contains('Failed to send IM notification (MalformedURLException)') }

        cleanup:
        detachAppender(appender)
        server?.stop(0)

        where:
        status << [200, 401]
    }

    def 'emailCommands sends the text e-mail with mail for a plaintext e-mail'() {
        expect:
        NfcoreNotificationUtils.emailCommands(true, 'subject', 'a@b.c', 'sendmail', '<p>html</p>', 'text') ==
            [[command: ['mail', '-s', 'subject', 'a@b.c'], input: 'text', name: 'mail']]
    }

    def 'emailCommands tries sendmail with the full e-mail, then mail with the HTML e-mail'() {
        expect:
        NfcoreNotificationUtils.emailCommands(false, 'subject', 'a@b.c', 'sendmail', '<p>html</p>', 'text') == [
            [command: ['sendmail', '-t'], input: 'sendmail', name: 'sendmail'],
            [command: ['mail', '-s', 'subject', '--content-type=text/html', 'a@b.c'], input: '<p>html</p>', name: 'mail'],
        ]
    }

    // --- reports on a remote file system (#65): a zip file system stands for object storage ---

    @Issue("https://github.com/nf-core/nf-core-utils/pull/71")
    def 'a failed remote report download leaves no temporary copy'() {
        given:
        def zip = zipWithReport('<html>report</html>')
        // Leave the ZIP directory and file size intact but make the compressed data unreadable.
        new RandomAccessFile(zip.toFile(), 'rw').withCloseable { file ->
            file.seek(26)
            int nameLength = Short.toUnsignedInt(Short.reverseBytes(file.readShort()))
            int extraLength = Short.toUnsignedInt(Short.reverseBytes(file.readShort()))
            file.seek(30 + nameLength + extraLength)
            file.writeByte(7) // Invalid DEFLATE block type.
        }
        def fs = FileSystems.newFileSystem(zip)
        def tempCopies = {
            Files.list(Path.of(System.getProperty('java.io.tmpdir'))).withCloseable { paths ->
                paths.filter { it.fileName.toString().startsWith('multiqc_report') }.toList().toSet()
            }
        }
        def before = tempCopies()
        def appender = captureLogsFor(NfcoreNotificationUtils)

        when:
        def report = NfcoreNotificationUtils.localReport(fs.getPath('/multiqc_report.html'), 1024)

        then:
        report == null
        logMessages(appender).any { it.contains('Could not read the MultiQC report') }
        tempCopies() == before

        cleanup:
        detachAppender(appender)
        fs?.close()
    }

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

    @Issue(["https://github.com/nf-core/nf-core-utils/issues/65", "https://github.com/nf-core/nf-core-utils/issues/74"])
    def 'localReport preserves remote basename #name and content'() {
        given:
        def zip = zipWithReport('<html>report</html>', name)
        def fs = java.nio.file.FileSystems.newFileSystem(zip)
        def remote = fs.getPath('/' + name)

        when:
        def local = NfcoreNotificationUtils.localReport(remote, 1024)

        then:
        NfcoreNotificationUtils.isLocal(local)
        local.fileName.toString() == name
        local.toFile().text == '<html>report</html>'

        cleanup:
        java.nio.file.Files.deleteIfExists(local)
        if (local?.parent?.fileName?.toString()?.startsWith('multiqc_report')) Files.deleteIfExists(local.parent)
        fs.close()

        where:
        name << ['multiqc_report.html', 'custom report.html']
    }

    @Issue("https://github.com/nf-core/nf-core-utils/issues/65")
    def 'localReport does not download a remote report too large to be attached'() {
        given:
        def zip = zipWithReport('<html>report</html>')
        def fs = java.nio.file.FileSystems.newFileSystem(zip)

        expect:
        NfcoreNotificationUtils.localReport(fs.getPath('/multiqc_report.html'), limit) == null

        cleanup:
        fs.close()

        where:
        limit << [0, 10, 19] // The 19-byte report is also excluded at the exact limit.
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

    private Path zipWithReport(String content, String name = 'multiqc_report.html') {
        def zip = java.nio.file.Files.createTempFile('work', '.zip')
        new java.util.zip.ZipOutputStream(java.nio.file.Files.newOutputStream(zip)).withCloseable { out ->
            out.putNextEntry(new java.util.zip.ZipEntry(name))
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
