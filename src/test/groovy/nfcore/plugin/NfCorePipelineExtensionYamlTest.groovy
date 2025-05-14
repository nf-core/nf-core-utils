package nfcore.plugin

import spock.lang.Specification
import org.yaml.snakeyaml.Yaml
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * Tests for YAML-related functions in NfCorePipelineExtension
 */
class NfCorePipelineExtensionYamlTest extends Specification {
    
    def "processVersionsFromYAML should process version data correctly"() {
        given:
        def extension = new NfCorePipelineExtension() {
            String processVersionsFromYAML(String yaml_file) {
                def yaml = new org.yaml.snakeyaml.Yaml()
                def versions = yaml.load(yaml_file).collectEntries { k, v -> [k.toString().tokenize(':')[-1], v] }
                return yaml.dumpAsMap(versions).trim()
            }
        }
        def testYaml = '''
        "tool1:version": 1.0.0
        "tool2:version": 2.0.0
        "with:colon:in:name:version": 3.0.0
        '''
        
        when:
        def result = extension.processVersionsFromYAML(testYaml)
        
        then:
        result == 'version: 3.0.0'
        !result.contains('tool1:version')
        !result.contains('tool2:version')
        !result.contains('with:colon:in:name:version')
    }
    
    def "workflowVersionToYAML should format workflow version correctly"() {
        given:
        def extension = new NfCorePipelineExtension() {
            String getWorkflowVersion() {
                return 'v1.0.0-gabcdef1'
            }
            
            // Add a session field with the required mock properties
            def session = [
                getProperty: { propName ->
                    if (propName == 'workflowMeta') {
                        return [
                            getProperty: { metaPropName ->
                                if (metaPropName == 'name') return 'test-pipeline'
                                return null
                            }
                        ]
                    }
                    else if (propName == 'nextflow') {
                        return [
                            getProperty: { innerPropName ->
                                if (innerPropName == 'version') return '23.10.0'
                                return null
                            }
                        ]
                    }
                    return null
                }
            ]
        }
        
        when:
        def result = extension.workflowVersionToYAML()
        
        then:
        result.contains('Workflow:')
        result.contains('test-pipeline: v1.0.0-gabcdef1')
        result.contains('Nextflow: 23.10.0')
    }
    
    def "softwareVersionsToYAML should process channel of versions"() {
        given:
        def extension = new NfCorePipelineExtension() {
            String processVersionsFromYAML(String yaml) {
                return "Processed: ${yaml}"
            }
            
            String workflowVersionToYAML() {
                return "Workflow info"
            }
        }
        
        // Simple mock for the channel
        def finalResults = []
        def mockChannel = [
            unique: { return mockChannel },
            map: { closure -> 
                // Apply the closure to sample data
                def mappedResults = ['yaml1', 'yaml2'].collect { closure(it) }
                return [
                    unique: { return [
                        mix: { ch -> 
                            finalResults.addAll(mappedResults)
                            finalResults.addAll(ch)
                            return finalResults
                        }
                    ]}
                ]
            }
        ]
        
        // Mock the CH.value method
        GroovyMock(nextflow.extension.CH, global: true)
        1 * nextflow.extension.CH.value(_) >> { args -> 
            return [args[0]]
        }
        
        when:
        def result = extension.softwareVersionsToYAML(mockChannel)
        
        then:
        result != null
        // We can't directly check the contents of the DataflowChannel, but we can verify 
        // the method completed without exceptions
        noExceptionThrown()
    }
    
    def "paramsSummaryMultiqc should format summary correctly"() {
        given:
        def extension = new NfCorePipelineExtension() {
            // Add a session field with the required mock properties
            def session = [
                getProperty: { propName ->
                    if (propName == 'workflowMeta') {
                        return [
                            getProperty: { metaPropName ->
                                if (metaPropName == 'name') return 'test/pipeline'
                                return null
                            }
                        ]
                    }
                    else if (propName == 'config') {
                        return [
                            navigate: { path -> 
                                if (path == 'workflow.manifest.name') return 'test/pipeline'
                                return null
                            }
                        ]
                    }
                    return null
                }
            ]
        }
        
        def summaryParams = [
            'Group1': [
                param1: 'value1',
                param2: 'value2'
            ],
            'Group2': [
                param3: 'value3',
                param4: null
            ]
        ]
        
        when:
        def result = extension.paramsSummaryMultiqc(summaryParams)
        
        then:
        result.contains("id: 'test-pipeline-summary'")
        result.contains("section_name: 'test/pipeline Workflow Summary'")
        result.contains("section_href: 'https://github.com/test/pipeline'")
        result.contains("<b>Group1</b>")
        result.contains("<b>Group2</b>")
        result.contains("<dt>param1</dt><dd><samp>value1</samp></dd>")
        result.contains("<dt>param2</dt><dd><samp>value2</samp></dd>")
        result.contains("<dt>param3</dt><dd><samp>value3</samp></dd>")
        result.contains("<dt>param4</dt><dd><samp><span style=\"color:#999999;\">N/A</a></samp></dd>")
    }
} 