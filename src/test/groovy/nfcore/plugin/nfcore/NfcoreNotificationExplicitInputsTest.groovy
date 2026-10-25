package nfcore.plugin.nfcore

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.sun.net.httpserver.HttpServer
import nextflow.Nextflow
import nextflow.Session
import nextflow.config.Manifest
import nextflow.mail.JavaMailProvider
import nextflow.mail.Mail
import nextflow.mail.Mailer
import nextflow.mail.MailProvider
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
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

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

    @Issue(["https://github.com/nf-core/tools/issues/112", "https://github.com/nf-core/nf-core-utils/issues/65", "https://github.com/nf-core/nf-core-utils/issues/74"])
    @ConfineMetaClassChanges(Nextflow)
    def 'completionEmail sends through Nextflow sendMail when plaintext=#plaintext, logo=#hasLogo, maxSize=#maxSize, sendFails=#sendFails, cleanupFails=#cleanupFails'() {
        given:
        def assets = Files.createDirectories(tempDir.resolve('assets'))
        assets.resolve('email_template.txt').text = 'Plain summary'
        assets.resolve('email_template.html').text = '<p>HTML summary <img src="cid:nfcorepipelinelogo"></p>'
        def logo = assets.resolve('nf-core-review_logo_light.png')
        if (hasLogo) {
            logo.bytes = [1, 2, 3] as byte[]
        }
        // Registered by URI, so that the outdir URI resolves to this file system instance
        def fs = FileSystems.newFileSystem(URI.create("jar:${zipWithReport('<html>report</html>').toUri()}"), [:])
        def outdir = fs.getPath('/results')
        stubSession(true, maxSize)
        def sent = []
        def staged = []
        Nextflow.metaClass.static.sendMail = { Closure params ->
            assert Files.readString(outdir.resolve('pipeline_info/pipeline_report.txt')) == 'Plain summary'
            def mail = mailOf(params)
            staged.addAll(mail.attachments.findAll { !it.contentId }*.file)
            if (sendFails) {
                throw new IOException('SMTP unavailable')
            }
            def mime = new ByteArrayOutputStream()
            new Mailer().createMimeMessage(mail).writeTo(mime)
            sent << [mail: mail, mime: mime.toString('UTF-8')]
            if (cleanupFails) {
                staged.each { it.delete(); it.mkdir(); new File(it, 'keep').text = 'prevent deletion' }
            }
        }
        def appender = captureLogsFor(NfcoreNotificationUtils)

        when:
        NfcoreNotificationUtils.completionEmail([:], 'test@example.invalid', null, plaintext, outdir.toUri().toString(), true, [fs.getPath('/multiqc_report.html')])

        then:
        noExceptionThrown()
        Files.readString(outdir.resolve('pipeline_info/pipeline_report.html')) == '<p>HTML summary <img src="cid:nfcorepipelinelogo"></p>'
        Files.readString(outdir.resolve('pipeline_info/pipeline_report.txt')) == 'Plain summary'
        Files.readString(fs.getPath('/multiqc_report.html')) == '<html>report</html>'
        sent.size() == (sendFails ? 0 : 1)
        if (!sendFails) {
            Mail mail = sent.first().mail
            def attachments = mail.attachments ?: []
            verifyAll {
                mail.to == 'test@example.invalid'
                mail.subject == '[nf-core/review] Successful: happy_run'
                mail.text == 'Plain summary'
                mail.body == (plaintext ? null : '<p>HTML summary <img src="cid:nfcorepipelinelogo"></p>')
                attachments.findAll { it.contentId }.collect { [it.contentId, it.disposition, it.file] } ==
                    (logoAttached ? [['<nfcorepipelinelogo>', 'inline', logo.toFile()]] : [])
                attachments.findAll { !it.contentId }*.fileName == (reportAttached ? ['multiqc_report.html'] : [])
                sent.first().mime.contains('Content-ID: <nfcorepipelinelogo>') == logoAttached
                sent.first().mime.contains('Content-Disposition: attachment; filename=multiqc_report.html') == reportAttached
                sent.first().mime.contains('<html>report</html>') == reportAttached
            }
        }
        // The local copy of the remote report is removed after sending, also when sending fails.
        staged.size() == (reportAttached ? 1 : 0)
        staged.every { it.exists() == cleanupFails && it.parentFile.exists() == cleanupFails }
        logMessages(appender).count { it.contains('Sent summary e-mail to test@example.invalid') } == (sendFails ? 0 : 1)
        logMessages(appender).any { it == '[nf-core/review] Could not send the summary e-mail to test@example.invalid: SMTP unavailable' } == sendFails
        logMessages(appender).any { it.contains('Could not remove temporary file') } == cleanupFails

        cleanup:
        detachAppender(appender)
        fs?.close()
        staged.each { it.deleteDir(); it.parentFile.deleteDir() }

        where:
        plaintext | hasLogo | maxSize | sendFails | cleanupFails || logoAttached | reportAttached
        false     | true    | '1 MB'  | false     | false        || true         | true
        false     | false   | '1 MB'  | false     | false        || false        | true
        false     | true    | '10 B'  | false     | false        || true         | false
        true      | true    | '1 MB'  | false     | false        || false        | false
        false     | true    | '1 MB'  | true      | false        || true         | true
        false     | true    | '1 MB'  | false     | true         || true         | true
    }

    @Issue("https://github.com/nf-core/nf-core-utils/pull/80")
    @ConfineMetaClassChanges(Nextflow)
    def 'completionEmail returns after 30 seconds when SMTP never sends a greeting'() {
        given: 'a real SMTP connection that ignores thread interruption'
        def assets = Files.createDirectories(tempDir.resolve('assets'))
        assets.resolve('email_template.txt').text = 'Plain summary'
        assets.resolve('email_template.html').text = '<p>HTML summary</p>'
        stubSession(false, '1 MB')
        def fs = FileSystems.newFileSystem(zipWithReport('<html>report</html>'))
        def server = new ServerSocket(0, 1, InetAddress.getByName('127.0.0.1'))
        def connection = new FutureTask<Socket>({ server.accept() } as Callable<Socket>)
        Thread.startDaemon { connection.run() }
        def sender = new AtomicReference<Thread>()
        def staged = new AtomicReference<File>()
        Nextflow.metaClass.static.sendMail = { Closure params ->
            sender.set(Thread.currentThread())
            def mail = mailOf(params)
            staged.set(mail.attachments.first().file)
            new Mailer([smtp: [host: '127.0.0.1', port: server.localPort, timeout: 45000]]) {
                protected MailProvider provider() { new JavaMailProvider() }
            }.send(mail)
        }
        def appender = captureLogsFor(NfcoreNotificationUtils)
        def started = System.nanoTime()
        def completion = new FutureTask<Void>({
            NfcoreNotificationUtils.completionEmail([:], null, 'fail@example.invalid', false,
                tempDir.resolve('results').toString(), true, fs.getPath('/multiqc_report.html'))
            return null
        } as Callable<Void>)
        // Do not let the sender inherit daemon status and mask a missing production guarantee.
        def caller = new Thread(completion, 'completion-email-test-caller')
        caller.daemon = false
        Socket socket

        when:
        caller.start()
        socket = connection.get(5, TimeUnit.SECONDS)
        completion.get(35, TimeUnit.SECONDS)

        then: 'shutdown can continue without deleting an attachment still in use'
        (System.nanoTime() - started) / 1e9 >= 29
        (System.nanoTime() - started) / 1e9 < 35
        sender.get().daemon
        sender.get().alive
        staged.get().text == '<html>report</html>'
        logMessages(appender).count { it.contains('Timed out sending the summary e-mail') } == 1
        !logMessages(appender).any { it.contains('Sent summary e-mail') }
        tempDir.resolve('results/pipeline_info/pipeline_report.txt').text == 'Plain summary'

        when: 'the blocked transport eventually exits'
        socket.close()
        sender.get().join(5000)

        then: 'the sender cleans up its copy, but not the original'
        !sender.get().alive
        !staged.get().exists()
        !staged.get().parentFile.exists()
        fs.getPath('/multiqc_report.html').text == '<html>report</html>'
        !logMessages(appender).any { it.contains('Sent summary e-mail') }

        cleanup:
        socket?.close()
        server?.close()
        completion?.cancel(true)
        caller?.join(5000)
        sender.get()?.join(5000)
        fs?.close()
        detachAppender(appender)
    }

    @Issue("https://github.com/nf-core/nf-core-utils/issues/74")
    @ConfineMetaClassChanges(Nextflow)
    def 'completionEmail skips attachment access for #scenario'() {
        given:
        def assets = Files.createDirectories(tempDir.resolve('assets'))
        assets.resolve('email_template.txt').text = 'Plain summary'
        assets.resolve('email_template.html').text = '<p>HTML summary</p>'
        def fs = FileSystems.newFileSystem(zipWithReport('<html>report</html>'))
        def report = fs.getPath('/multiqc_report.html')
        fs.close() // Any size check or download now fails visibly, as in the issue reproducer.
        stubSession(success, '1 MB')
        def sent = []
        Nextflow.metaClass.static.sendMail = { Closure params -> sent << mailOf(params) }
        def appender = captureLogsFor(NfcoreNotificationUtils)

        when:
        NfcoreNotificationUtils.completionEmail([:], email, emailOnFail, plaintext, tempDir.resolve('results').toString(), true, [report])

        then:
        !appender.list.any { it.level == Level.WARN || it.level == Level.ERROR }
        sent*.to == (plaintext ? [email ?: emailOnFail] : [])
        sent.every { it.text == 'Plain summary' && it.body == null && !it.attachments }
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
    def 'localReport keeps a local report under the size limit, and warns about a missing remote one'() {
        given:
        def local = java.nio.file.Files.createTempFile('report', '.html')
        local.text = 'x'
        def fs = java.nio.file.FileSystems.newFileSystem(zipWithReport('x'))
        def appender = captureLogsFor(NfcoreNotificationUtils)

        expect:
        NfcoreNotificationUtils.localReport(local, 1024) == local
        NfcoreNotificationUtils.localReport(local, 1) == null
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

    private void stubSession(boolean success, String maxMultiqcEmailSize) {
        def metadata = Mock(WorkflowMetadata) {
            getLaunchDir() >> tempDir
            getProjectDir() >> tempDir
            getRunName() >> 'happy_run'
            getSuccess() >> success
        }
        def session = Stub(Session) {
            getManifest() >> Stub(Manifest) { getName() >> 'nf-core/review'; getVersion() >> '1.0' }
            getConfig() >> [params: [max_multiqc_email_size: maxMultiqcEmailSize]]
            getWorkflowMetadata() >> metadata
        }
        Nextflow.metaClass.static.getSession = { -> session }
    }

    /** Evaluate a sendMail closure the way {@link Mailer#send(Closure)} does, without sending it. */
    private static Mail mailOf(Closure params) {
        def mail = new Mail()
        def copy = (Closure) params.clone()
        copy.resolveStrategy = Closure.DELEGATE_FIRST
        copy.delegate = mail
        copy.call(mail)
        return mail
    }

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
