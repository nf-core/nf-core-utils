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

package nfcore.plugin

import nextflow.Session
import nextflow.plugin.extension.Function
import nextflow.plugin.extension.PluginExtensionPoint
import nfcore.plugin.nfcore.NfcoreConfigValidator
import nfcore.plugin.nfcore.NfcoreVersionUtils
import nfcore.plugin.nfcore.NfcoreCitationUtils
import nfcore.plugin.nfcore.NfcoreReportingOrchestrator

/**
 * Implements a custom function which can be imported by
 * Nextflow scripts.
 */
class NfUtilsExtension extends PluginExtensionPoint {

    private Session session

    @Override
    protected void init(Session session) {
        this.session = session
    }

    /**
     * Say hello to the given target.
     *
     * @param target
     */
    @Function
    void sayHello(String target) {
        println "Hello, ${target}!"
    }

    // --- Methods from NfcoreExtension ---
    /**
     * Generate methods description text for MultiQC report
     * @param workflow Workflow metadata
     * @param mqcMethodsYaml Path to MultiQC methods YAML template
     * @param modulePaths List of paths to module directories containing meta.yml files
     * @return HTML formatted methods description
     */
    // TODO This is still in the works
    // @Function
    // String methodsDescriptionText(nextflow.script.WorkflowMetadata workflow, String mqcMethodsYaml, List modulePaths) {
    //     return NfcoreExtension.methodsDescriptionText(workflow, mqcMethodsYaml, modulePaths)
    // }

    /**
     * Send completion email
     * @param summaryParams Map of parameter groups and their parameters
     * @param email Email address to send to
     * @param emailOnFail Email address to send to on failure
     * @param plaintextEmail Whether to send plaintext email
     * @param outdir Output directory
     * @param monochromeLogs Whether to use monochrome logs
     * @param multiqcReports List of MultiQC report paths
     */
    @Function
    void completionEmail(Map summaryParams, String email, String emailOnFail, boolean plaintextEmail, String outdir, boolean monochromeLogs, List multiqcReports) {
        nfcore.plugin.nfcore.NfcoreNotificationUtils.completionEmail(summaryParams, email, emailOnFail, plaintextEmail, outdir, monochromeLogs, multiqcReports)
    }

    /**
     * Send completion summary to log
     * @param monochromeLogs Whether to use monochrome logs
     */
    @Function
    void completionSummary(boolean monochromeLogs) {
        nfcore.plugin.nfcore.NfcoreNotificationUtils.completionSummary(monochromeLogs)
    }

    /**
     * Send Instant Messenger notification
     * @param summaryParams Map of parameter groups and their parameters
     * @param hookUrl Hook URL for the IM service
     */
    @Function
    void imNotification(Map summaryParams, String hookUrl) {
        nfcore.plugin.nfcore.NfcoreNotificationUtils.imNotification(summaryParams, hookUrl)
    }

    /**
     * Check if the profile string is valid and warn about positional arguments
     * @param args The command line arguments (as List)
     * @param monochromeLogs Whether to use monochrome logs (default: true)
     */
    @Function
    void checkProfileProvided(List args, boolean monochromeLogs = true) {
        String profile = null
        for (int i = 0; i < args.size(); i++) {
            if (args[i] == '-profile' && i + 1 < args.size()) {
                profile = args[i + 1]
                break
            }
        }
        String commandLine = args.join(' ')
        NfcoreConfigValidator.checkProfileProvided(profile, commandLine, monochromeLogs)
    }

    /**
     * Check if a custom config or profile has been provided, logs a warning if not.
     * @return true if config is valid, false otherwise
     */
    @Function
    boolean checkConfigProvided() {
        def meta = this.session?.getWorkflowMetadata()
        def config = this.session?.config
        String projectName = null
        if (meta != null && meta.metaClass?.hasProperty(meta, 'projectName')) {
            projectName = meta.projectName
        }
        return NfcoreConfigValidator.checkConfigProvided(projectName, config)
    }

    // --- Methods from ReferencesExtension ---
    /**
     * Get references file from a references list or parameters
     * @param referencesList The references list (List of [meta, _readme])
     * @param param The file parameter
     * @param attribute The attribute to look for in metadata
     * @param basepath The base path for igenomes
     * @return A list of reference files
     */
    @Function
    List getReferencesFile(List<List> referencesList, String param, String attribute, String basepath) {
        return nfcore.plugin.references.ReferencesUtils.getReferencesFile(referencesList, param, attribute, basepath)
    }

    /**
     * Get references value from a references list or parameters
     * @param referencesList The references list (List of [meta, _readme])
     * @param param The value parameter
     * @param attribute The attribute to look for in metadata
     * @return A list of reference values
     */
    @Function
    List getReferencesValue(List<List> referencesList, String param, String attribute) {
        return nfcore.plugin.references.ReferencesUtils.getReferencesValue(referencesList, param, attribute)
    }

    // --- Methods from NextflowPipelineExtension ---
    /**
     * Generate version string for a workflow
     * @param version The workflow version (optional)
     * @param commitId The workflow commit ID (optional)
     * @return A formatted version string
     */
    @Function
    String getWorkflowVersion(String version = null, String commitId = null) {
        return NfcoreVersionUtils.getWorkflowVersion(this.session, version, commitId)
    }

    /**
     * Dump pipeline parameters to a JSON file
     * @param outdir The output directory
     * @param params The pipeline parameters
     */
    @Function
    void dumpParametersToJSON(String outdir, Map params) {
        if (outdir == null) return
        java.nio.file.Path outdirPath = java.nio.file.Paths.get(outdir)
        nfcore.plugin.nextflow.NextflowPipelineUtils.dumpParametersToJSON(outdirPath, params)
    }

    /**
     * Check if conda channels are set up correctly
     * @return True if channels are set up correctly, false otherwise
     */
    @Function
    boolean checkCondaChannels() {
        return nfcore.plugin.nextflow.NextflowPipelineUtils.checkCondaChannels()
    }

    // --- Enhanced Version Utilities ---
    /**
     * Process versions from both topic channels and legacy files
     * Supports progressive migration from versions.yml files to topic channels
     *
     * @param topicVersions List of [process, name, version] from 'versions' topic
     * @param versionsFiles List of file paths from 'versions_file' topic
     * @return Combined YAML string with all versions
     */
    @Function
    String processMixedVersionSources(List<List> topicVersions, List<String> versionsFiles) {
        return NfcoreVersionUtils.processMixedVersionSources(topicVersions, versionsFiles, this.session)
    }

    /**
     * Convert legacy YAML string to new eval syntax format
     * Transforms old versions.yml content to [process, name, version] tuples
     *
     * @param yamlContent The YAML content as string
     * @param processName The process name to use (defaults to 'LEGACY')
     * @return List of [process, name, version] tuples
     */
    @Function
    List<List> convertLegacyYamlToEvalSyntax(String yamlContent, String processName = 'LEGACY') {
        return NfcoreVersionUtils.convertLegacyYamlToEvalSyntax(yamlContent, processName)
    }

    /**
     * Generate YAML output from eval syntax data
     * Converts [process, name, version] tuples back to YAML format for reporting
     *
     * @param evalData List of [process, name, version] tuples
     * @param includeWorkflow Whether to include workflow version info
     * @return YAML string suitable for MultiQC and reporting
     */
    @Function
    String generateYamlFromEvalSyntax(List<List> evalData, boolean includeWorkflow = true) {
        return NfcoreVersionUtils.generateYamlFromEvalSyntax(evalData, this.session, includeWorkflow)
    }

    /**
     * Process versions from topic channel format (new eval syntax)
     *
     * @param topicData List containing [process, name, version] tuples
     * @return YAML string with processed versions
     */
    @Function
    String processVersionsFromTopic(List<List> topicData) {
        return NfcoreVersionUtils.processVersionsFromTopic(topicData)
    }

    /**
     * Process versions from file paths (legacy format)
     *
     * @param versionsFiles List of file paths to versions.yml files
     * @return YAML string with processed versions
     */
    @Function
    String processVersionsFromFile(List<String> versionsFiles) {
        return NfcoreVersionUtils.processVersionsFromFile(versionsFiles)
    }

    /**
     * Get workflow version information as topic channel data
     * For use with topic channels
     *
     * @return List of [process, name, version] tuples for workflow info
     */
    @Function
    List<List> workflowVersionToChannel() {
        return NfcoreVersionUtils.workflowVersionToChannel(this.session)
    }

    // --- Citation Management Functions ---
    /**
     * Generate citation for a tool from meta.yml at the module level
     *
     * @param metaFilePath Path to the meta.yml file (String or File)
     * @return Map containing tool citations for the module
     */
    @Function
    Map generateModuleToolCitation(Object metaFilePath) {
        return NfcoreCitationUtils.generateModuleToolCitation(metaFilePath)
    }

    /**
     * Generate methods description for MultiQC using collected citations
     *
     * @param collectedCitations Map containing all tool citations from modules
     * @return Formatted citation string for tools used in the workflow
     */
    @Function
    String toolCitationText(Map collectedCitations) {
        return NfcoreCitationUtils.toolCitationText(collectedCitations)
    }

    /**
     * Generate bibliography text from collected citations
     *
     * @param collectedCitations Map containing all tool citations from modules
     * @return Formatted bibliography HTML for tools used in the workflow
     */
    @Function
    String toolBibliographyText(Map collectedCitations) {
        return NfcoreCitationUtils.toolBibliographyText(collectedCitations)
    }

    /**
     * Generate methods description text using collected citations
     *
     * @param mqcMethodsYaml MultiQC methods YAML file path
     * @param collectedCitations Map containing all tool citations from modules (optional)
     * @param meta Additional metadata (optional)
     * @return Formatted methods description HTML
     */
    @Function
    String methodsDescriptionText(String mqcMethodsYamlPath, Map collectedCitations = [:], Map meta = [:]) {
        def mqcFile = new File(mqcMethodsYamlPath)
        return NfcoreCitationUtils.methodsDescriptionText(mqcFile, collectedCitations, meta)
    }

    /**
     * Collect citations from multiple meta.yml files
     *
     * @param metaFilePaths List of paths to module meta.yml files
     * @return Map containing all collected citations
     */
    @Function
    Map collectCitationsFromFiles(List<String> metaFilePaths) {
        def allCitations = [:]
        metaFilePaths.each { metaPath ->
            try {
                def citations = NfcoreCitationUtils.generateModuleToolCitation(metaPath)
                allCitations.putAll(citations)
            } catch (Exception e) {
                System.err.println("Warning: Could not process meta.yml at ${metaPath}: ${e.message}")
            }
        }
        return allCitations
    }

    // --- Orchestrated Reporting Functions ---
    /**
     * Generate comprehensive version and citation report
     *
     * @param topicVersions List of topic channel data [process, name, version]
     * @param legacyVersions List of legacy YAML version strings (optional)
     * @param metaFilePaths List of paths to module meta.yml files (optional)
     * @param mqcMethodsYamlPath Path to MultiQC methods description template (optional)
     * @return Map containing versions YAML, citations, bibliography, and methods description
     */
    @Function
    Map generateComprehensiveReport(
        List<List> topicVersions,
        List<String> legacyVersions = [],
        List<String> metaFilePaths = [],
        String mqcMethodsYamlPath = null
    ) {
        def mqcFile = mqcMethodsYamlPath ? new File(mqcMethodsYamlPath) : null
        return NfcoreReportingOrchestrator.generateComprehensiveReport(
            topicVersions, legacyVersions, metaFilePaths, mqcFile, this.session
        )
    }

    /**
     * Generate version-only report
     *
     * @param topicVersions List of topic channel data [process, name, version]
     * @param legacyVersions List of legacy YAML version strings (optional)
     * @return Map containing only versions information
     */
    @Function
    Map generateVersionReport(List<List> topicVersions, List<String> legacyVersions = []) {
        return NfcoreReportingOrchestrator.generateVersionReport(topicVersions, legacyVersions, this.session)
    }

    /**
     * Generate citation-only report
     *
     * @param metaFilePaths List of paths to module meta.yml files
     * @param mqcMethodsYamlPath Path to MultiQC methods description template (optional)
     * @return Map containing only citation information
     */
    @Function
    Map generateCitationReport(List<String> metaFilePaths, String mqcMethodsYamlPath = null) {
        def mqcFile = mqcMethodsYamlPath ? new File(mqcMethodsYamlPath) : null
        return NfcoreReportingOrchestrator.generateCitationReport(metaFilePaths, mqcFile, this.session)
    }

    // --- Enhanced Citation Topic Channel Functions ---
    /**
     * Process citations from topic channel format (new eval syntax)
     *
     * @param topicData List containing [module, tool, citation_data] tuples
     * @return Map of tool citations
     */
    @Function
    Map processCitationsFromTopic(List<List> topicData) {
        return NfcoreCitationUtils.processCitationsFromTopic(topicData)
    }

    /**
     * Process citations from file paths (legacy format)
     *
     * @param citationFiles List of file paths to meta.yml files
     * @return Map of tool citations
     */
    @Function
    Map processCitationsFromFile(List<String> citationFiles) {
        return NfcoreCitationUtils.processCitationsFromFile(citationFiles)
    }

    /**
     * Process citations from both topic channels and legacy files
     * Supports progressive migration from meta.yml files to topic channels
     *
     * @param topicCitations List of [module, tool, citation_data] from 'citations' topic
     * @param citationFiles List of file paths from 'citations_file' topic
     * @return Combined map of all citations
     */
    @Function
    Map processMixedCitationSources(List<List> topicCitations, List<String> citationFiles) {
        return NfcoreCitationUtils.processMixedCitationSources(topicCitations, citationFiles)
    }

    /**
     * Convert legacy meta.yml data to new topic channel format
     * Transforms meta.yml tools data to [module, tool, citation_data] tuples
     *
     * @param metaFilePath Path to meta.yml file
     * @param moduleName Name of the module (defaults to filename)
     * @return List of [module, tool, citation_data] tuples
     */
    @Function
    List<List> convertMetaYamlToTopicFormat(String metaFilePath, String moduleName = null) {
        return NfcoreCitationUtils.convertMetaYamlToTopicFormat(metaFilePath, moduleName)
    }

    /**
     * Extract citation from meta.yml for topic channel emission
     * Use in process outputs: val(getCitation("${moduleDir}/meta.yml")), topic: citation
     *
     * @param metaYmlPath Path to the module's meta.yml file
     * @return List in topic channel format ready for emission
     */
    @Function
    List getCitation(String metaYmlPath) {
        return NfcoreCitationUtils.getCitation(metaYmlPath)
    }

    /**
     * Automatically generate citation text from topic channel data
     * Use after collecting from topic: channel.topic('citation').collect()
     *
     * @param topicCitations Citation data collected from topic channel
     * @return Formatted citation text ready for reports
     */
    @Function
    String autoToolCitationText(List topicCitations = []) {
        return NfcoreCitationUtils.autoToolCitationText(topicCitations)
    }

    /**
     * Automatically generate bibliography from topic channel data
     * Use after collecting from topic: channel.topic('citation').collect()
     *
     * @param topicCitations Citation data collected from topic channel
     * @return Formatted bibliography HTML ready for reports
     */
    @Function
    String autoToolBibliographyText(List topicCitations = []) {
        return NfcoreCitationUtils.autoToolBibliographyText(topicCitations)
    }

}
