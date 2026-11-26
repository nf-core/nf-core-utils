package nfcore.plugin

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.slf4j.LoggerFactory
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

/** Beta functions warn once per run (ADR-0003). */
class NfUtilsExtensionBetaTest extends Specification {

    ListAppender<ILoggingEvent> appender = new ListAppender<ILoggingEvent>()
    Logger extensionLog = (Logger) LoggerFactory.getLogger('nextflow.nfcore.NfUtilsExtension')

    @Shared
    @TempDir
    Path tempDir

    @Shared
    String schema

    def setupSpec() {
        def file = tempDir.resolve('schema_references.json')
        file.text = '{"items": {"properties": {"fasta": {"meta": ["fasta"]}}}}'
        schema = file.toString()
    }

    def setup() {
        appender.start()
        extensionLog.addAppender(appender)
    }

    def cleanup() {
        extensionLog.detachAppender(appender)
    }

    // shortcut: log.warn1 dedups for the whole JVM, so this must stay the only spec that calls the extension's beta functions
    def '#function warns that it is beta once, however often it is called'() {
        given:
        def extension = new NfUtilsExtension()

        when:
        2.times { extension."${function}"(args) }

        then:
        appender.list.count { it.formattedMessage == "${function}() is beta: its arguments and output may change in a minor release" } == 1

        where:
        function            | args
        'genomeExistsError' | [genome: 'GRCh38', genomes: [GRCh38: [:]]]
        'getReferenceNames' | [schema: schema]
        'getReferences'     | [schema: schema, genome: 'GRCh38', genomes: [GRCh38: [:]]]
    }
}
