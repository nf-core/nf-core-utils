package nfcore.plugin

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import nextflow.Nextflow
import nextflow.Session
import nextflow.SysEnv
import nextflow.config.Manifest
import nextflow.exception.AbortOperationException
import nextflow.mail.Mail
import nextflow.script.ScriptBinding
import nextflow.script.WorkflowMetadata
import org.codehaus.groovy.runtime.InvokerHelper
import org.slf4j.LoggerFactory
import spock.lang.*
import spock.util.mop.ConfineMetaClassChanges

import java.nio.file.Files
import java.nio.file.Path

@Title('Public extension functions accept untyped CLI boolean parameters')
@Narrative('Strict syntax passes untyped CLI flags as strings; false must not become Groovy truth, and invalid completion flags must not drop notifications.')
@Issue('https://github.com/nf-core/nf-core-utils/issues/94')
@See('https://github.com/nf-core/nf-core-utils/blob/main/ARCHITECTURE.md#boolean-cli-parameter-compatibility')
class NfUtilsExtensionBooleanParamsTest extends Specification {
    @TempDir Path tempDir
    Logger logger
    ListAppender<ILoggingEvent> logs

    def setup() {
        logger = (Logger) LoggerFactory.getLogger('nextflow.nfcore')
        logs = new ListAppender<ILoggingEvent>()
        logs.start()
        logger.addAppender(logs)
    }

    def cleanup() {
        logger.detachAppender(logs)
        logs.stop()
    }

    private Session session(String profile = 'test') {
        def metadata = Mock(WorkflowMetadata) {
            getLaunchDir() >> tempDir
            getProjectDir() >> tempDir
            getRunName() >> 'happy_run'
            getSuccess() >> true
        }
        Stub(Session) {
            getManifest() >> Stub(Manifest) {
                getName() >> 'nf-core/review'
                getVersion() >> '1.0'
            }
            getConfig() >> [params: [:]]
            getParams() >> new ScriptBinding.ParamsMap()
            getWorkflowMetadata() >> metadata
            getProfile() >> profile
        }
    }

    private NfUtilsExtension extension(Session session) {
        def extension = new NfUtilsExtension()
        extension.init(session)
        extension
    }

    private List<String> messages() {
        logs.list*.formattedMessage
    }

    def 'summary dynamically dispatches #value and uses the requested colour mode'() {
        when:
        InvokerHelper.invokeMethod(extension(session()), 'completionSummary', [value] as Object[])

        then:
        def summary = messages().find { it.contains('Pipeline completed with errors') }
        summary != null
        summary.contains('nf-core/review')
        summary.contains('\033[') == coloured
        messages().any { it.contains('Expected monochromeLogs') && it.contains('using true') } == warned

        where:
        value     | coloured | warned
        true      | false    | false
        false     | true     | false
        'TrUe'    | false    | false
        'FALSE'   | true     | false
        ' false ' | true     | false
        null      | false    | false
        'yes'     | false    | true
        1         | false    | true
    }

    def 'summary defaults to monochrome when the argument is omitted'() {
        when:
        InvokerHelper.invokeMethod(extension(session()), 'completionSummary', [] as Object[])

        then:
        def summary = messages().find { it.contains('Pipeline completed with errors') }
        summary != null
        !summary.contains('\033[')
    }

    def 'profile validation dynamically dispatches #arguments and preserves colour behavior'() {
        when:
        InvokerHelper.invokeMethod(extension(session('test,')), 'checkProfileProvided', arguments as Object[])

        then:
        def error = thrown(AbortOperationException)
        error.message.contains('trailing comma')
        error.message.contains('\033[') == coloured

        where:
        arguments       | coloured
        [[]]            | false
        [[], null]      | false
        [[], true]      | false
        [[], false]     | true
        [[], 'TRUE']    | false
        [[], 'FaLsE']   | true
    }

    def 'profile validation rejects invalid #value with an actionable parameter name'() {
        when:
        InvokerHelper.invokeMethod(extension(session()), 'checkProfileProvided', [[], value] as Object[])

        then:
        def error = thrown(IllegalArgumentException)
        error.message.contains('monochromeLogs')
        error.message.contains('true or false')

        where:
        value << ['yes', '', 1, []]
    }

    @ConfineMetaClassChanges(Nextflow)
    def 'completionEmail dynamically dispatches plaintext=#plaintext monochrome=#monochrome and still delivers reports and mail'() {
        given:
        SysEnv.push([:])
        def currentSession = session()
        Nextflow.metaClass.static.getSession = { -> currentSession }
        def assets = Files.createDirectories(tempDir.resolve('assets'))
        assets.resolve('email_template.txt').text = 'Plain summary'
        assets.resolve('email_template.html').text = '<p>HTML summary</p>'
        def sent = []
        Nextflow.metaClass.static.sendMail = { Closure configure ->
            def mail = new Mail()
            configure.call(mail)
            sent << mail
        }
        def outdir = tempDir.resolve('results')

        when:
        InvokerHelper.invokeMethod(extension(currentSession), 'completionEmail',
            [[:], 'test@example.invalid', null, plaintext, outdir.toString(), monochrome, []] as Object[])

        then:
        Files.readString(outdir.resolve('pipeline_info/pipeline_report.txt')) == 'Plain summary'
        Files.readString(outdir.resolve('pipeline_info/pipeline_report.html')) == '<p>HTML summary</p>'
        sent.size() == 1
        sent[0].to == 'test@example.invalid'
        sent[0].subject == '[nf-core/review] Successful: happy_run'
        sent[0].text == 'Plain summary'
        sent[0].body == (plain ? null : '<p>HTML summary</p>')
        def delivery = messages().find { it.contains('Sent summary e-mail') }
        delivery != null
        delivery.contains('\033[') == coloured
        messages().any { it.contains('Expected plaintextEmail') && it.contains('using false') } == warnPlain
        messages().any { it.contains('Expected monochromeLogs') && it.contains('using true') } == warnMono

        cleanup:
        SysEnv.pop()

        where:
        plaintext | monochrome | plain | coloured | warnPlain | warnMono
        true      | true       | true  | false    | false     | false
        false     | false      | false | true     | false     | false
        'TrUe'    | 'FALSE'    | true  | true     | false     | false
        'false'   | 'TRUE'     | false | false    | false     | false
        null      | null       | false | false    | false     | false
        'yes'     | 'bad'      | false | false    | true      | true
        1         | []         | false | false    | true      | true
    }
}
