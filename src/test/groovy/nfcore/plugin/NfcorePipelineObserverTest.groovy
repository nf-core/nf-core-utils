package nfcore.plugin

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import java.nio.file.Paths
import nextflow.Session
import nextflow.script.ScriptBinding
import nfcore.plugin.nfcore.NfcoreConfigValidator
import org.slf4j.LoggerFactory
import spock.lang.Issue
import spock.lang.Specification

class NfcorePipelineObserverTest extends Specification {
    // This file is now reserved for observer-specific logic tests.
    // All config, version, and factory tests have been moved to dedicated files.

    @Issue("https://github.com/nf-core/nf-core-utils/issues/66")
    def 'onFlowCreate warns about the configuration and positional arguments of the run'() {
        given:
        // a real Session: Nextflow keeps the profile and config files there, not in the config map
        def session = new Session()
        session.profile = profile
        session.configFiles = configFiles.collect { Paths.get(it) }
        session.commandLine = "nextflow run main.nf -profile ${profile} ${args.join(' ')}"
        session.binding = new ScriptBinding().setArgs(args)
        def appender = new ListAppender<ILoggingEvent>()
        appender.start()
        ((Logger) LoggerFactory.getLogger("nextflow.nfcore.NfcoreConfigValidator")).addAppender(appender)

        when:
        new NfcorePipelineObserver().onFlowCreate(session)
        def warnings = appender.list.findAll { it.level == Level.WARN }*.formattedMessage

        then:
        warnings.any { it.contains('without any custom configuration') } == configWarning
        warnings.any { it.contains('positional argument') } == positionalWarning
        !warnings.any { it.contains('`nextflow`') }

        cleanup:
        ((Logger) LoggerFactory.getLogger("nextflow.nfcore.NfcoreConfigValidator")).detachAppender(appender)

        where:
        profile    | configFiles                            | args       || configWarning | positionalWarning
        'standard' | ['nextflow.config']                    | []         || true          | false
        'docker'   | ['nextflow.config']                    | []         || false         | false
        'standard' | ['nextflow.config', 'custom.config']   | []         || false         | false
        'test'     | ['nextflow.config']                    | ['docker'] || false         | true
    }

    def 'onFlowCreate throws when called directly with a profile ending in a comma'() {
        given:
        def session = new Session()
        session.profile = 'test,'
        session.configFiles = [Paths.get('nextflow.config')]
        session.binding = new ScriptBinding().setArgs(['docker'])

        when:
        new NfcorePipelineObserver().onFlowCreate(session)

        then:
        def error = thrown(IllegalArgumentException)
        error.message.contains('cannot end with a trailing comma')
    }

    def 'onFlowCreate should log start message at TRACE level'() {
        given:
        def observer = new NfcorePipelineObserver()
        def session = Mock(nextflow.Session) {
            workflowMetadata >> [projectName: 'test-pipeline']
            config >> [:]
            profile >> 'standard'
            commandLine >> ''
        }

        // Set up logging capture
        Logger logger = (Logger) LoggerFactory.getLogger("nextflow.nfcore.NfcorePipelineObserver")
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>()
        listAppender.start()
        logger.addAppender(listAppender)
        def originalLevel = logger.level
        logger.setLevel(Level.TRACE)

        when:
        observer.onFlowCreate(session)

        then:
        def logEvents = listAppender.list
        logEvents.any { it.level == Level.TRACE && it.message.contains('Pipeline is starting! 🚀') }

        cleanup:
        logger.setLevel(originalLevel)
        logger.detachAppender(listAppender)
    }

    def 'onFlowComplete should log complete message at TRACE level'() {
        given:
        def observer = new NfcorePipelineObserver()

        // Set up logging capture
        Logger logger = (Logger) LoggerFactory.getLogger("nextflow.nfcore.NfcorePipelineObserver")
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>()
        listAppender.start()
        logger.addAppender(listAppender)
        def originalLevel = logger.level
        logger.setLevel(Level.TRACE)

        when:
        observer.onFlowComplete()

        then:
        def logEvents = listAppender.list
        logEvents.any { it.level == Level.TRACE && it.message.contains('Pipeline complete! 👋') }

        cleanup:
        logger.setLevel(originalLevel)
        logger.detachAppender(listAppender)
    }

    def 'onFlowCreate should NOT log start message below TRACE level'() {
        given:
        def observer = new NfcorePipelineObserver()
        def session = Mock(nextflow.Session) {
            workflowMetadata >> [projectName: 'test-pipeline']
            config >> [:]
            profile >> 'standard'
            commandLine >> ''
        }

        // Set up logging capture with DEBUG level
        Logger logger = (Logger) LoggerFactory.getLogger("nextflow.nfcore.NfcorePipelineObserver")
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>()
        listAppender.start()
        logger.addAppender(listAppender)
        def originalLevel = logger.level
        logger.setLevel(Level.DEBUG)

        when:
        observer.onFlowCreate(session)

        then:
        def logEvents = listAppender.list
        !logEvents.any { it.message.contains('Pipeline is starting! 🚀') }

        cleanup:
        logger.setLevel(originalLevel)
        logger.detachAppender(listAppender)
    }

    def 'onFlowComplete should NOT log complete message below TRACE level'() {
        given:
        def observer = new NfcorePipelineObserver()

        // Set up logging capture with DEBUG level
        Logger logger = (Logger) LoggerFactory.getLogger("nextflow.nfcore.NfcorePipelineObserver")
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>()
        listAppender.start()
        logger.addAppender(listAppender)
        def originalLevel = logger.level
        logger.setLevel(Level.DEBUG)

        when:
        observer.onFlowComplete()

        then:
        def logEvents = listAppender.list
        !logEvents.any { it.message.contains('Pipeline complete! 👋') }

        cleanup:
        logger.setLevel(originalLevel)
        logger.detachAppender(listAppender)
    }
}
