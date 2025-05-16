package nfcore.plugin.util

import nextflow.Session
import nextflow.config.Manifest
import spock.lang.Specification
import spock.lang.TempDir
import java.nio.file.Path
import org.yaml.snakeyaml.Yaml
import spock.lang.PendingFeature

class NfcoreCitationUtilsTest extends Specification {
    @TempDir
    Path tempDir

    def 'toolBibliographyText returns expected bibliography HTML from meta.yml'() {
        given:
        // Download the latest meta.yml from the nf-core/modules repo
        def metaUrl = 'https://raw.githubusercontent.com/nf-core/modules/refs/heads/master/modules/nf-core/fastqc/meta.yml'
        File tempMeta = File.createTempFile('meta', '.yml')
        tempMeta.withOutputStream { out ->
            out << new URL(metaUrl).openStream()
        }

        when:
        def citations = NfcoreCitationUtils.generateModuleToolCitation(tempMeta)
        def result = NfcoreCitationUtils.toolBibliographyText(citations)

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
        def meta = [:]
        meta["workflow"] = [name: 'nf-core/testpipe']
        // Mock Nextflow session and manifest
        def manifest = Mock(Manifest) {
            getName() >> 'nf-core/testpipe'
            getVersion() >> '1.0.0'
            toMap() >> [name: 'nf-core/testpipe', doi: '10.5281/zenodo.123456']
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
        def citations = NfcoreCitationUtils.generateModuleToolCitation(tempMeta)
        meta["tool_citations"] = NfcoreCitationUtils.toolCitationText(citations)
        meta["tool_bibliography"] = NfcoreCitationUtils.toolBibliographyText(citations)
        def result = NfcoreCitationUtils.methodsDescriptionText(tempYaml, citations, meta)

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
        def result = NfcoreCitationUtils.generateModuleToolCitation(metaFile)
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
        def result = NfcoreCitationUtils.toolCitationText(collectedCitations)

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
        def result = NfcoreCitationUtils.toolBibliographyText(collectedCitations)

        then:
        result == "<li>Samtools citation details</li> <li>FastQC citation details</li>"
    }

    @PendingFeature()
    def "methodsDescriptionText should generate HTML with collected citations"() {
        given:
        def mqcMethodsFile = new File(tempDir.toFile(), "mqc_methods.txt")
        mqcMethodsFile << """
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
        def mockWorkflowMetadata = Mock() { toMap() >> [name: "test-workflow"] }
        mockSession.getManifest() >> mockManifest
        mockSession.getWorkflowMetadata() >> mockWorkflowMetadata
        def originalSession = nextflow.Nextflow.metaClass.static.getSession
        nextflow.Nextflow.metaClass.static.getSession = { -> mockSession }

        when:
        def meta = [:]
        if (!meta.containsKey("workflow")) {
            meta.workflow = mockSession.getWorkflowMetadata()?.toMap() ?: [:]
        }
        if (!meta.containsKey("manifest_map")) {
            meta["manifest_map"] = mockSession.getManifest()?.toMap() ?: [:]
        }
        meta["tool_citations"] = NfcoreCitationUtils.toolCitationText(collectedCitations)
        meta["tool_bibliography"] = NfcoreCitationUtils.toolBibliographyText(collectedCitations)
        def result
        try {
            result = NfcoreCitationUtils.methodsDescriptionText(mqcMethodsFile, collectedCitations, meta)
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
        def citationText = NfcoreCitationUtils.toolCitationText(emptyMap)
        def bibText = NfcoreCitationUtils.toolBibliographyText(emptyMap)

        then:
        citationText == "No tools used in the workflow."
        bibText == "No bibliography entries found."
    }

    @PendingFeature()
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
        def mockWorkflowMetadata = Mock() { toMap() >> [name: "test-workflow"] }
        mockSession.getManifest() >> mockManifest
        mockSession.getWorkflowMetadata() >> mockWorkflowMetadata
        def originalSession = nextflow.Nextflow.metaClass.static.getSession
        nextflow.Nextflow.metaClass.static.getSession = { -> mockSession }

        when:
        // Simulate processing at module level
        def module1Citations = NfcoreCitationUtils.generateModuleToolCitation(metaFile1)
        def module2Citations = NfcoreCitationUtils.generateModuleToolCitation(metaFile2)
        
        // Simulate collecting citations from channel
        def allCitations = [:]
        allCitations.putAll(module1Citations)
        allCitations.putAll(module2Citations)
        
        // Generate final methods description
        def meta = [:]
        if (!meta.containsKey("workflow")) {
            meta.workflow = mockSession.getWorkflowMetadata()?.toMap() ?: [:]
        }
        if (!meta.containsKey("manifest_map")) {
            meta["manifest_map"] = mockSession.getManifest()?.toMap() ?: [:]
        }
        meta["tool_citations"] = NfcoreCitationUtils.toolCitationText(allCitations)
        meta["tool_bibliography"] = NfcoreCitationUtils.toolBibliographyText(allCitations)
        def methodsHtml
        try {
            methodsHtml = NfcoreCitationUtils.methodsDescriptionText(mqcMethodsFile, allCitations, meta)
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