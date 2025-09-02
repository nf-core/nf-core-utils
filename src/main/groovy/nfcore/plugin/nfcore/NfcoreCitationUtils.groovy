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
import org.yaml.snakeyaml.Yaml

/**
 * Utility functions for nf-core citations
 * Supports both legacy meta.yml files and new topic channel approach
 */
@Slf4j
class NfcoreCitationUtils {

    /**
     * Generate citation for a tool from meta.yml at the module level
     * @param metaFilePath Path to the meta.yml file (String or File)
     * @return Map containing tool citations for the module
     */
    static Map generateModuleToolCitation(Object metaFilePath) {
        File file = metaFilePath instanceof File ? metaFilePath : new File(metaFilePath.toString())
        if (!file.exists()) {
            throw new IllegalArgumentException("meta.yml file not found at: ${file.getAbsolutePath()}")
        }
        def yaml = new Yaml()
        Map meta
        file.withInputStream { is ->
            meta = yaml.load(is)
        }
        def tools = meta?.tools ?: []
        def moduleCitations = [:]

        tools.each { toolEntry ->
            toolEntry.each { toolName, toolInfo ->
                def citation = toolName
                def bibEntry = null

                // Generate citation text
                if (toolInfo instanceof Map) {
                    if (toolInfo.doi) {
                        citation += " (DOI: ${toolInfo.doi})"
                    } else if (toolInfo.description) {
                        citation += " (${toolInfo.description})"
                    }

                    // Generate bibliography entry
                    def author = toolInfo.author ?: ""
                    def year = toolInfo.year ?: ""
                    def title = toolInfo.title ?: toolName
                    def journal = toolInfo.journal ?: ""
                    def doi = toolInfo.doi ? "doi: ${toolInfo.doi}" : ""
                    def url = toolInfo.homepage ?: ""
                    def bibCitation = [author, year, title, journal, doi].findAll { it }.join(". ")
                    if (url) bibCitation += ". <a href='${url}'>${url}</a>"
                    bibEntry = "<li>${bibCitation}</li>"
                }

                moduleCitations[toolName] = [
                        citation    : citation,
                        bibliography: bibEntry
                ]
            }
        }

        return moduleCitations
    }

    /**
     * Generate methods description for MultiQC using collected citations
     * @param collectedCitations Map containing all tool citations from modules
     * @return Formatted citation string for tools used in the workflow
     */
    static String toolCitationText(Map collectedCitations) {
        if (collectedCitations.isEmpty()) {
            return "No tools used in the workflow."
        }

        def toolCitations = collectedCitations.values().collect { it.citation }
        return "Tools used in the workflow included: " + toolCitations.join(', ') + "."
    }

    /**
     * Generate bibliography text from collected citations
     * @param collectedCitations Map containing all tool citations from modules
     * @return Formatted bibliography HTML for tools used in the workflow
     */
    static String toolBibliographyText(Map collectedCitations) {
        if (collectedCitations.isEmpty()) {
            return "No bibliography entries found."
        }

        def bibEntries = collectedCitations.values()
                .findAll { it.bibliography }
                .collect { it.bibliography }

        return bibEntries.join(" ")
    }

    /**
     * Process citations from topic channel format
     * Handles the new eval syntax: [module, tool, citation_data]
     *
     * @param topicData List containing [module, tool, citation_data] tuples
     * @return Map of tool citations
     */
    static Map processCitationsFromTopic(List<List> topicData) {
        def citations = [:]
        topicData.each { tuple ->
            if (tuple.size() >= 3) {
                def module = tuple[0]
                def tool = tuple[1]
                def citationData = tuple[2]

                // Convert citation data to standard format
                if (citationData instanceof Map) {
                    citations[tool] = [
                        citation: formatCitationFromData(tool, citationData),
                        bibliography: formatBibliographyFromData(tool, citationData)
                    ]
                } else {
                    // Handle simple string citations
                    citations[tool] = [
                        citation: citationData.toString(),
                        bibliography: "<li>${citationData}</li>"
                    ]
                }
            }
        }
        return citations
    }

    /**
     * Process citations from citations_file topic (legacy meta.yml files)
     * Handles the old meta.yml file path output style
     *
     * @param citationFileData List containing file paths to meta.yml files
     * @return Map of tool citations
     */
    static Map processCitationsFromFile(List<String> citationFileData) {
        def allCitations = [:]
        citationFileData.each { filePath ->
            try {
                def citations = generateModuleToolCitation(filePath)
                allCitations.putAll(citations)
            } catch (Exception e) {
                System.err.println("Warning: Could not process citation file ${filePath}: ${e.message}")
            }
        }
        return allCitations
    }

    /**
     * Process mixed citation sources (topic channels and file-based)
     * Combines data from both 'citations' and 'citations_file' topics
     *
     * @param topicCitations List of [module, tool, citation_data] from 'citations' topic
     * @param citationFiles List of file paths from 'citations_file' topic
     * @return Combined map of all citations
     */
    static Map processMixedCitationSources(List<List> topicCitations, List<String> citationFiles) {
        def combinedCitations = [:]

        // Process new topic format
        if (topicCitations) {
            def topicCitationMap = processCitationsFromTopic(topicCitations)
            combinedCitations.putAll(topicCitationMap)
        }

        // Process legacy file format
        if (citationFiles) {
            def fileCitationMap = processCitationsFromFile(citationFiles)
            combinedCitations.putAll(fileCitationMap)
        }

        return combinedCitations
    }

    /**
     * Convert legacy meta.yml data to new topic channel format
     * Transforms meta.yml tools data to [module, tool, citation_data] tuples
     *
     * @param metaFilePath Path to meta.yml file
     * @param moduleName Name of the module (defaults to filename)
     * @return List of [module, tool, citation_data] tuples
     */
    static List<List> convertMetaYamlToTopicFormat(String metaFilePath, String moduleName = null) {
        try {
            def file = new File(metaFilePath)
            if (!file.exists()) {
                return []
            }

            if (!moduleName) {
                moduleName = file.getParentFile()?.getName() ?: 'unknown'
            }

            def yaml = new Yaml()
            Map meta
            file.withInputStream { is ->
                meta = yaml.load(is)
            }

            def result = []
            def tools = meta?.tools ?: []

            tools.each { toolEntry ->
                toolEntry.each { toolName, toolInfo ->
                    result.add([moduleName, toolName, toolInfo])
                }
            }

            return result
        } catch (Exception e) {
            System.err.println("Warning: Could not convert meta.yml to topic format: ${e.message}")
            return []
        }
    }

    /**
     * Format citation text from citation data
     *
     * @param toolName Name of the tool
     * @param citationData Citation data map
     * @return Formatted citation string
     */
    private static String formatCitationFromData(String toolName, Map citationData) {
        def citation = toolName
        if (citationData.doi) {
            citation += " (DOI: ${citationData.doi})"
        } else if (citationData.description) {
            citation += " (${citationData.description})"
        }
        return citation
    }

    /**
     * Format bibliography entry from citation data
     *
     * @param toolName Name of the tool
     * @param citationData Citation data map
     * @return Formatted bibliography HTML
     */
    private static String formatBibliographyFromData(String toolName, Map citationData) {
        def author = citationData.author ?: ""
        def year = citationData.year ?: ""
        def title = citationData.title ?: toolName
        def journal = citationData.journal ?: ""
        def doi = citationData.doi ? "doi: ${citationData.doi}" : ""
        def url = citationData.homepage ?: ""

        def bibCitation = [author, year, title, journal, doi].findAll { it }.join(". ")
        if (url) {
            bibCitation += ". <a href='${url}'>${url}</a>"
        }

        return "<li>${bibCitation}</li>"
    }

    /**
     * Generate methods description text using collected citations
     * @param mqc_methods_yaml MultiQC methods YAML file
     * @param collectedCitations Map containing all tool citations from modules (optional)
     * @param meta Additional metadata (optional)
     * @return Formatted methods description HTML
     */
    static String methodsDescriptionText(File mqc_methods_yaml, Map collectedCitations = [:], Map meta = [:]) {
        // Convert to a named map so can be used as with familiar NXF ${workflow} variable syntax in the MultiQC YML file
        if (!meta) meta = [:]
        def session = (Session) nextflow.Nextflow.session
        if (!meta.containsKey("workflow")) {
            meta.workflow = session.getWorkflowMetadata()?.toMap() ?: [:]
        }
        if (!meta.containsKey("manifest_map")) {
            meta["manifest_map"] = session.getManifest()?.toMap() ?: [:]
        }
        // Pipeline DOI
        if (meta.manifest_map?.doi) {
            def temp_doi_ref = ""
            def manifest_doi = meta.manifest_map.doi.tokenize(",")
            manifest_doi.each { doi_ref ->
                temp_doi_ref += "(doi: <a href='https://doi.org/${doi_ref.replace('https://doi.org/', '').replace(' ', '')}'>${doi_ref.replace('https://doi.org/', '').replace(' ', '')}</a>), "
            }
            meta["doi_text"] = temp_doi_ref[0..-3]
        } else {
            meta["doi_text"] = ""
        }
        meta["nodoi_text"] = meta.manifest_map?.doi ? "" : "<li>If available, make sure to update the text to include the Zenodo DOI of version of the pipeline used. </li>"
        // Generate tool citations and bibliography if not already provided
        if (!meta.containsKey("tool_citations")) {
            meta["tool_citations"] = toolCitationText(collectedCitations)
        }
        if (!meta.containsKey("tool_bibliography")) {
            meta["tool_bibliography"] = toolBibliographyText(collectedCitations)
        }
        def engine = new groovy.text.SimpleTemplateEngine()
        def description_html = engine.createTemplate(mqc_methods_yaml.text).make(meta)
        return description_html.toString()
    }

    /**
     * Extract citation from meta.yml file for topic channel emission
     * Used by processes to emit citation data at runtime
     *
     * @param metaYmlPath Path to the module's meta.yml file (typically "${moduleDir}/meta.yml")
     * @return List in topic channel format [module_name, tool_name, citation_data] or empty list if error
     */
    static List getCitation(String metaYmlPath) {
        try {
            File metaFile = new File(metaYmlPath)
            if (!metaFile.exists()) {
                System.err.println("Warning: meta.yml not found at ${metaYmlPath}")
                return []
            }

            // Extract module name from path (e.g., "modules/nf-core/fastqc/meta.yml" -> "FASTQC")
            def moduleName = extractModuleNameFromPath(metaYmlPath)

            // Parse meta.yml and extract tools
            def yaml = new Yaml()
            Map meta
            metaFile.withInputStream { is ->
                meta = yaml.load(is)
            }

            def tools = meta?.tools ?: []
            def citations = []

            // Convert each tool to topic channel format
            tools.each { toolEntry ->
                toolEntry.each { toolName, toolInfo ->
                    if (toolInfo instanceof Map) {
                        citations << [moduleName, toolName, toolInfo]
                    }
                }
            }

            return citations

        } catch (Exception e) {
            System.err.println("Warning: Failed to extract citation from ${metaYmlPath}: ${e.message}")
            return []
        }
    }

    /**
     * Extract module name from meta.yml file path
     * Handles various path patterns commonly used in nf-core
     *
     * @param metaYmlPath Path to meta.yml file
     * @return Module name in uppercase format
     */
    private static String extractModuleNameFromPath(String metaYmlPath) {
        try {
            def pathParts = metaYmlPath.split('/')
            def metaIndex = pathParts.findLastIndexOf { it == 'meta.yml' }

            if (metaIndex > 0) {
                // Extract module name from parent directory
                def moduleName = pathParts[metaIndex - 1]
                return moduleName.toUpperCase()
            }

            // Fallback: extract from current directory or default
            def currentDir = new File('.').getAbsolutePath()
            def dirName = new File(currentDir).name
            return dirName.toUpperCase()

        } catch (Exception e) {
            return "UNKNOWN_MODULE"
        }
    }

    /**
     * Automatically collect citations from the 'citation' topic channel and generate citation text
     * This function works with the topic channel pattern where processes emit citations
     *
     * @param topicCitations List of citation data from topic channel (typically collected via channel.topic('citation').collect())
     * @return Formatted citation text ready for use in reports
     */
    static String autoToolCitationText(List topicCitations = []) {
        if (!topicCitations || topicCitations.isEmpty()) {
            return "No tools used in the workflow."
        }

        // Flatten and process topic citations - handle nested structures
        def allCitations = []
        topicCitations.each { item ->
            if (item instanceof List) {
                if (item.size() == 3) {
                    // This is already a [module, tool, data] tuple
                    allCitations.add(item)
                } else {
                    // This might be a list of tuples, flatten it
                    item.each { subItem ->
                        if (subItem instanceof List && subItem.size() == 3) {
                            allCitations.add(subItem)
                        }
                    }
                }
            }
        }

        // Process using existing logic
        def processedCitations = processCitationsFromTopic(allCitations)
        return toolCitationText(processedCitations)
    }

    /**
     * Automatically collect citations from the 'citation' topic channel and generate bibliography
     * This function works with the topic channel pattern where processes emit citations
     *
     * @param topicCitations List of citation data from topic channel (typically collected via channel.topic('citation').collect())
     * @return Formatted bibliography HTML ready for use in reports
     */
    static String autoToolBibliographyText(List topicCitations = []) {
        if (!topicCitations || topicCitations.isEmpty()) {
            return "No bibliography entries found."
        }

        // Flatten and process topic citations - handle nested structures
        def allCitations = []
        topicCitations.each { item ->
            if (item instanceof List) {
                if (item.size() == 3) {
                    // This is already a [module, tool, data] tuple
                    allCitations.add(item)
                } else {
                    // This might be a list of tuples, flatten it
                    item.each { subItem ->
                        if (subItem instanceof List && subItem.size() == 3) {
                            allCitations.add(subItem)
                        }
                    }
                }
            }
        }

        // Process using existing logic
        def processedCitations = processCitationsFromTopic(allCitations)
        return toolBibliographyText(processedCitations)
    }
}
