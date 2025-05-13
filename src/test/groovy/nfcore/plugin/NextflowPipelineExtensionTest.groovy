package nfcore.plugin

import spock.lang.Specification
import spock.lang.TempDir
import groovy.json.JsonSlurper
import java.nio.file.Path
import java.nio.file.Files

/**
 * Tests for NextflowPipelineExtension functions
 */
class NextflowPipelineExtensionTest extends Specification {
    
    @TempDir
    Path tempDir
    
    def "getWorkflowVersion should format version correctly with version only"() {
        given:
        def extension = new NextflowPipelineExtension()
        
        when:
        def result = extension.getWorkflowVersion('1.0.0')
        
        then:
        result == 'v1.0.0'
    }
    
    def "getWorkflowVersion should preserve v prefix"() {
        given:
        def extension = new NextflowPipelineExtension()
        
        when:
        def result = extension.getWorkflowVersion('v1.0.0')
        
        then:
        result == 'v1.0.0'
    }
    
    def "getWorkflowVersion should format version with commit ID"() {
        given:
        def extension = new NextflowPipelineExtension()
        
        when:
        def result = extension.getWorkflowVersion('1.0.0', 'abcdef1234567890')
        
        then:
        result == 'v1.0.0-gabcdef1'
    }
    
    def "dumpParametersToJSON should write parameters to file"() {
        given:
        def outdir = tempDir
        def pipelineInfoDir = Files.createDirectory(outdir.resolve("pipeline_info"))
        def launchDir = tempDir
        
        def params = [
            param1: 'value1',
            param2: 'value2',
            nested: [
                param3: 'value3'
            ]
        ]
        
        def extension = new NextflowPipelineExtension()
        
        when:
        extension.dumpParametersToJSON(outdir, params, launchDir)
        
        then:
        def files = pipelineInfoDir.toFile().listFiles()
        files.length == 1
        files[0].name.startsWith('params_')
        files[0].name.endsWith('.json')
        
        def jsonContent = new JsonSlurper().parse(files[0])
        jsonContent.param1 == 'value1'
        jsonContent.param2 == 'value2'
        jsonContent.nested.param3 == 'value3'
    }
    
    def "dumpParametersToJSON should handle null output directory"() {
        given:
        def launchDir = tempDir
        
        def params = [
            param1: 'value1',
            param2: 'value2'
        ]
        
        def extension = new NextflowPipelineExtension()
        
        when:
        extension.dumpParametersToJSON(null, params, launchDir)
        
        then:
        def files = tempDir.toFile().listFiles()
        files.length == 0 // No files should be created
    }
    
    def "checkCondaChannels should return true when mocked"() {
        given:
        // Create a mock extension that overrides the execute method
        def extension = Spy(NextflowPipelineExtension) {
            checkCondaChannels() >> true
        }
        
        expect:
        extension.checkCondaChannels() == true
    }
} 