package nfcore.plugin.util

import nextflow.Session
import nextflow.config.Manifest
import spock.lang.Specification
import spock.lang.Ignore

class NfcorePipelineUtilsTest extends Specification {
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
        def result = NfcorePipelineUtils.paramsSummaryMultiqc(summaryParams)
        
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
        def result = NfcorePipelineUtils.paramsSummaryMultiqc(summaryParams)
        
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
        def result = NfcorePipelineUtils.paramsSummaryMultiqc(summaryParams)
        
        then:
        // Normal parameter is included
        result.contains("<dt>param1</dt><dd><samp>value1</samp></dd>")
        
        // Null parameter shows N/A
        result.contains("<dt>param2</dt><dd><samp><span style=\"color:#999999;\">N/A</a></samp></dd>")
        
        cleanup:
        // Restore original method
        nextflow.Nextflow.metaClass.static.getSession = originalSession
    }

    def 'toolBibliographyText returns expected bibliography HTML'() {
        when:
        def result = NfcorePipelineUtils.toolBibliographyText([:])

        then:
        result.contains('<li>Quinlan AR, Hall IM. BEDTools: a flexible suite of utilities for comparing genomic features.')
        result.contains('Langmead, B., Salzberg, S. Fast gapped-read alignment with Bowtie 2.')
        result.contains('Ewels P, Magnusson M, Lundin S, Käller M. MultiQC: summarize analysis results for multiple tools and samples in a single report.')
        result.contains('</li>')
    }

    @Ignore("FIXME")
    def 'methodsDescriptionText substitutes template variables and includes citations'() {
        given:
        // Minimal MultiQC methods YAML template with placeholders
        def yamlContent = '''
        <h2>Workflow: ${workflow.name}</h2>
        <div>${tool_citations}</div>
        <ul>${tool_bibliography}</ul>
        '''.stripIndent()
        File tempYaml = File.createTempFile('mqc_methods', '.yml')
        tempYaml.text = yamlContent
        def meta = [workflow: [name: 'nf-core/testpipe']]

        // Mock Nextflow session and manifest
        def manifest = Mock(Manifest) {
            getName() >> 'nf-core/testpipe'
            getVersion() >> '1.0.0'
        }
        def workflowMetadata = new groovy.util.Expando()
        workflowMetadata.toMap = { -> [name: 'nf-core/testpipe'] }
        def session = Mock(Session) {
            getManifest() >> manifest
            getWorkflowMetadata() >> workflowMetadata
        }
        def originalSession = nextflow.Nextflow.metaClass.static.getSession
        nextflow.Nextflow.metaClass.static.getSession = { -> session }

        when:
        def result = NfcorePipelineUtils.methodsDescriptionText(tempYaml, meta)

        then:
        result.contains('<h2>Workflow: nf-core/testpipe</h2>')
        result.contains('BEDTools (Quinlan 2010)')
        result.contains('<li>Quinlan AR, Hall IM. BEDTools: a flexible suite of utilities for comparing genomic features.')

        cleanup:
        tempYaml.delete()
        nextflow.Nextflow.metaClass.static.getSession = originalSession
    }

    // TODO If we can't download the meta.yml file, we can't run this test
    // @IgnoreIf()
    def 'toolCitationText reads tools from meta.yml and formats citation string'() {
        // Download the latest meta.yml from the nf-core/modules repo
        given:
        def metaUrl = 'https://raw.githubusercontent.com/nf-core/modules/refs/heads/master/modules/nf-core/fastqc/meta.yml'
        File tempMeta = File.createTempFile('meta', '.yml')
        tempMeta.withOutputStream { out ->
            out << new URL(metaUrl).openStream()
        }

        when:
        def result = NfcorePipelineUtils.toolCitationText(tempMeta)

        then:
        result.startsWith('Tools used in the workflow included:')
        result.endsWith('.')
        // Optionally, check for a tool known to be in the latest meta.yml
        result.contains('fastqc')

        cleanup:
        tempMeta.delete()
    }
} 