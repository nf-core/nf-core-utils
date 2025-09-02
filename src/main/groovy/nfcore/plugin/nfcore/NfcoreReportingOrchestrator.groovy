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

/**
 * Orchestration utility that coordinates between version management and citation management
 * to provide comprehensive pipeline reporting capabilities.
 *
 * This class follows the composition pattern, using NfcoreVersionUtils and NfcoreCitationUtils
 * as focused, single-responsibility services while providing convenient high-level methods
 * for common reporting scenarios.
 */
class NfcoreReportingOrchestrator {

    /**
     * Generate comprehensive version and citation report for a pipeline
     *
     * This method orchestrates between version and citation utilities to provide
     * a complete report including versions, citations, bibliography, and methods description.
     *
     * @param topicVersions List of topic channel data [process, name, version]
     * @param legacyVersions List of legacy YAML version strings (optional)
     * @param metaFilePaths List of paths to module meta.yml files for citations (optional)
     * @param mqcMethodsYaml Path to MultiQC methods description template (optional)
     * @param session The Nextflow session
     * @return Map containing versions YAML, citations, bibliography, and methods description
     */
    static Map generateComprehensiveReport(
        List<List> topicVersions,
        List<String> legacyVersions = [],
        List<String> metaFilePaths = [],
        File mqcMethodsYaml = null,
        Session session
    ) {
        // Generate versions YAML using focused version utility
        def versionsYaml = NfcoreVersionUtils.processVersionsFromTopicChannels(
            topicVersions, legacyVersions, session
        )

        // Generate citations if meta files provided using focused citation utility
        def allCitations = [:]
        if (metaFilePaths) {
            allCitations = collectCitationsFromMetaFiles(metaFilePaths)
        }

        // Generate citation text and bibliography using citation utility
        def toolCitations = NfcoreCitationUtils.toolCitationText(allCitations)
        def toolBibliography = NfcoreCitationUtils.toolBibliographyText(allCitations)

        // Generate methods description if template provided
        def methodsDescription = ""
        if (mqcMethodsYaml && mqcMethodsYaml.exists()) {
            methodsDescription = generateMethodsDescription(mqcMethodsYaml, allCitations, session)
        }

        return [
            versions_yaml: versionsYaml,
            tool_citations: toolCitations,
            tool_bibliography: toolBibliography,
            methods_description: methodsDescription,
            citations_map: allCitations
        ]
    }

    /**
     * Generate a version-only report (no citations)
     * Useful when citations are not needed or handled separately
     *
     * @param topicVersions List of topic channel data [process, name, version]
     * @param legacyVersions List of legacy YAML version strings (optional)
     * @param session The Nextflow session
     * @return Map containing only versions information
     */
    static Map generateVersionReport(
        List<List> topicVersions,
        List<String> legacyVersions = [],
        Session session
    ) {
        def versionsYaml = NfcoreVersionUtils.processVersionsFromTopicChannels(
            topicVersions, legacyVersions, session
        )

        return [
            versions_yaml: versionsYaml
        ]
    }

    /**
     * Generate a citation-only report (no versions)
     * Useful when versions are handled separately
     *
     * @param metaFilePaths List of paths to module meta.yml files for citations
     * @param mqcMethodsYaml Path to MultiQC methods description template (optional)
     * @param session The Nextflow session (optional, used for methods description)
     * @return Map containing only citation information
     */
    static Map generateCitationReport(
        List<String> metaFilePaths,
        File mqcMethodsYaml = null,
        Session session = null
    ) {
        def allCitations = collectCitationsFromMetaFiles(metaFilePaths)
        def toolCitations = NfcoreCitationUtils.toolCitationText(allCitations)
        def toolBibliography = NfcoreCitationUtils.toolBibliographyText(allCitations)

        def methodsDescription = ""
        if (mqcMethodsYaml && mqcMethodsYaml.exists() && session) {
            methodsDescription = generateMethodsDescription(mqcMethodsYaml, allCitations, session)
        }

        return [
            tool_citations: toolCitations,
            tool_bibliography: toolBibliography,
            methods_description: methodsDescription,
            citations_map: allCitations
        ]
    }

    /**
     * Private helper method to collect citations from meta.yml files
     * Handles error cases gracefully and continues processing other files
     *
     * @param metaFilePaths List of paths to meta.yml files
     * @return Map of all collected citations
     */
    private static Map collectCitationsFromMetaFiles(List<String> metaFilePaths) {
        def allCitations = [:]

        metaFilePaths.each { metaPath ->
            try {
                def citations = NfcoreCitationUtils.generateModuleToolCitation(metaPath)
                allCitations.putAll(citations)
            } catch (Exception e) {
                // Log warning but continue processing other files
                println "Warning: Could not process meta.yml at ${metaPath}: ${e.message}"
            }
        }

        return allCitations
    }

    /**
     * Private helper method to generate methods description
     * Handles session metadata preparation for template processing
     *
     * @param mqcMethodsYaml MultiQC methods template file
     * @param allCitations Map of collected citations
     * @param session Nextflow session
     * @return Methods description HTML string
     */
    private static String generateMethodsDescription(File mqcMethodsYaml, Map allCitations, Session session) {
        try {
            // Prepare metadata for template processing
            def meta = prepareMetadataForTemplate(session)
            return NfcoreCitationUtils.methodsDescriptionText(mqcMethodsYaml, allCitations, meta)
        } catch (Exception e) {
            println "Warning: Could not generate methods description: ${e.message}"
            return ""
        }
    }

    /**
     * Private helper method to prepare metadata for template processing
     * Centralizes the metadata preparation logic
     *
     * @param session Nextflow session
     * @return Map containing prepared metadata
     */
    private static Map prepareMetadataForTemplate(Session session) {
        def meta = [:]

        // Add manifest information
        def manifest = session?.getManifest()
        if (manifest) {
            meta["manifest_map"] = manifest.toMap() ?: [:]
        } else {
            meta["manifest_map"] = [:]
        }

        // Add workflow metadata
        try {
            if (session) {
                meta.workflow = session.getWorkflowMetadata()?.toMap() ?: [:]
            } else {
                meta.workflow = [:]
            }
        } catch (Exception e) {
            // Handle case where getWorkflowMetadata() is not available
            meta.workflow = [:]
        }

        return meta
    }
}
