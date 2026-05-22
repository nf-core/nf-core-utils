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

import groovy.util.logging.Slf4j
import nextflow.Session

/**
 * Single reporting seam that coordinates version and citation report assembly.
 *
 * Primary API accepts {@link PipelineExecutionContext}; Session-based overloads
 * are kept for backward compatibility.
 *
 * Uses {@link SoftwareVersionReport} for version collection and
 * {@link NfcoreCitationUtils} for citation formatting.
 *
 * @see docs/adr/0001-prioritize-deep-modules-around-execution-context-and-reporting.md §4
 */
@Slf4j
class NfcoreReportingOrchestrator {

    // =========================================================================
    // PRIMARY API — accepts PipelineExecutionContext
    // =========================================================================

    /**
     * Generate comprehensive version and citation report.
     *
     * @param ctx Pipeline execution context (null for safe defaults)
     * @param topicVersions List of topic channel data [process, name, version]
     * @param legacyVersions List of legacy YAML version strings
     * @param metaFilePaths List of paths to module meta.yml files for citations
     * @param mqcMethodsYaml MultiQC methods description template file
     * @return Map with versions_yaml, tool_citations, tool_bibliography, methods_description, citations_map
     */
    static Map generateComprehensiveReport(
        PipelineExecutionContext ctx,
        List<List> topicVersions,
        List<String> legacyVersions = [],
        List<String> metaFilePaths = [],
        File mqcMethodsYaml = null
    ) {
        def versionsYaml = buildVersionsYaml(ctx, topicVersions, legacyVersions)

        def allCitations = metaFilePaths ? collectCitationsFromMetaFiles(metaFilePaths) : [:]
        def toolCitations = NfcoreCitationUtils.toolCitationText(allCitations)
        def toolBibliography = NfcoreCitationUtils.toolBibliographyText(allCitations)

        def methodsDescription = ""
        if (mqcMethodsYaml && mqcMethodsYaml.exists()) {
            methodsDescription = generateMethodsDescription(mqcMethodsYaml, allCitations, ctx)
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
     * Generate version-only report.
     */
    static Map generateVersionReport(
        PipelineExecutionContext ctx,
        List<List> topicVersions,
        List<String> legacyVersions = []
    ) {
        return [versions_yaml: buildVersionsYaml(ctx, topicVersions, legacyVersions)]
    }

    /**
     * Generate citation-only report.
     */
    static Map generateCitationReport(
        PipelineExecutionContext ctx,
        List<String> metaFilePaths,
        File mqcMethodsYaml = null
    ) {
        def allCitations = collectCitationsFromMetaFiles(metaFilePaths)
        def toolCitations = NfcoreCitationUtils.toolCitationText(allCitations)
        def toolBibliography = NfcoreCitationUtils.toolBibliographyText(allCitations)

        def methodsDescription = ""
        if (mqcMethodsYaml && mqcMethodsYaml.exists() && ctx) {
            methodsDescription = generateMethodsDescription(mqcMethodsYaml, allCitations, ctx)
        }

        return [
            tool_citations: toolCitations,
            tool_bibliography: toolBibliography,
            methods_description: methodsDescription,
            citations_map: allCitations
        ]
    }

    // =========================================================================
    // COMPATIBILITY — Session-based overloads delegate to context-based API
    // =========================================================================

    /**
     * @deprecated Use {@link #generateComprehensiveReport(PipelineExecutionContext, List, List, List, File)}
     */
    @Deprecated
    static Map generateComprehensiveReport(
        List<List> topicVersions,
        List<String> legacyVersions = [],
        List<String> metaFilePaths = [],
        File mqcMethodsYaml = null,
        Session session
    ) {
        def ctx = PipelineExecutionContext.fromSession(session)
        return generateComprehensiveReport(ctx, topicVersions, legacyVersions, metaFilePaths, mqcMethodsYaml)
    }

    /**
     * @deprecated Use {@link #generateVersionReport(PipelineExecutionContext, List, List)}
     */
    @Deprecated
    static Map generateVersionReport(
        List<List> topicVersions,
        List<String> legacyVersions = [],
        Session session
    ) {
        def ctx = PipelineExecutionContext.fromSession(session)
        return generateVersionReport(ctx, topicVersions, legacyVersions)
    }

    /**
     * @deprecated Use {@link #generateCitationReport(PipelineExecutionContext, List, File)}
     */
    @Deprecated
    static Map generateCitationReport(
        List<String> metaFilePaths,
        File mqcMethodsYaml = null,
        Session session = null
    ) {
        def ctx = session ? PipelineExecutionContext.fromSession(session) : null
        return generateCitationReport(ctx, metaFilePaths, mqcMethodsYaml)
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private static String buildVersionsYaml(
        PipelineExecutionContext ctx,
        List<List> topicVersions,
        List<String> legacyVersions
    ) {
        def report = new SoftwareVersionReport()

        if (topicVersions) report.addInput(topicVersions)
        if (legacyVersions) report.addInput(legacyVersions)

        if (ctx) {
            def wfVersion = NfcoreVersionUtils.getWorkflowVersion(
                null, ctx.workflowVersion
            )
            report.addWorkflowVersion(ctx.workflowName, wfVersion, ctx.nextflowVersion)
        }

        return report.renderYaml()
    }

    private static Map collectCitationsFromMetaFiles(List<String> metaFilePaths) {
        def allCitations = [:]
        metaFilePaths.each { metaPath ->
            try {
                def citations = NfcoreCitationUtils.generateModuleToolCitation(metaPath)
                allCitations.putAll(citations)
            } catch (Exception e) {
                log.warn("Could not process meta.yml at ${metaPath}: ${e.message}")
            }
        }
        return allCitations
    }

    private static String generateMethodsDescription(File mqcMethodsYaml, Map allCitations, PipelineExecutionContext ctx) {
        try {
            def meta = [
                manifest_map: ctx?.manifestMap ?: [:],
                workflow: ctx?.workflowMap ?: [:]
            ]
            return NfcoreCitationUtils.methodsDescriptionText(mqcMethodsYaml, allCitations, meta)
        } catch (Exception e) {
            log.warn("Could not generate methods description: ${e.message}")
            return ""
        }
    }

    /** @deprecated kept for Session-based overload */
    @Deprecated
    private static String generateMethodsDescription(File mqcMethodsYaml, Map allCitations, Session session) {
        try {
            def meta = prepareMetadataForTemplate(session)
            return NfcoreCitationUtils.methodsDescriptionText(mqcMethodsYaml, allCitations, meta)
        } catch (Exception e) {
            log.warn("Could not generate methods description: ${e.message}")
            return ""
        }
    }

    /** @deprecated kept for Session-based overload */
    @Deprecated
    private static Map prepareMetadataForTemplate(Session session) {
        def meta = [:]
        def manifest = session?.getManifest()
        meta["manifest_map"] = manifest?.toMap() ?: [:]
        try {
            meta.workflow = session?.getWorkflowMetadata()?.toMap() ?: [:]
        } catch (Exception e) {
            meta.workflow = [:]
        }
        return meta
    }
}
