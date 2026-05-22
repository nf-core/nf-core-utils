package nfcore.plugin.nfcore

import spock.lang.Narrative
import spock.lang.Issue
import spock.lang.See
import spock.lang.Specification
import spock.lang.Title

@Title("ReportingOrchestrator accepts PipelineExecutionContext instead of Session")
@Narrative("""
Deepens the orchestrator to be the single reporting seam: it accepts
PipelineExecutionContext and uses SoftwareVersionReport internally,
removing the dependency on deprecated NfcoreVersionUtils methods.
""")
@Issue("https://github.com/nf-core/nf-core-utils/pull/41")
@See("https://github.com/nf-core/nf-core-utils/blob/main/docs/adr/0001-prioritize-deep-modules-around-execution-context-and-reporting.md#4-reporting-orchestration-seam")
class NfcoreReportingOrchestratorDeepTest extends Specification {

    def 'generateComprehensiveReport with PipelineExecutionContext produces versions + citations'() {
        given:
        def ctx = new PipelineExecutionContext(
            workflowName: 'nf-core/rnaseq',
            workflowVersion: '3.14.0',
            nextflowVersion: '25.04.0',
            manifestMap: [name: 'nf-core/rnaseq', version: '3.14.0']
        )
        def topicVersions = [
            ['NFCORE_FASTQC', 'fastqc', '0.12.1'],
            ['NFCORE_MULTIQC', 'multiqc', '1.15']
        ]
        def metaFilePath = 'src/test/resources/example_meta.yml'

        when:
        def result = NfcoreReportingOrchestrator.generateComprehensiveReport(
            ctx, topicVersions, [], [metaFilePath], null
        )

        then:
        result.versions_yaml.contains('fastqc: 0.12.1')
        result.versions_yaml.contains('Workflow:')
        result.versions_yaml.contains('nf-core/rnaseq:')
        result.tool_citations.contains('fastqc')
        result.citations_map.size() > 0
    }

    def 'generateVersionReport with PipelineExecutionContext uses SoftwareVersionReport'() {
        given:
        def ctx = new PipelineExecutionContext(
            workflowName: 'nf-core/sarek',
            workflowVersion: '3.4.0',
            nextflowVersion: '25.04.0'
        )
        def topicVersions = [
            ['PROCESS_A', 'bwa', '0.7.17']
        ]

        when:
        def result = NfcoreReportingOrchestrator.generateVersionReport(ctx, topicVersions, [])

        then:
        result.versions_yaml.contains('bwa:')
        result.versions_yaml.contains('Workflow:')
        result.versions_yaml.contains('nf-core/sarek:')
    }

    def 'generateCitationReport with PipelineExecutionContext'() {
        given:
        def ctx = new PipelineExecutionContext(
            workflowName: 'nf-core/rnaseq',
            manifestMap: [name: 'nf-core/rnaseq', version: '3.14.0']
        )
        def metaFilePath = 'src/test/resources/example_meta.yml'

        when:
        def result = NfcoreReportingOrchestrator.generateCitationReport(ctx, [metaFilePath], null)

        then:
        result.tool_citations.contains('fastqc')
        result.tool_bibliography.contains('<li>')
    }

    def 'generateComprehensiveReport with null context uses safe defaults'() {
        given:
        def topicVersions = [
            ['PROC', 'tool1', '1.0']
        ]

        when:
        def result = NfcoreReportingOrchestrator.generateComprehensiveReport(
            null as PipelineExecutionContext, topicVersions, [], [], null
        )

        then:
        result.versions_yaml.contains('tool1:')
        noExceptionThrown()
    }

    def 'generateVersionReport with legacy versions merges correctly'() {
        given:
        def ctx = new PipelineExecutionContext(
            workflowName: 'pipe',
            workflowVersion: '1.0.0',
            nextflowVersion: '25.04.0'
        )
        def topicVersions = [['FASTQC', 'fastqc', '0.12.1']]
        def legacyVersions = ['samtools: 1.17']

        when:
        def result = NfcoreReportingOrchestrator.generateVersionReport(ctx, topicVersions, legacyVersions)

        then:
        result.versions_yaml.contains('fastqc: 0.12.1')
        result.versions_yaml.contains('samtools: 1.17')
        result.versions_yaml.contains('pipe:')
    }
}
