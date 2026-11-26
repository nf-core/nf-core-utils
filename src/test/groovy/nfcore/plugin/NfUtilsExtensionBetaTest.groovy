package nfcore.plugin

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import nextflow.Session
import org.slf4j.LoggerFactory
import spock.lang.Specification

/** Beta functions warn once per run (ADR-0003). */
class NfUtilsExtensionBetaTest extends Specification {

    ListAppender<ILoggingEvent> appender = new ListAppender<ILoggingEvent>()
    Logger extensionLog = (Logger) LoggerFactory.getLogger('nextflow.nfcore.NfUtilsExtension')

    def setup() {
        appender.start()
        extensionLog.addAppender(appender)
    }

    def cleanup() {
        extensionLog.detachAppender(appender)
    }

    // shortcut: log.warn1 dedups for the whole JVM, so this must stay the only spec that calls the extension's genomeExistsError()
    def 'genomeExistsError() warns that it is beta once, however often it is called'() {
        given:
        def extension = new NfUtilsExtension()
        extension.session = Mock(Session) {
            getConfig() >> [params: [genome: 'GRCh38', genomes: [GRCh38: [:]]]]
        }

        when:
        extension.genomeExistsError()
        extension.genomeExistsError()

        then:
        appender.list.count { it.formattedMessage == 'genomeExistsError() is beta: its arguments and output may change in a minor release' } == 1
    }
}
