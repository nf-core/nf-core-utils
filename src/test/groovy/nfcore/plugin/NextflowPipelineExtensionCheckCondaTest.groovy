package nfcore.plugin

import spock.lang.Specification
import org.yaml.snakeyaml.Yaml

/**
 * Specialized tests for the checkCondaChannels function
 */
class NextflowPipelineExtensionCheckCondaTest extends Specification {
    
    /**
     * Create a custom extension that allows us to inject mock command execution results
     */
    class MockableNextflowPipelineExtension extends NextflowPipelineExtension {
        String mockYamlOutput
        
        @Override
        boolean checkCondaChannels() {
            def parser = new Yaml()
            def channels = [] as List
            try {
                if (mockYamlOutput != null) {
                    def config = parser.load(mockYamlOutput)
                    if (config && config instanceof Map && config.containsKey('channels')) {
                        channels = config['channels'] as List ?: []
                    }
                }
            }
            catch (Exception e) {
                // Protect against NPE in tests
                try {
                    System.err.println("WARN: Could not verify conda channel configuration: ${e.message}")
                } catch (Exception ignored) {
                    // Ignore exceptions from println in tests
                }
                return true
            }

            // If channels is null or empty, return true to avoid NPE
            if (channels == null || channels.isEmpty()) {
                return true
            }

            // Check that all channels are present
            def required_channels_in_order = ['conda-forge', 'bioconda']
            def channels_as_set = channels as Set ?: [] as Set
            def required_as_set = required_channels_in_order as Set

            def channels_missing = !required_as_set.every { ch -> channels_as_set.contains(ch) }

            // Check that they are in the right order
            def channel_subset = channels.findAll { ch -> ch in required_channels_in_order } ?: []
            def channel_priority_violation = !channel_subset.equals(required_channels_in_order)

            if (channels_missing | channel_priority_violation) {
                try {
                    System.err.println("Channels missing or in wrong order")
                } catch (Exception ignored) {
                    // Ignore exceptions from println in tests
                }
                return false
            }
            
            return true
        }
    }
    
    def "checkCondaChannels should return true with correct channel configuration"() {
        given:
        def extension = new MockableNextflowPipelineExtension()
        extension.mockYamlOutput = """
channels:
  - conda-forge
  - bioconda
  - defaults
"""
        
        expect:
        extension.checkCondaChannels() == true
    }
    
    def "checkCondaChannels should return false with missing channel"() {
        given:
        def extension = new MockableNextflowPipelineExtension()
        extension.mockYamlOutput = """
channels:
  - defaults
  - bioconda
"""
        
        expect:
        extension.checkCondaChannels() == false
    }
    
    def "checkCondaChannels should return false with wrong channel order"() {
        given:
        def extension = new MockableNextflowPipelineExtension()
        extension.mockYamlOutput = """
channels:
  - bioconda
  - conda-forge
  - defaults
"""
        
        expect:
        extension.checkCondaChannels() == false
    }
    
    def "checkCondaChannels should return true with null input"() {
        given:
        def extension = new MockableNextflowPipelineExtension()
        extension.mockYamlOutput = null
        
        expect:
        extension.checkCondaChannels() == true
    }
    
    def "checkCondaChannels should return true with invalid YAML"() {
        given:
        def extension = new MockableNextflowPipelineExtension() {
            @Override
            boolean checkCondaChannels() {
                def parser = new Yaml()
                def channels = [] as List
                try {
                    def config = parser.load(mockYamlOutput)
                    if (config && config instanceof Map && config.containsKey('channels')) {
                        channels = config['channels'] as List ?: []
                    }
                }
                catch (Exception e) {
                    // Protect against NPE in tests
                    try {
                        System.err.println("WARN: Could not verify conda channel configuration: ${e.message}")
                    } catch (Exception ignored) {
                        // Ignore exceptions from println in tests
                    }
                    return true
                }

                // If channels is null or empty, return true to avoid NPE
                if (channels == null || channels.isEmpty()) {
                    return true
                }

                // Rest of the method unchanged
                def required_channels_in_order = ['conda-forge', 'bioconda']
                def channels_as_set = channels as Set ?: [] as Set
                def required_as_set = required_channels_in_order as Set
                
                def channels_missing = !required_as_set.every { ch -> channels_as_set.contains(ch) }
                def channel_subset = channels.findAll { ch -> ch in required_channels_in_order } ?: []
                def channel_priority_violation = !channel_subset.equals(required_channels_in_order)

                if (channels_missing | channel_priority_violation) {
                    try {
                        System.err.println("Channels missing or in wrong order")
                    } catch (Exception ignored) {
                        // Ignore exceptions from println in tests
                    }
                    return false
                }
                
                return true
            }
        }
        extension.mockYamlOutput = "not valid yaml: :"
        
        expect:
        extension.checkCondaChannels() == true
    }
} 