package nfcore.plugin.nfcore

import spock.lang.Specification
import spock.lang.TempDir
import java.nio.file.Path

class NfcoreNotificationUtilsTest extends Specification {

    @TempDir
    Path tempDir

    def "logColours returns correct color codes"() {
        when:
        def colors = NfcoreNotificationUtils.logColours(false)
        
        then:
        colors['reset'] == "\033[0m"
        colors['green'] == "\033[0;32m"
        colors['red'] == "\033[0;31m"
    }

    def "logColours returns empty strings for monochrome"() {
        when:
        def colors = NfcoreNotificationUtils.logColours(true)
        
        then:
        colors['reset'] == ''
        colors['green'] == ''
        colors['red'] == ''
    }


    def "processSummaryParams processes nested maps correctly"() {
        given:
        def summaryParams = [
            "group1": ["param1": "value1", "param2": "value2"],
            "group2": ["param3": "value3"]
        ]
        
        when:
        def result = NfcoreNotificationUtils.processSummaryParams(summaryParams)
        
        then:
        result.size() == 3
        result["param1"] == "value1" 
        result["param2"] == "value2"
        result["param3"] == "value3"
    }

    def "processSummaryParams handles null input"() {
        when:
        def result = NfcoreNotificationUtils.processSummaryParams(null)
        
        then:
        result.isEmpty()
    }

    def "processSummaryParams handles empty map"() {
        when:
        def result = NfcoreNotificationUtils.processSummaryParams([:])
        
        then:
        result.isEmpty()
    }

    def "processSummaryParams ignores non-map values"() {
        given:
        def summaryParams = [
            "group1": ["param1": "value1"],
            "group2": "not a map",
            "group3": ["param2": "value2"]
        ]
        
        when:
        def result = NfcoreNotificationUtils.processSummaryParams(summaryParams)
        
        then:
        result.size() == 2
        result["param1"] == "value1"
        result["param2"] == "value2"
        !result.containsKey("group2")
    }
}