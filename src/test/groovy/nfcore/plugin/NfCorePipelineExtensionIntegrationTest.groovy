package nfcore.plugin

import nextflow.Session
import spock.lang.Specification

/**
 * Integration tests for NfCorePipelineExtension that capture the workflow test logic
 * from the original utils_nfcore_pipeline subworkflow
 */
class NfCorePipelineExtensionIntegrationTest extends Specification {
    
    /**
     * Custom extension class for integrated testing of workflow-like behavior
     */
    class WorkflowMockExtension extends NfCorePipelineExtension {
        Map workflowProfile = 'standard'
        List configFiles = [1] // One default config file
        Map workflowMeta = [
            name: 'test-pipeline',
            version: '1.0.0',
            commitId: 'abcdef1234567890'
        ]
        
        @Override
        public void init(Session session) {
            // Don't initialize with session in tests
        }
        
        /**
         * Simulate the original workflow behavior
         * This replicates the logic from the UTILS_NFCORE_PIPELINE workflow
         */
        def runWorkflow(List nextflow_cli_args) {
            // The original workflow called these functions in sequence
            def valid_config = checkConfigProvided()
            checkProfileProvided(nextflow_cli_args)
            
            // Return what the workflow would emit
            return [valid_config: valid_config]
        }
    }
    
    def "Workflow should run without failures with empty cli args"() {
        given:
        def extension = new WorkflowMockExtension()
        
        when:
        def result = extension.runWorkflow([])
        
        then:
        // In the main.workflow.nf.test, it just asserted workflow.success
        // Here we verify the workflow logic completed without exceptions
        result != null
        result.valid_config == false // With standard profile and default config
    }
    
    def "Workflow should run without failures with cli args"() {
        given:
        def extension = new WorkflowMockExtension()
        
        when:
        def result = extension.runWorkflow(["positional_arg"])
        
        then:
        result != null
        result.valid_config == false
    }
    
    def "Workflow should run successfully with custom profile"() {
        given:
        def extension = new WorkflowMockExtension()
        extension.workflowProfile = 'docker'
        
        when:
        def result = extension.runWorkflow([])
        
        then:
        result != null
        result.valid_config == true
    }
    
    def "Workflow should handle failing profile validation"() {
        given:
        def extension = new WorkflowMockExtension()
        extension.workflowProfile = 'test,'
        
        when:
        extension.runWorkflow([])
        
        then:
        thrown(RuntimeException)
    }
    
    def "Workflow should run successfully with multiple config files"() {
        given:
        def extension = new WorkflowMockExtension()
        extension.configFiles = [1, 2] // Multiple config files
        
        when: 
        def result = extension.runWorkflow([])
        
        then:
        result != null
        result.valid_config == true
    }
} 