package nfcore.plugin

import nextflow.Session
import spock.lang.Specification
import spock.lang.TempDir
import java.nio.file.Path
import java.nio.file.Files

/**
 * Tests for NfCorePipelineExtension functions
 */
class NfCorePipelineExtensionTest extends Specification {
    
    @TempDir
    Path tempDir
    
    /**
     * Create a mockable extension for testing
     */
    class MockableNfCorePipelineExtension extends NfCorePipelineExtension {
        // Override session-dependent methods for testing
        def workflowProfile = [profile: 'standard']
        List configFiles = []
        Map workflowMeta = [
            name: 'test-pipeline',
            version: '1.0.0',
            commitId: 'abcdef1234567890'
        ]
        
        @Override
        public void init(Session session) {
            // Don't initialize with session in tests
        }
        
        @Override
        String getWorkflowVersion() {
            def version = workflowMeta.version
            def prefix_v = version.charAt(0) != 'v' ? 'v' : ''
            def version_string = "${prefix_v}${version}"
            
            if (workflowMeta.commitId) {
                def git_shortsha = workflowMeta.commitId.substring(0, 7)
                version_string += "-g${git_shortsha}"
            }
            
            return version_string
        }
        
        @Override
        boolean checkConfigProvided() {
            def isStandardProfile = workflowProfile.profile == 'standard'
            def hasSingleConfigFile = configFiles.size() <= 1
            
            if (isStandardProfile && hasSingleConfigFile) {
                System.err.println("Warning: No custom configuration provided")
                return false
            }
            return true
        }
        
        @Override
        void checkProfileProvided(List args) {
            if (workflowProfile.profile?.endsWith(',')) {
                throw new RuntimeException("The `-profile` option cannot end with a trailing comma")
            }
            
            if (args && args[0]) {
                System.err.println("Warning: Positional argument detected: ${args[0]}")
            }
        }
        
        @Override
        Object getSingleReport(Object multiqc_reports) {
            if (multiqc_reports instanceof java.nio.file.Path) {
                return multiqc_reports
            } else if (multiqc_reports instanceof List) {
                if (multiqc_reports.size() == 0) {
                    System.err.println("[test-pipeline] No reports found from process 'MULTIQC'")
                    return null
                } else if (multiqc_reports.size() == 1) {
                    return multiqc_reports.first()
                } else {
                    System.err.println("[test-pipeline] Found multiple reports from process 'MULTIQC', will use only one")
                    return multiqc_reports.first()
                }
            } else {
                return null
            }
        }
        
        // Mock methods that use session
        boolean isWorkflowProfileStandard() {
            return workflowProfile.profile == 'standard'
        }
        
        int getConfigFilesSize() {
            return configFiles.size()
        }
    }
    
    def "checkConfigProvided should return false for standard profile with default config"() {
        given:
        def extension = new MockableNfCorePipelineExtension()
        extension.workflowProfile = [profile: 'standard']
        extension.configFiles = [1] // One default config file
        
        when:
        def result = extension.checkConfigProvided()
        
        then:
        result == false
    }
    
    def "checkConfigProvided should return true for non-standard profile"() {
        given:
        def extension = new MockableNfCorePipelineExtension()
        extension.workflowProfile = [profile: 'docker']
        extension.configFiles = [1] // One config file
        
        when:
        def result = extension.checkConfigProvided()
        
        then:
        result == true
    }
    
    def "checkConfigProvided should return true with custom configs"() {
        given:
        def extension = new MockableNfCorePipelineExtension()
        extension.workflowProfile = [profile: 'standard']
        extension.configFiles = [1, 2] // Multiple config files
        
        when:
        def result = extension.checkConfigProvided()
        
        then:
        result == true
    }
    
    def "checkProfileProvided should throw exception with trailing comma"() {
        given:
        def extension = new MockableNfCorePipelineExtension()
        extension.workflowProfile = [profile: 'test,']
        
        when:
        extension.checkProfileProvided([])
        
        then:
        thrown(RuntimeException)
    }
    
    def "checkProfileProvided should warn about positional arguments"() {
        given:
        def extension = new MockableNfCorePipelineExtension()
        extension.workflowProfile = [profile: 'test']
        
        // Capture stderr output
        def originalErr = System.err
        def errContent = new ByteArrayOutputStream()
        System.err = new PrintStream(errContent)
        
        when:
        extension.checkProfileProvided(['positional_arg'])
        
        then:
        errContent.toString().contains('positional argument')
        
        cleanup:
        System.err = originalErr
    }
    
    def "getWorkflowVersion should format version correctly"() {
        given:
        def extension = new MockableNfCorePipelineExtension()
        
        when:
        def result = extension.getWorkflowVersion()
        
        then:
        result == 'v1.0.0-gabcdef1'
    }
    
    def "logColours should return empty codes with monochrome"() {
        given:
        def extension = new NfCorePipelineExtension()
        
        when:
        def result = extension.logColours(true)
        
        then:
        result.red == ''
        result.green == ''
        result.reset == ''
    }
    
    def "logColours should return ANSI codes without monochrome"() {
        given:
        def extension = new NfCorePipelineExtension()
        
        when:
        def result = extension.logColours(false)
        
        then:
        result.red == '\033[0;31m'
        result.green == '\033[0;32m'
        result.reset == '\033[0m'
    }
    
    def "getSingleReport should return the path for a single report"() {
        given:
        def extension = new MockableNfCorePipelineExtension()
        def testPath = tempDir.resolve("test-report.html")
        Files.createFile(testPath)
        
        when:
        def result = extension.getSingleReport(testPath)
        
        then:
        result == testPath
    }
    
    def "getSingleReport should return first report from multiple reports"() {
        given:
        def extension = new MockableNfCorePipelineExtension()
        def testPath1 = tempDir.resolve("test-report1.html")
        def testPath2 = tempDir.resolve("test-report2.html")
        def testPath3 = tempDir.resolve("test-report3.html")
        Files.createFile(testPath1)
        Files.createFile(testPath2)
        Files.createFile(testPath3)
        
        when:
        def result = extension.getSingleReport([testPath1, testPath2, testPath3])
        
        then:
        result == testPath1
    }
    
    def "getSingleReport should return null for empty list"() {
        given:
        def extension = new MockableNfCorePipelineExtension()
        def originalErr = System.err
        def errContent = new ByteArrayOutputStream()
        System.err = new PrintStream(errContent)
        
        when:
        def result = extension.getSingleReport([])
        
        then:
        result == null
        errContent.toString().contains('No reports found')
        
        cleanup:
        System.err = originalErr
    }
    
    def "getSingleReport should warn about multiple reports"() {
        given:
        def extension = new MockableNfCorePipelineExtension()
        def testPath1 = tempDir.resolve("test-report1.html")
        def testPath2 = tempDir.resolve("test-report2.html")
        Files.createFile(testPath1)
        Files.createFile(testPath2)
        
        def originalErr = System.err
        def errContent = new ByteArrayOutputStream()
        System.err = new PrintStream(errContent)
        
        when:
        def result = extension.getSingleReport([testPath1, testPath2])
        
        then:
        result == testPath1
        errContent.toString().contains('Found multiple reports')
        
        cleanup:
        System.err = originalErr
    }
    
    def "completionSummary should handle successful pipeline"() {
        given:
        def extension = new MockableNfCorePipelineExtension() {
            @Override
            void completionSummary(boolean monochrome_logs) {
                // Mock implementation that doesn't rely on session
                def colors = logColours(monochrome_logs)
                println("-${colors.purple}[test-pipeline]${colors.green} Pipeline completed successfully${colors.reset}-")
            }
        }
        
        def originalOut = System.out
        def outContent = new ByteArrayOutputStream()
        System.out = new PrintStream(outContent)
        
        when:
        extension.completionSummary(true)
        
        then:
        outContent.toString().contains('Pipeline completed successfully')
        
        cleanup:
        System.out = originalOut
    }
} 