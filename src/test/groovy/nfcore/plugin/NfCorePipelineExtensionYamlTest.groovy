package nfcore.plugin

import spock.lang.Specification
import org.yaml.snakeyaml.Yaml

/**
 * Tests for YAML-related functions in NfCorePipelineExtension
 */
class NfCorePipelineExtensionYamlTest extends Specification {
    
    def "processVersionsFromYAML should process version data correctly"() {
        given:
        def extension = new NfCorePipelineExtension()
        def testYaml = '''
        tool1:version: 1.0.0
        tool2:version: 2.0.0
        with:colon:in:name:version: 3.0.0
        '''
        
        when:
        def result = extension.processVersionsFromYAML(testYaml)
        
        then:
        result.contains('version: 1.0.0')
        result.contains('version: 2.0.0')
        result.contains('version: 3.0.0')
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
            
            // Mock relevant session methods
            def getSession() {
                return [
                    workflowMeta: [
                        name: 'test-pipeline'
                    ],
                    nextflow: [
                        version: '23.10.0'
                    ]
                ]
            }
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
        
        // Create a mock channel with map method - simplified for testing
        def mockChannel = [
            unique: { return mockChannel },
            map: { closure -> 
                def result = []
                ['yaml1', 'yaml2'].each { result << closure(it) }
                return [
                    unique: { return result },
                    mix: { ch -> return result + ch }
                ]
            }
        ]
        
        // Mock CH.value
        def originalCH = nextflow.extension.CH
        nextflow.extension.CH = [
            value: { val -> return [val] }
        ]
        
        when:
        def result = extension.softwareVersionsToYAML(mockChannel)
        
        then:
        result.contains('Processed: yaml1')
        result.contains('Processed: yaml2')
        result.contains('Workflow info')
        
        cleanup:
        nextflow.extension.CH = originalCH
    }
    
    def "paramsSummaryMultiqc should format summary correctly"() {
        given:
        def extension = new NfCorePipelineExtension() {
            // Mock relevant session methods
            def getSession() {
                return [
                    workflowMeta: [
                        name: 'test/pipeline'
                    ]
                ]
            }
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