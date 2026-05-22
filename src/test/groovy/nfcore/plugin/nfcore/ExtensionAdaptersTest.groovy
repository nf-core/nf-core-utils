package nfcore.plugin.nfcore

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.slf4j.LoggerFactory
import spock.lang.Narrative
import spock.lang.Issue
import spock.lang.See
import spock.lang.Specification
import spock.lang.Title

@Title("Focused extension adapters bridge PipelineExecutionContext to domain modules")
@Narrative("""
NfUtilsExtension delegates to focused adapters initialized with
PipelineExecutionContext. Each adapter can be tested with a fake context
and no live Nextflow Session.
""")
@Issue("https://github.com/nf-core/nf-core-utils/pull/41")
@See("https://github.com/nf-core/nf-core-utils/blob/main/docs/adr/0001-prioritize-deep-modules-around-execution-context-and-reporting.md#5-extension-adapter-modules")
class ExtensionAdaptersTest extends Specification {

    // --- ValidationAdapter ---

    def 'ValidationAdapter.checkConfigProvided delegates to validator with context'() {
        given:
        def ctx = new PipelineExecutionContext(
            projectName: 'nf-core/rnaseq',
            config: [profile: 'standard', configFiles: ['a.config', 'b.config']]
        )
        def adapter = new ValidationAdapter(ctx)

        when:
        def result = adapter.checkConfigProvided()

        then:
        result == true
    }

    def 'ValidationAdapter.checkConfigProvided warns with standard profile and single config'() {
        given:
        def appender = captureLogsFor(NfcoreConfigValidator)
        def ctx = new PipelineExecutionContext(
            projectName: 'pipe',
            config: [profile: 'standard', configFiles: ['main.config']]
        )
        def adapter = new ValidationAdapter(ctx)

        when:
        def result = adapter.checkConfigProvided()

        then:
        !result

        cleanup:
        detachAppender(appender, NfcoreConfigValidator)
    }

    def 'ValidationAdapter.checkProfileProvided detects trailing comma'() {
        given:
        def ctx = new PipelineExecutionContext(profile: 'test')
        def adapter = new ValidationAdapter(ctx)

        when:
        adapter.checkProfileProvided(['-profile', 'test,'], true)

        then:
        thrown(IllegalArgumentException)
    }

    // --- VersionAdapter ---

    def 'VersionAdapter.getWorkflowVersion formats version from context'() {
        given:
        def ctx = new PipelineExecutionContext(workflowVersion: '3.14.0')
        def adapter = new VersionAdapter(ctx)

        when:
        def result = adapter.getWorkflowVersion()

        then:
        result == 'v3.14.0'
    }

    def 'VersionAdapter.getWorkflowVersion uses explicit version over context'() {
        given:
        def ctx = new PipelineExecutionContext(workflowVersion: '3.14.0')
        def adapter = new VersionAdapter(ctx)

        when:
        def result = adapter.getWorkflowVersion('2.0.0', null)

        then:
        result == 'v2.0.0'
    }

    def 'VersionAdapter.collectVersions includes workflow from context'() {
        given:
        def ctx = new PipelineExecutionContext(
            workflowName: 'nf-core/rnaseq',
            workflowVersion: '3.14.0',
            nextflowVersion: '25.04.0'
        )
        def adapter = new VersionAdapter(ctx)

        when:
        def result = adapter.collectVersions('fastqc: 0.12.1')

        then:
        result.contains('fastqc: 0.12.1')
        result.contains('Workflow:')
        result.contains('nf-core/rnaseq:')
    }

    // --- ReportingAdapter ---

    def 'ReportingAdapter.generateVersionReport uses context'() {
        given:
        def ctx = new PipelineExecutionContext(
            workflowName: 'pipe',
            workflowVersion: '1.0.0',
            nextflowVersion: '25.04.0'
        )
        def adapter = new ReportingAdapter(ctx)

        when:
        def result = adapter.generateVersionReport(
            [['PROC', 'tool1', '1.0']], []
        )

        then:
        result.versions_yaml.contains('tool1:')
        result.versions_yaml.contains('pipe:')
    }

    // --- helpers ---

    private ListAppender<ILoggingEvent> captureLogsFor(Class clazz) {
        def appender = new ListAppender<ILoggingEvent>()
        def logger = (Logger) LoggerFactory.getLogger(clazz)
        appender.start()
        logger.addAppender(appender)
        return appender
    }

    private void detachAppender(ListAppender appender, Class clazz) {
        ((Logger) LoggerFactory.getLogger(clazz)).detachAppender(appender)
    }
}
