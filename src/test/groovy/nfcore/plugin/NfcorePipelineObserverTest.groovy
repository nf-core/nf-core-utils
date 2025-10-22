package nfcore.plugin

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import nfcore.plugin.nfcore.NfcoreConfigValidator
import org.slf4j.LoggerFactory
import spock.lang.PendingFeature
import spock.lang.Specification

class NfcorePipelineObserverTest extends Specification {
    // This file is now reserved for observer-specific logic tests.
    // All config, version, and factory tests have been moved to dedicated files.

    @PendingFeature()
    def 'onFlowCreate should call configValidator methods with correct arguments'() {
        given:
        def validator = Spy(NfcoreConfigValidator)
        def observer = new NfcorePipelineObserver(validator)
        def session = Mock(nextflow.Session) {
            workflowMetadata >> [projectName: 'test-pipeline']
            config >> [profile: 'standard', configFiles: ['main.config']]
            profile >> 'standard'
            commandLine >> '--foo bar'
        }

        when:
        observer.onFlowCreate(session)

        then:
        1 * validator.checkConfigProvided('test-pipeline', { it.profile == 'standard' && it.configFiles == ['main.config'] })
        1 * validator.checkProfileProvided('standard', '--foo bar', true)
    }

    @PendingFeature()
    def 'onFlowCreate handles missing metadata gracefully'() {
        given:
        def validator = Spy(NfcoreConfigValidator)
        def observer = new NfcorePipelineObserver(validator)
        def session = Mock(nextflow.Session) {
            workflowMetadata >> null
            config >> [profile: 'standard', configFiles: []]
            profile >> 'standard'
            commandLine >> ''
        }

        when:
        observer.onFlowCreate(session)

        then:
        1 * validator.checkConfigProvided(null, { it.profile == 'standard' && it.configFiles == [] })
        1 * validator.checkProfileProvided('standard', '', true)
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
        Logger logger = (Logger) LoggerFactory.getLogger(NfcorePipelineObserver)
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
        Logger logger = (Logger) LoggerFactory.getLogger(NfcorePipelineObserver)
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
        Logger logger = (Logger) LoggerFactory.getLogger(NfcorePipelineObserver)
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
        Logger logger = (Logger) LoggerFactory.getLogger(NfcorePipelineObserver)
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
