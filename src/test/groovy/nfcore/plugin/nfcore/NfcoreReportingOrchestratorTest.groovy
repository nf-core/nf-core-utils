/*
 * Copyright 2025, nf-core
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nfcore.plugin.nfcore

import nextflow.Session
import nextflow.config.Manifest
import spock.lang.Specification
import spock.lang.Issue

class NfcoreReportingOrchestratorTest extends Specification {

    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'generateComprehensiveReport should create complete report'() {
        given:
        def topicVersions = [
            ['NFCORE_FASTQC', 'fastqc', '0.12.1'],
            ['NFCORE_MULTIQC', 'multiqc', '1.15']
        ]
        def legacyVersions = ['samtools: 1.17']
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreReportingOrchestrator.generateComprehensiveReport(
            topicVersions, legacyVersions, [], null, session
        )

        then:
        result.containsKey('versions_yaml')
        result.containsKey('tool_citations')
        result.containsKey('tool_bibliography')
        result.containsKey('methods_description')
        result.containsKey('citations_map')

        result.versions_yaml.contains('fastqc: 0.12.1')
        result.versions_yaml.contains("multiqc: '1.15'") || result.versions_yaml.contains('multiqc: 1.15')
        result.versions_yaml.contains('samtools: 1.17')
        result.versions_yaml.contains('testpipeline: v1.0.0')
    }

    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'generateComprehensiveReport should handle citations when meta files provided'() {
        given:
        def topicVersions = [
            ['NFCORE_FASTQC', 'fastqc', '0.12.1']
        ]
        def metaFilePath = 'src/test/resources/example_meta.yml'
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreReportingOrchestrator.generateComprehensiveReport(
            topicVersions, [], [metaFilePath], null, session
        )

        then:
        result.tool_citations.contains('fastqc')
        result.tool_bibliography.contains('<li>')
        result.citations_map.size() > 0
    }

    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'generateComprehensiveReport should handle methods description template'() {
        given:
        def topicVersions = [
            ['NFCORE_FASTQC', 'fastqc', '0.12.1']
        ]
        def metaFilePath = 'src/test/resources/example_meta.yml'
        def mqcMethodsFile = new File('src/test/resources/multiqc_methods_description.yml')
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
            toMap() >> [name: 'testpipeline', version: '1.0.0']
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreReportingOrchestrator.generateComprehensiveReport(
            topicVersions, [], [metaFilePath], mqcMethodsFile, session
        )

        then:
        result.containsKey('methods_description')
        result.tool_citations.contains('fastqc')
        result.tool_bibliography.contains('<li>')
    }

    @Issue("https://github.com/nf-core/proposals/issues/46")
    def 'generateComprehensiveReport should handle missing meta files gracefully'() {
        given:
        def topicVersions = [
            ['NFCORE_FASTQC', 'fastqc', '0.12.1']
        ]
        def nonExistentMetaPath = 'path/to/nonexistent/meta.yml'
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreReportingOrchestrator.generateComprehensiveReport(
            topicVersions, [], [nonExistentMetaPath], null, session
        )

        then:
        result.versions_yaml.contains('fastqc: 0.12.1')
        result.tool_citations == 'No tools used in the workflow.'
        result.tool_bibliography == 'No bibliography entries found.'
    }

    def 'generateVersionReport should create version-only report'() {
        given:
        def topicVersions = [
            ['NFCORE_FASTQC', 'fastqc', '0.12.1'],
            ['NFCORE_MULTIQC', 'multiqc', '1.15']
        ]
        def legacyVersions = ['samtools: 1.17']
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreReportingOrchestrator.generateVersionReport(
            topicVersions, legacyVersions, session
        )

        then:
        result.containsKey('versions_yaml')
        !result.containsKey('tool_citations')
        !result.containsKey('tool_bibliography')
        !result.containsKey('methods_description')
        !result.containsKey('citations_map')

        result.versions_yaml.contains('fastqc: 0.12.1')
        result.versions_yaml.contains("multiqc: '1.15'") || result.versions_yaml.contains('multiqc: 1.15')
        result.versions_yaml.contains('samtools: 1.17')
        result.versions_yaml.contains('testpipeline: v1.0.0')
    }

    def 'generateCitationReport should create citation-only report'() {
        given:
        def metaFilePath = 'src/test/resources/example_meta.yml'
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
            toMap() >> [name: 'testpipeline', version: '1.0.0']
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreReportingOrchestrator.generateCitationReport(
            [metaFilePath], null, session
        )

        then:
        !result.containsKey('versions_yaml')
        result.containsKey('tool_citations')
        result.containsKey('tool_bibliography')
        result.containsKey('methods_description')
        result.containsKey('citations_map')

        result.tool_citations.contains('fastqc')
        result.tool_bibliography.contains('<li>')
        result.citations_map.size() > 0
    }

    def 'generateCitationReport should handle methods description template'() {
        given:
        def metaFilePath = 'src/test/resources/example_meta.yml'
        def mqcMethodsFile = new File('src/test/resources/multiqc_methods_description.yml')
        def manifest = Mock(Manifest) {
            getName() >> 'testpipeline'
            getVersion() >> '1.0.0'
            toMap() >> [name: 'testpipeline', version: '1.0.0']
        }
        def session = Mock(Session) {
            getManifest() >> manifest
            getConfig() >> [nextflow: [version: '23.04.1']]
        }

        when:
        def result = NfcoreReportingOrchestrator.generateCitationReport(
            [metaFilePath], mqcMethodsFile, session
        )

        then:
        result.methods_description.length() > 0
        result.tool_citations.contains('fastqc')
        result.tool_bibliography.contains('<li>')
    }

    def 'generateCitationReport should handle missing session gracefully'() {
        given:
        def metaFilePath = 'src/test/resources/example_meta.yml'
        def mqcMethodsFile = new File('src/test/resources/multiqc_methods_description.yml')

        when:
        def result = NfcoreReportingOrchestrator.generateCitationReport(
            [metaFilePath], mqcMethodsFile, null
        )

        then:
        result.methods_description == ""  // Should be empty when no session provided
        result.tool_citations.contains('fastqc')
        result.tool_bibliography.contains('<li>')
    }

    def 'generateCitationReport should handle empty meta files list'() {
        when:
        def result = NfcoreReportingOrchestrator.generateCitationReport(
            [], null, null
        )

        then:
        result.tool_citations == 'No tools used in the workflow.'
        result.tool_bibliography == 'No bibliography entries found.'
        result.methods_description == ""
        result.citations_map.isEmpty()
    }
}
