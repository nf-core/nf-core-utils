package nfcore.plugin

import nfcore.plugin.nfcore.NfcoreConfigValidator
import org.slf4j.LoggerFactory
import spock.lang.Specification

class NfcoreConfigValidatorTest extends Specification {
    def 'should log warning if profile is standard and configFiles is empty or size 1'() {
        given:
        def validator = new NfcoreConfigValidator()
        def projectName = 'test-pipeline'
        def config = [profile: 'standard', configFiles: configFiles]
        // Capture logs
        def appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>()
        def logger = LoggerFactory.getLogger(NfcoreConfigValidator)
        if (logger instanceof ch.qos.logback.classic.Logger) {
            logger.addAppender(appender)
            appender.start()
        }

        when:
        def valid = validator.checkConfigProvided(projectName, config)

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

    def 'should check profile validity and positional arguments'() {
        given:
        def validator = new NfcoreConfigValidator()
        // Capture logs
        def appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>()
        def logger = LoggerFactory.getLogger(NfcoreConfigValidator)
        if (logger instanceof ch.qos.logback.classic.Logger) {
            logger.addAppender(appender)
            appender.start()
        }

        when:
        def thrownException = null
        try {
            // Join nextflowCliArgs into a string, as checkProfileProvided expects a command line string
            validator.checkProfileProvided(profile, nextflowCliArgs instanceof List ? nextflowCliArgs.join(' ') : nextflowCliArgs, true)
        } catch (Exception e) {
            thrownException = e
        }
        def logs = appender.list.collect { it.formattedMessage }

        then:
        if (shouldThrow) {
            assert thrownException instanceof IllegalArgumentException
            assert thrownException.message.contains('cannot end with a trailing comma')
        } else if (shouldWarn) {
            assert logs.any { it.contains('nf-core pipelines do not accept positional arguments') }
        } else {
            assert !thrownException
            assert logs.every { !it.contains('nf-core pipelines do not accept positional arguments') }
        }

        cleanup:
        if (logger instanceof ch.qos.logback.classic.Logger) {
            logger.detachAppender(appender)
        }

        where:
        profile | nextflowCliArgs || shouldThrow | shouldWarn
        'test,' | []              || true        | false
        'test'  | ['foo']         || false       | true
        'test'  | []              || false       | false
        null    | ['bar']         || false       | true
        null    | []              || false       | false
    }

    def 'should add colors to profile error message when monochrome is disabled'() {
        given:
        def validator = new NfcoreConfigValidator()

        when:
        def thrownException = null
        try {
            validator.checkProfileProvided('test,', '', false)  // monochromeLogs = false
        } catch (Exception e) {
            thrownException = e
        }

        then:
        assert thrownException instanceof IllegalArgumentException
        def message = thrownException.message
        assert message.contains('\033[0;31m')  // red color code
        assert message.contains('\033[0;33m')  // yellow color code
        assert message.contains('\033[0m')     // reset color code
        assert message.contains('ERROR')
        assert message.contains('HINT')
    }

    def 'should not add colors to profile error message when monochrome is enabled'() {
        given:
        def validator = new NfcoreConfigValidator()

        when:
        def thrownException = null
        try {
            validator.checkProfileProvided('test,', '', true)  // monochromeLogs = true
        } catch (Exception e) {
            thrownException = e
        }

        then:
        assert thrownException instanceof IllegalArgumentException
        def message = thrownException.message
        assert !message.contains('\033[')  // no color codes
        assert message.contains('ERROR')
        assert message.contains('HINT')
    }
} 