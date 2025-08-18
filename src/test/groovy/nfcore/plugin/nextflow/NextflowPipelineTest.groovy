package nfcore.plugin.nextflow

import groovy.json.JsonSlurper
import nfcore.plugin.nextflow.NextflowPipelineUtils
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

/**
 * Tests for NextflowPipeline functions
 */
class NextflowPipelineTest extends Specification {

    @TempDir
    Path tempDir

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

        when:
        NextflowPipelineUtils.dumpParametersToJSON(outdir, params)

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

        when:
        NextflowPipelineUtils.dumpParametersToJSON(null, params)

        then:
        def files = tempDir.toFile().listFiles()
        files.length == 0 // No files should be created
    }

    @Ignore("TODO")
    def "checkCondaChannels should return true for correct config"() {
        given:
        // Mock the execute() method for String
        String.metaClass.execute = { ->
            [text: 'channels:\n  - conda-forge\n  - bioconda\n  - defaults\n'] as Process
        }

        when:
        def result = NextflowPipelineUtils.checkCondaChannels()
        println "DEBUG: checkCondaChannels() returned: ${result}"

        then:
        result == true

        cleanup:
        GroovySystem.metaClassRegistry.removeMetaClass(String)
    }

    @Ignore("TODO")
    def "checkCondaChannels should return false for wrong channel order"() {
        given:
        // Mock the execute() method for String
        String.metaClass.execute = { ->
            [text: '''
channels:
  - bioconda
  - conda-forge
  - defaults
'''] as Process
        }

        when:
        def result = NextflowPipelineUtils.checkCondaChannels()
        println "DEBUG: checkCondaChannels() returned: ${result} (wrong order)"

        then:
        result == false

        cleanup:
        GroovySystem.metaClassRegistry.removeMetaClass(String)
    }
} 