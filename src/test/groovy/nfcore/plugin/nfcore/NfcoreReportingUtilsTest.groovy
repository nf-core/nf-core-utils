package nfcore.plugin.util

import nextflow.Session
import nextflow.config.Manifest
import spock.lang.Specification
import spock.lang.Ignore
import spock.lang.TempDir
import java.nio.file.Path
import org.yaml.snakeyaml.Yaml
import nfcore.plugin.nfcore.NfcoreReportingUtils

class NfcoreReportingUtilsTest extends Specification {
    @TempDir
    Path tempDir

    def 'paramsSummaryMultiqc generates valid YAML with summary'() {
        given:
        def summaryParams = [
            'Input/output options': [
                'input': 'data/*.csv',
                'outdir': './results'
            ],
            'Reference genome options': [
                'genome': 'GRCh38',
                'igenomes_base': '/path/to/igenomes'
            ]
        ]
        
        // Mock Nextflow session - will be replaced by PowerMock in the actual running test
        def manifest = Mock(Manifest) {
            getName() >> 'nf-core/testpipe'
        }
        
        def originalSession = nextflow.Nextflow.metaClass.static.getSession
        nextflow.Nextflow.metaClass.static.getSession = { ->
            return Mock(Session) {
                getManifest() >> manifest
            }
        }
        
        when:
        def result = NfcoreReportingUtils.paramsSummaryMultiqc(summaryParams)
        
        then:
        // Check YAML structure
        result.contains("id: 'nf-core-testpipe-summary'")
        result.contains("section_name: 'nf-core/testpipe Workflow Summary'")
        result.contains("section_href: 'https://github.com/nf-core/testpipe'")
        result.contains("plot_type: 'html'")
        
        // Check parameter groups are included
        result.contains("<p style=\"font-size:110%\"><b>Input/output options</b></p>")
        result.contains("<p style=\"font-size:110%\"><b>Reference genome options</b></p>")
        
        // Check parameters are included
        result.contains("<dt>input</dt><dd><samp>data/*.csv</samp></dd>")
        result.contains("<dt>outdir</dt><dd><samp>./results</samp></dd>")
        result.contains("<dt>genome</dt><dd><samp>GRCh38</samp></dd>")
        result.contains("<dt>igenomes_base</dt><dd><samp>/path/to/igenomes</samp></dd>")
        
        cleanup:
        // Restore original method
        nextflow.Nextflow.metaClass.static.getSession = originalSession
    }
    
    def 'paramsSummaryMultiqc handles empty groups correctly'() {
        given:
        def summaryParams = [
            'Input options': [
                'input': 'data/*.csv'
            ],
            'Empty group': [:]
        ]
        
        // Mock Nextflow session
        def manifest = Mock(Manifest) {
            getName() >> 'nf-core/testpipe'
        }
        
        def originalSession = nextflow.Nextflow.metaClass.static.getSession
        nextflow.Nextflow.metaClass.static.getSession = { ->
            return Mock(Session) {
                getManifest() >> manifest
            }
        }

        when:
        def result = NfcoreReportingUtils.paramsSummaryMultiqc(summaryParams)
        
        then:
        // Non-empty group is included
        result.contains("<p style=\"font-size:110%\"><b>Input options</b></p>")
        result.contains("<dt>input</dt><dd><samp>data/*.csv</samp></dd>")
        
        // Empty group should not be in the output
        !result.contains("<p style=\"font-size:110%\"><b>Empty group</b></p>")
        
        cleanup:
        // Restore original method
        nextflow.Nextflow.metaClass.static.getSession = originalSession
    }
    
    def 'paramsSummaryMultiqc handles null values with N/A display'() {
        given:
        def summaryParams = [
            'Parameters': [
                'param1': 'value1',
                'param2': null
            ]
        ]
        
        // Mock Nextflow session
        def manifest = Mock(Manifest) {
            getName() >> 'nf-core/testpipe'
        }
        
        def originalSession = nextflow.Nextflow.metaClass.static.getSession
        nextflow.Nextflow.metaClass.static.getSession = { ->
            return Mock(Session) {
                getManifest() >> manifest
            }
        }
        
        when:
        def result = NfcoreReportingUtils.paramsSummaryMultiqc(summaryParams)

        then:
        // Normal parameter is included
        result.contains("<dt>param1</dt><dd><samp>value1</samp></dd>")
        
        // Null parameter shows N/A
        result.contains("<dt>param2</dt><dd><samp><span style=\"color:#999999;\">N/A</a></samp></dd>")
        
        cleanup:
        // Restore original method
        nextflow.Nextflow.metaClass.static.getSession = originalSession
    }
} 