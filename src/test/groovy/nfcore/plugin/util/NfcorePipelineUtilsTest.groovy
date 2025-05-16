package nfcore.plugin.util

import nextflow.Session
import nextflow.config.Manifest
import spock.lang.Specification
import spock.lang.Ignore
import spock.lang.TempDir
import java.nio.file.Path
import org.yaml.snakeyaml.Yaml

class NfcorePipelineUtilsTest extends Specification {
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

    def 'toolBibliographyText returns expected bibliography HTML from meta.yml'() {
        given:
        // Download the latest meta.yml from the nf-core/modules repo
        def metaUrl = 'https://raw.githubusercontent.com/nf-core/modules/refs/heads/master/modules/nf-core/fastqc/meta.yml'
        File tempMeta = File.createTempFile('meta', '.yml')
        tempMeta.withOutputStream { out ->
            out << new URL(metaUrl).openStream()
        }

        when:
        def citations = NfcorePipelineUtils.generateModuleToolCitation(tempMeta)
        def result = NfcorePipelineUtils.toolBibliographyText(citations)

        then:
        result.contains('<li>')
        result.toLowerCase().contains('fastqc')
        result.toLowerCase().contains('bioinformatics')

        cleanup:
        tempMeta.delete()
    }

    def 'methodsDescriptionText substitutes template variables and includes citations from meta.yml'() {
        given:
        // Minimal MultiQC methods YAML template with placeholders
        def yamlContent = '''
        <h2>Workflow: ${workflow.name}</h2>
        <div>${tool_citations}</div>
        <ul>${tool_bibliography}</ul>
        '''.stripIndent()
        File tempYaml = File.createTempFile('mqc_methods', '.yml')
        tempYaml.text = yamlContent
        // Download the latest meta.yml from the nf-core/modules repo
        def metaUrl = 'https://raw.githubusercontent.com/nf-core/modules/refs/heads/master/modules/nf-core/fastqc/meta.yml'
        File tempMeta = File.createTempFile('meta', '.yml')
        tempMeta.withOutputStream { out ->
            out << new URL(metaUrl).openStream()
        }
        def meta = [workflow: [name: 'nf-core/testpipe']]

        // Mock Nextflow session and manifest
        def manifest = Mock(Manifest) {
            getName() >> 'nf-core/testpipe'
            getVersion() >> '1.0.0'
        }
        // Use a mock for WorkflowMetadata with a toMap() method
        def workflowMetadata = Mock(nextflow.script.WorkflowMetadata) {
            toMap() >> [name: 'nf-core/testpipe']
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getWorkflowMetadata() >> workflowMetadata
        }
        def originalSession = nextflow.Nextflow.metaClass.static.getSession
        nextflow.Nextflow.metaClass.static.getSession = { -> session }

        when:
        def citations = NfcorePipelineUtils.generateModuleToolCitation(tempMeta)
        if (!meta.containsKey("tool_citations")) {
            meta["tool_citations"] = NfcorePipelineUtils.toolCitationText(citations)
        }
        if (!meta.containsKey("tool_bibliography")) {
            meta["tool_bibliography"] = NfcorePipelineUtils.toolBibliographyText(citations)
        }
        def result = NfcorePipelineUtils.methodsDescriptionText(tempYaml, citations, meta)

        then:
        result.contains('<h2>Workflow: nf-core/testpipe</h2>')
        result.toLowerCase().contains('fastqc')
        result.contains('<li>')
        result.toLowerCase().contains('bioinformatics')

        cleanup:
        tempYaml.delete()
        tempMeta.delete()
        nextflow.Nextflow.metaClass.static.getSession = originalSession
    }

    def "generateModuleToolCitation should parse meta.yml and return tool citations"() {
        given:
        def metaFile = new File(tempDir.toFile(), "meta.yml")
        metaFile << """
        name: test_module
        tools:
          - samtools:
              doi: "10.1093/bioinformatics/btp352"
              homepage: "https://www.htslib.org/"
              author: "Li H, Handsaker B, Wysoker A, et al."
              year: 2009
              title: "The Sequence Alignment/Map format and SAMtools"
              journal: "Bioinformatics"
          - fastqc:
              description: "Quality control tool for high throughput sequence data"
        """

        when:
        def result = NfcorePipelineUtils.generateModuleToolCitation(metaFile)
        println "DEBUG: result from generateModuleToolCitation: ${result}"

        then:
        result.size() == 2
        result.containsKey("samtools")
        result.containsKey("fastqc")
        result.samtools.citation == "samtools (DOI: 10.1093/bioinformatics/btp352)"
        result.samtools.bibliography.contains("<li>Li H, Handsaker B, Wysoker A, et al.. 2009. The Sequence Alignment/Map format and SAMtools. Bioinformatics. doi: 10.1093/bioinformatics/btp352. <a href='https://www.htslib.org/'>https://www.htslib.org/</a></li>")
        result.fastqc.citation == "fastqc (Quality control tool for high throughput sequence data)"
        result.fastqc.bibliography == "<li>fastqc</li>"
    }

    def "toolCitationText should format citations from collected module citations"() {
        given:
        def collectedCitations = [
            'samtools': [
                citation: "samtools (DOI: 10.1093/bioinformatics/btp352)",
                bibliography: "<li>Citation 1</li>"
            ],
            'fastqc': [
                citation: "fastqc (Quality control tool)",
                bibliography: "<li>Citation 2</li>"
            ]
        ]

        when:
        def result = NfcorePipelineUtils.toolCitationText(collectedCitations)

        then:
        result == "Tools used in the workflow included: samtools (DOI: 10.1093/bioinformatics/btp352), fastqc (Quality control tool)."
    }

    def "toolBibliographyText should format bibliography from collected module citations"() {
        given:
        def collectedCitations = [
            'samtools': [
                citation: "samtools citation",
                bibliography: "<li>Samtools citation details</li>"
            ],
            'fastqc': [
                citation: "fastqc citation",
                bibliography: "<li>FastQC citation details</li>"
            ],
            'empty_bib': [
                citation: "tool with no bibliography",
                bibliography: null
            ]
        ]

        when:
        def result = NfcorePipelineUtils.toolBibliographyText(collectedCitations)

        then:
        result == "<li>Samtools citation details</li> <li>FastQC citation details</li>"
    }

    def "methodsDescriptionText should generate HTML with collected citations"() {
        given:
        def mqcMethodsFile = new File(tempDir.toFile(), "mqc_methods.yml")
        mqcMethodsFile << """
        id: 'methods-description'
        section_name: 'Test Pipeline Methods'
        section_href: 'https://example.com'
        plot_type: 'html'
        description: |
            This pipeline uses the following tools: ${tool_citations}
            
            <h4>Bibliography</h4>
            <ol>
            ${tool_bibliography}
            </ol>
        """

        def collectedCitations = [
            'samtools': [
                citation: "samtools (DOI: 10.1093/bioinformatics/btp352)",
                bibliography: "<li>Samtools citation details</li>"
            ],
            'fastqc': [
                citation: "fastqc (Quality control tool)",
                bibliography: "<li>FastQC citation details</li>"
            ]
        ]

        // Mock session for the test
        GroovyMock(Session, global: true)
        def mockSession = Mock(Session)
        def mockManifest = Mock() { toMap() >> [name: "test-workflow", doi: "10.5281/zenodo.123456"] }
        def mockWorkflowMetadata = Mock() { toMap() >> [projectName: "test_pipeline"] }
        mockSession.getManifest() >> mockManifest
        mockSession.getWorkflowMetadata() >> mockWorkflowMetadata
        def originalSession = nextflow.Nextflow.metaClass.static.getSession
        nextflow.Nextflow.metaClass.static.getSession = { -> mockSession }

        when:
        def meta = [:]
        meta["tool_citations"] = NfcorePipelineUtils.toolCitationText(collectedCitations)
        meta["tool_bibliography"] = NfcorePipelineUtils.toolBibliographyText(collectedCitations)
        def result
        try {
            result = NfcorePipelineUtils.methodsDescriptionText(mqcMethodsFile, collectedCitations, meta)
        } catch (Exception e) {
            println "DEBUG: Exception in methodsDescriptionText: ${e}"
            throw e
        }

        then:
        result.contains("samtools (DOI: 10.1093/bioinformatics/btp352)")
        result.contains("fastqc (Quality control tool)")
        result.contains("<li>Samtools citation details</li>")
        result.contains("<li>FastQC citation details</li>")

        cleanup:
        nextflow.Nextflow.metaClass.static.getSession = originalSession
    }

    def "should handle empty citations list"() {
        given:
        def emptyMap = [:]

        when:
        def citationText = NfcorePipelineUtils.toolCitationText(emptyMap)
        def bibText = NfcorePipelineUtils.toolBibliographyText(emptyMap)

        then:
        citationText == "No tools used in the workflow."
        bibText == "No bibliography entries found."
    }

    def "integration test of all citation functions"() {
        given:
        // Create test meta.yml files for different modules
        def metaFile1 = new File(tempDir.toFile(), "module1_meta.yml")
        metaFile1 << """
        name: module1
        tools:
          - samtools:
              doi: "10.1093/bioinformatics/btp352"
              homepage: "https://www.htslib.org/"
              author: "Li H, et al."
              year: 2009
              title: "The Sequence Alignment/Map format and SAMtools"
              journal: "Bioinformatics"
        """

        def metaFile2 = new File(tempDir.toFile(), "module2_meta.yml")
        metaFile2 << """
        name: module2
        tools:
          - fastqc:
              description: "Quality control tool for sequence data"
              homepage: "https://www.bioinformatics.babraham.ac.uk/projects/fastqc/"
        """
        
        def mqcMethodsFile = new File(tempDir.toFile(), "methods_description.yml")
        mqcMethodsFile << """
        id: 'methods-description'
        section_name: 'Test Pipeline Methods'
        section_href: 'https://example.com'
        plot_type: 'html'
        description: |
            This pipeline uses the following tools: ${tool_citations}
            
            <h4>Bibliography</h4>
            <ol>
            ${tool_bibliography}
            </ol>
        """

        // Mock Nextflow session
        GroovyMock(Session, global: true)
        def mockSession = Mock(Session)
        def mockManifest = Mock() { toMap() >> [name: "test-workflow", doi: "10.5281/zenodo.123456"] }
        def mockWorkflowMetadata = Mock() { toMap() >> [projectName: "test_pipeline"] }
        mockSession.getManifest() >> mockManifest
        mockSession.getWorkflowMetadata() >> mockWorkflowMetadata
        def originalSession = nextflow.Nextflow.metaClass.static.getSession
        nextflow.Nextflow.metaClass.static.getSession = { -> mockSession }

        when:
        // Simulate processing at module level
        def module1Citations = NfcorePipelineUtils.generateModuleToolCitation(metaFile1)
        def module2Citations = NfcorePipelineUtils.generateModuleToolCitation(metaFile2)
        
        // Simulate collecting citations from channel
        def allCitations = [:]
        allCitations.putAll(module1Citations)
        allCitations.putAll(module2Citations)
        
        // Generate final methods description
        def meta = [:]
        meta["tool_citations"] = NfcorePipelineUtils.toolCitationText(allCitations)
        meta["tool_bibliography"] = NfcorePipelineUtils.toolBibliographyText(allCitations)
        def methodsHtml
        try {
            methodsHtml = NfcorePipelineUtils.methodsDescriptionText(mqcMethodsFile, allCitations, meta)
        } catch (Exception e) {
            println "DEBUG: Exception in integration test methodsDescriptionText: ${e}"
            throw e
        }

        then:
        // Verify module level citations
        module1Citations.size() == 1
        module1Citations.containsKey("samtools")
        module1Citations.samtools.citation.contains("samtools")
        module1Citations.samtools.citation.contains("10.1093/bioinformatics/btp352")
        
        module2Citations.size() == 1
        module2Citations.containsKey("fastqc")
        module2Citations.fastqc.citation.contains("fastqc")
        
        // Verify final methods HTML
        methodsHtml.contains("samtools")
        methodsHtml.contains("fastqc")
        methodsHtml.contains("Quality control tool")
        methodsHtml.contains("Li H, et al")

        cleanup:
        nextflow.Nextflow.metaClass.static.getSession = originalSession
    }
} 