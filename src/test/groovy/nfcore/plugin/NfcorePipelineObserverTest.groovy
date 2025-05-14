package nfcore.plugin

import nextflow.Session
import nextflow.config.Manifest
import spock.lang.Specification
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

    def 'should check profile validity and positional arguments'() {
        given:
        def observer = new NfcorePipelineObserver()
        // Capture logs
        def appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>()
        def logger = LoggerFactory.getLogger(NfcorePipelineObserver)
        if (logger instanceof ch.qos.logback.classic.Logger) {
            logger.addAppender(appender)
            appender.start()
        }

        when:
        def thrownException = null
        try {
            // Join nextflowCliArgs into a string, as checkProfileProvided expects a command line string
            observer.checkProfileProvided(profile, nextflowCliArgs instanceof List ? nextflowCliArgs.join(' ') : nextflowCliArgs)
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
        profile           | nextflowCliArgs      || shouldThrow | shouldWarn
        'test,'           | []                   || true        | false
        'test'            | ['foo']              || false       | true
        'test'            | []                   || false       | false
        null              | ['bar']              || false       | true
        null              | []                   || false       | false
    }

    def 'getWorkflowVersion formats version with v prefix'() {
        given:
        def observer = new NfcorePipelineObserver()
        def manifest = Mock(Manifest) {
            getVersion() >> version
        }
        def session = Mock(Session) {
            getManifest() >> manifest
        }

        when:
        def result = observer.getWorkflowVersion(session)

        then:
        result == expected

        where:
        version     | expected
        '1.0.0'     | 'v1.0.0'
        'v2.1.0'    | 'v2.1.0'
        null        | ''
        '3.0.0'     | 'v3.0.0'
    }

    def 'processVersionsFromYAML should parse and flatten YAML keys'() {
        given:
        def observer = new NfcorePipelineObserver()
        def yamlString = """
        tool:foo: 1.0.0
        bar: 2.0.0
        """.stripIndent()

        when:
        def result = observer.processVersionsFromYAML(yamlString)

        then:
        result.contains('foo: 1.0.0')
        result.contains('bar: 2.0.0')
        !result.contains('tool:foo:')
    }

    def 'workflowVersionToYAML contains workflow details'() {
        given:
        def observer = new NfcorePipelineObserver()
        def manifest = Mock(Manifest) {
            getName() >> wfName
            getVersion() >> wfVersion
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: nfVersion]]
        }

        when:
        def result = observer.workflowVersionToYAML(session)
        
        then:
        result.contains(wfName ?: 'unknown')
        result.contains(nfVersion ?: 'unknown')
        if (wfVersion) {
            result.contains(wfVersion.startsWith('v') ? wfVersion : "v${wfVersion}")
        }

        where:
        wfName      | wfVersion | nfVersion
        'mypipe'    | '1.0.0'   | '23.04.1'
        null        | null      | null
    }

    def 'softwareVersionsToYAML combines unique YAMLs and workflow YAML'() {
        given:
        def observer = new NfcorePipelineObserver()
        def yaml1 = 'foo: 1.0.0\nbar: 2.0.0'
        def yaml2 = 'baz: 3.0.0\nfoo: 1.0.0'
        def chVersions = [yaml1, yaml2]
        def manifest = Mock(Manifest) {
            getName() >> 'pipe'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = observer.softwareVersionsToYAML(chVersions, session)

        then:
        result.contains('foo: 1.0.0')
        result.contains('bar: 2.0.0')
        result.contains('baz: 3.0.0')
        result.contains('Workflow:')
        result.contains('pipe: v1.0.0')
        result.contains('Nextflow: 23.04.1')
    }
} 