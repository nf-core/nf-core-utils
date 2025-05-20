package nfcore.plugin

import nextflow.Session
import spock.lang.Specification

class NfcorePipelineObserverFactoryTest extends Specification {
    def 'should create the observer instance'() {
        given:
        def factory = new NfcorePipelineObserverFactory()

        when:
        def result = factory.create(Mock(Session))

        then:
        result.size() == 1
        result.first() instanceof NfcorePipelineObserver
    }
} 