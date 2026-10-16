package nfcore.plugin

import nfcore.plugin.nextflow.NextflowPipelineUtils
import nfcore.plugin.nfcore.NfcoreCitationUtils
import nfcore.plugin.nfcore.NfcoreConfigValidator
import nfcore.plugin.nfcore.NfcoreNotificationUtils
import nfcore.plugin.nfcore.NfcoreReportingOrchestrator
import nfcore.plugin.nfcore.NfcoreReportingUtils
import nfcore.plugin.nfcore.NfcoreVersionUtils
import nfcore.plugin.nfcore.SoftwareVersionReport
import spock.lang.Issue
import spock.lang.Specification

class PluginLoggingTest extends Specification {

    // The Nextflow console only shows the loggers under `nextflow`, the others only go to .nextflow.log
    @Issue("https://github.com/nf-core/nf-core-utils/issues/67")
    def 'the plugin logs under nextflow, so its warnings reach the console'() {
        expect:
        clazz.log.name == "nextflow.nfcore.${clazz.simpleName}"

        where:
        clazz << [
            NfcorePipelineObserver,
            NfUtilsExtension,
            NextflowPipelineUtils,
            NfcoreCitationUtils,
            NfcoreConfigValidator,
            NfcoreNotificationUtils,
            NfcoreReportingOrchestrator,
            NfcoreReportingUtils,
            NfcoreVersionUtils,
            SoftwareVersionReport,
        ]
    }
}
