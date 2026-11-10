package nfcore.plugin

import nextflow.Nextflow
import nextflow.Session
import nextflow.SysEnv
import nextflow.config.Manifest
import nextflow.script.ScriptBinding
import nextflow.script.WorkflowMetadata
import nextflow.exception.AbortOperationException
import nfcore.plugin.NfUtilsExtension
import nfcore.plugin.nfcore.NfcoreNotificationUtils
import nfcore.plugin.references.ReferenceSelection
import org.codehaus.groovy.runtime.InvokerHelper
import spock.lang.Specification
import spock.lang.TempDir
import spock.util.mop.ConfineMetaClassChanges

import java.nio.file.Files
import java.nio.file.Path

class NfUtilsExtensionBooleanParamsTest extends Specification {
    @TempDir Path tempDir

    private Session stubSession(Map params = [:], String profile = 'test') {
        def metadata = Mock(WorkflowMetadata) {
            getLaunchDir() >> tempDir
            getProjectDir() >> tempDir
            getRunName() >> 'happy_run'
            getSuccess() >> true
        }
        def pm = new ScriptBinding.ParamsMap()
        pm.putAll(params)
        def session = Stub(Session) {
            getManifest() >> Stub(Manifest) { getName() >> 'nf-core/review'; getVersion() >> '1.0' }
            getConfig() >> [params: [max_multiqc_email_size: '1 MB']]
            getParams() >> pm
            getWorkflowMetadata() >> metadata
            getProfile() >> profile
        }
        Nextflow.metaClass.static.getSession = { -> session }
        return session
    }

    private NfUtilsExtension ext(Session s) {
        def e = new NfUtilsExtension()
        e.init(s)
        return e
    }

    def 'completionSummary accepts Booleans, strings and null'() {
        given:
        def e = ext(stubSession())

        when:
        InvokerHelper.invokeMethod(e, 'completionSummary', [value] as Object[])

        then:
        noExceptionThrown()

        where:
        value << ['true', 'FALSE', null, true, false]
    }

    def 'completionSummary warns and uses the default for an unrecognised value'() {
        when:
        InvokerHelper.invokeMethod(ext(stubSession()), 'completionSummary', ['yes'] as Object[])

        then:
        noExceptionThrown()
    }

    def 'checkProfileProvided takes strings and defaults to monochrome'() {
        given:
        def e = ext(stubSession([:], 'test,'))

        when:
        InvokerHelper.invokeMethod(e, 'checkProfileProvided', args as Object[])

        then:
        def ex = thrown(AbortOperationException)
        ex.message.contains('\033') == coloured

        where:
        args           | coloured
        [[]]           | false
        [[], null]     | false
        [[], 'true']   | false
        [[], 'false']  | true
        [[], false]    | true
    }

    def 'checkProfileProvided rejects an unrecognised value'() {
        when:
        InvokerHelper.invokeMethod(ext(stubSession([:], 'test,')), 'checkProfileProvided', [[], 'yes'] as Object[])

        then:
        thrown(IllegalArgumentException)
    }

    @ConfineMetaClassChanges(Nextflow)
    def 'completion email is still sent outside Platform when force_completion_email is #val'() {
        given:
        SysEnv.push([:])
        def assets = Files.createDirectories(tempDir.resolve('assets'))
        assets.resolve('email_template.txt').text = 'Plain summary'
        assets.resolve('email_template.html').text = '<p>HTML summary</p>'
        stubSession([force_completion_email: val])
        def sent = []
        Nextflow.metaClass.static.sendMail = { Closure c -> sent << 1 }

        when:
        NfcoreNotificationUtils.completionEmail([:], 'a@example.invalid', null, true, tempDir.resolve('results').toString(), true, [])

        then:
        sent.size() == 1

        cleanup:
        SysEnv.pop()

        where:
        val << ['yes', '', 1, 'true']
    }

    def 'getReferencePath treats the string false as false'() {
        expect:
        ReferenceSelection.getReferencePath(genome: 'GRCh38', igenomesIgnore: value, datasheetPath: 's3://x') == expected

        where:
        value   | expected
        true    | 's3://x/GRCh38.yml'
        'true'  | 's3://x/GRCh38.yml'
        false   | null
        'false' | null
        null    | null
    }
}
