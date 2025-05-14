package nfcore.plugin

import nextflow.Session
import spock.lang.Specification
import spock.util.concurrent.BlockingVariable
import org.slf4j.LoggerFactory
import org.slf4j.Logger

class NfcorePipelineObserverTest extends Specification {

    def 'should create the observer instance' () {
        given:
        def factory = new NfcorePipelineObserverFactory()
        when:
        def result = factory.create(Mock(Session))
        then:
        result.size() == 1
        result.first() instanceof NfcorePipelineObserver
    }

    def 'should log warning if profile is standard and configFiles is empty or size 1'() {
        given:
        def observer = new NfcorePipelineObserver()
        def projectName = 'test-pipeline'
        def config = [ profile: 'standard', configFiles: configFiles ]
        // Capture logs
        def appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>()
        def logger = LoggerFactory.getLogger(NfcorePipelineObserver)
        if (logger instanceof ch.qos.logback.classic.Logger) {
            logger.addAppender(appender)
            appender.start()
        }

        when:
        def valid = observer.checkConfigProvided(projectName, config)

        then:
        def logs = appender.list.collect { it.formattedMessage }
        if (configFiles.size() <= 1) {
            assert logs.any { it.contains('You are attempting to run the pipeline without any custom configuration!') }
            assert !valid
        } else {
            assert logs.every { !it.contains('You are attempting to run the pipeline without any custom configuration!') }
            assert valid
        }

        cleanup:
        if (logger instanceof ch.qos.logback.classic.Logger) {
            logger.detachAppender(appender)
        }

        where:
        configFiles << [[], ['main.config'], ['main.config', 'custom.config']]
    }
} 