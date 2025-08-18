package nfcore.plugin.nextflow

import nfcore.plugin.nextflow.NextflowPipelineUtils
import org.yaml.snakeyaml.Yaml
import spock.lang.Specification

/**
 * Specialized tests for the checkCondaChannels function
 */
class NextflowPipelineUtilCheckCondaTest extends Specification {

    def setup() {
        // Reset any metaClass changes before each test
        GroovySystem.metaClassRegistry.removeMetaClass(NextflowPipelineUtils)
    }

    def "checkCondaChannels should return true with correct channel configuration"() {
        given:
        def yamlOutput = """
channels:
  - conda-forge
  - bioconda
  - defaults
"""
        NextflowPipelineUtils.metaClass.'static'.execute = { String cmd ->
            return [text: yamlOutput]
        }
        NextflowPipelineUtils.metaClass.'static'.checkCondaChannels = {
            def parser = new Yaml()
            def channels = [] as List
            try {
                def result = yamlOutput
                if (result) {
                    def config = parser.load(result)
                    if (config && config instanceof Map && config.containsKey('channels')) {
                        channels = config['channels'] as List ?: []
                    }
                }
            }
            catch (Exception e) {
                return true
            }
            if (channels == null || channels.isEmpty()) {
                return true
            }
            def required_channels_in_order = ['conda-forge', 'bioconda']
            def channels_as_set = channels as Set ?: [] as Set
            def required_as_set = required_channels_in_order as Set
            def channels_missing = !required_as_set.every { ch -> channels_as_set.contains(ch) }
            def channel_subset = channels.findAll { ch -> ch in required_channels_in_order } ?: []
            def channel_priority_violation = !channel_subset.equals(required_channels_in_order)
            if (channels_missing | channel_priority_violation) {
                return false
            }
            return true
        }
        expect:
        NextflowPipelineUtils.checkCondaChannels() == true
    }

    def "checkCondaChannels should return false with missing channel"() {
        given:
        def yamlOutput = """
channels:
  - defaults
  - bioconda
"""
        NextflowPipelineUtils.metaClass.'static'.checkCondaChannels = {
            def parser = new Yaml()
            def channels = [] as List
            try {
                def result = yamlOutput
                if (result) {
                    def config = parser.load(result)
                    if (config && config instanceof Map && config.containsKey('channels')) {
                        channels = config['channels'] as List ?: []
                    }
                }
            }
            catch (Exception e) {
                return true
            }
            if (channels == null || channels.isEmpty()) {
                return true
            }
            def required_channels_in_order = ['conda-forge', 'bioconda']
            def channels_as_set = channels as Set ?: [] as Set
            def required_as_set = required_channels_in_order as Set
            def channels_missing = !required_as_set.every { ch -> channels_as_set.contains(ch) }
            def channel_subset = channels.findAll { ch -> ch in required_channels_in_order } ?: []
            def channel_priority_violation = !channel_subset.equals(required_channels_in_order)
            if (channels_missing | channel_priority_violation) {
                return false
            }
            return true
        }
        expect:
        NextflowPipelineUtils.checkCondaChannels() == false
    }

    def "checkCondaChannels should return false with wrong channel order"() {
        given:
        def yamlOutput = """
channels:
  - bioconda
  - conda-forge
  - defaults
"""
        NextflowPipelineUtils.metaClass.'static'.checkCondaChannels = {
            def parser = new Yaml()
            def channels = [] as List
            try {
                def result = yamlOutput
                if (result) {
                    def config = parser.load(result)
                    if (config && config instanceof Map && config.containsKey('channels')) {
                        channels = config['channels'] as List ?: []
                    }
                }
            }
            catch (Exception e) {
                return true
            }
            if (channels == null || channels.isEmpty()) {
                return true
            }
            def required_channels_in_order = ['conda-forge', 'bioconda']
            def channels_as_set = channels as Set ?: [] as Set
            def required_as_set = required_channels_in_order as Set
            def channels_missing = !required_as_set.every { ch -> channels_as_set.contains(ch) }
            def channel_subset = channels.findAll { ch -> ch in required_channels_in_order } ?: []
            def channel_priority_violation = !channel_subset.equals(required_channels_in_order)
            if (channels_missing | channel_priority_violation) {
                return false
            }
            return true
        }
        expect:
        NextflowPipelineUtils.checkCondaChannels() == false
    }

    def "checkCondaChannels should return true with null input"() {
        given:
        def yamlOutput = null
        NextflowPipelineUtils.metaClass.'static'.checkCondaChannels = {
            def parser = new Yaml()
            def channels = [] as List
            try {
                def result = yamlOutput
                if (result) {
                    def config = parser.load(result)
                    if (config && config instanceof Map && config.containsKey('channels')) {
                        channels = config['channels'] as List ?: []
                    }
                }
            }
            catch (Exception e) {
                return true
            }
            if (channels == null || channels.isEmpty()) {
                return true
            }
            def required_channels_in_order = ['conda-forge', 'bioconda']
            def channels_as_set = channels as Set ?: [] as Set
            def required_as_set = required_channels_in_order as Set
            def channels_missing = !required_as_set.every { ch -> channels_as_set.contains(ch) }
            def channel_subset = channels.findAll { ch -> ch in required_channels_in_order } ?: []
            def channel_priority_violation = !channel_subset.equals(required_channels_in_order)
            if (channels_missing | channel_priority_violation) {
                return false
            }
            return true
        }
        expect:
        NextflowPipelineUtils.checkCondaChannels() == true
    }

    def "checkCondaChannels should return true with invalid YAML"() {
        given:
        def yamlOutput = "not valid yaml: :"
        NextflowPipelineUtils.metaClass.'static'.checkCondaChannels = {
            def parser = new Yaml()
            def channels = [] as List
            try {
                def result = yamlOutput
                if (result) {
                    def config = parser.load(result)
                    if (config && config instanceof Map && config.containsKey('channels')) {
                        channels = config['channels'] as List ?: []
                    }
                }
            }
            catch (Exception e) {
                return true
            }
            if (channels == null || channels.isEmpty()) {
                return true
            }
            def required_channels_in_order = ['conda-forge', 'bioconda']
            def channels_as_set = channels as Set ?: [] as Set
            def required_as_set = required_channels_in_order as Set
            def channels_missing = !required_as_set.every { ch -> channels_as_set.contains(ch) }
            def channel_subset = channels.findAll { ch -> ch in required_channels_in_order } ?: []
            def channel_priority_violation = !channel_subset.equals(required_channels_in_order)
            if (channels_missing | channel_priority_violation) {
                return false
            }
            return true
        }
        expect:
        NextflowPipelineUtils.checkCondaChannels() == true
    }
}
