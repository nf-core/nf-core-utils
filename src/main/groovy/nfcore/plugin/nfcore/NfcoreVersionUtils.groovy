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
import org.yaml.snakeyaml.Yaml

/**
 * Utility class for handling version information in nf-core pipelines.
 * Supports both legacy versions.yml files and new topic channels approach.
 * 
 * This class is focused solely on version management and does not handle
 * citation-related functionality (see NfcoreCitationUtils for that).
 */
class NfcoreVersionUtils {

    /**
     * Generate workflow version string
     *
     * @param session The Nextflow session (if null, version must be provided)
     * @param version The workflow version (optional if session is provided)
     * @param commitId The workflow commit ID (optional)
     * @return A formatted version string
     */
    static String getWorkflowVersion(Session session = null, String version = null, String commitId = null) {
        // Get version from session if not explicitly provided
        if (session && !version) {
            def manifest = session.getManifest()
            version = manifest?.getVersion()
        }

        def versionString = ""
        if (version) {
            def prefixV = version[0] != 'v' ? 'v' : ''
            versionString += "${prefixV}${version}"
        }

        // Add git commit information if provided
        if (commitId) {
            def gitShortsha = commitId.substring(0, 7)
            versionString += "-g${gitShortsha}"
        }

        return versionString
    }

    /**
     * Parses a YAML string of software versions and flattens keys.
     * Example: "tool:foo: 1.0.0\nbar: 2.0.0" -> "foo: 1.0.0\nbar: 2.0.0"
     */
    static String processVersionsFromYAML(String yamlFile) {
        def yaml = new Yaml()
        def loaded = yaml.load(yamlFile)
        if (!(loaded instanceof Map)) return ''
        def versions = ((Map) loaded).collectEntries { k, v ->
            if (k instanceof String) {
                [k.tokenize(':')[-1], v]
            } else {
                [k, v]
            }
        }
        return yaml.dumpAsMap(versions).trim()
    }

    /**
     * Process version information from topic channel format
     * Handles the new eval syntax: [process, name, version]
     * 
     * @param topicData List containing [process, name, version] tuples
     * @return YAML string with processed versions
     */
    static String processVersionsFromTopic(List<List> topicData) {
        def versions = [:]
        topicData.each { tuple ->
            if (tuple.size() >= 3) {
                def process = tuple[0]
                def name = tuple[1]
                def version = tuple[2]
                
                // Extract tool name from process (remove module path prefix)
                def toolName = process.tokenize(':').last()
                versions[name] = version
            }
        }
        
        def yaml = new Yaml()
        def yamlString = yaml.dumpAsMap(versions)
        // Remove trailing newline for consistency
        return yamlString.trim()
    }

    /**
     * Get workflow version for pipeline as channel data
     * For use with topic channels
     */
    def workflowVersionToChannel() {
        return [
            ['Workflow', workflow.manifest.name, getWorkflowVersion()],
            ['Workflow', 'Nextflow', workflow.nextflow.version]
        ]
    }

    /**
     * Get workflow version for pipeline as YAML string
     */
    static String workflowVersionToYAML(Session session) {
        def manifest = session.getManifest()
        def workflowName = manifest?.getName() ?: 'unknown'
        def workflowVersion = getWorkflowVersion(session)
        def nextflowVersion = (session.config instanceof Map && session.config.nextflow instanceof Map && session.config.nextflow['version']) ? session.config.nextflow['version'] : 'unknown'
        return """
        Workflow:
            ${workflowName}: ${workflowVersion}
            Nextflow: ${nextflowVersion}
        """.stripIndent().trim()
    }

    /**
     * Combines a list of YAML strings (from Nextflow pipeline) into a single YAML string,
     * deduplicating entries and appending workflow version info.
     *
     * Usage pattern for piecemeal accumulation:
     *   1. Each process emits a YAML string (e.g., to a channel or list).
     *   2. Collect all YAMLs at the end (e.g., with `versions_ch.collect()` or accumulating in a list).
     *   3. Call this function with the collected list and the session.
     *
     * Example (Nextflow DSL2):
     *   versions_ch = Channel.create()
     *   // ... processes emit YAML strings to versions_ch ...
     *   workflow.onComplete {
     *     def all_versions = versions_ch.collect()
     *     def versions_yaml = NfcoreVersionUtils.softwareVersionsToYAML(all_versions, workflow.session)
     *     println versions_yaml
     *   }
     *
     * @param chVersions List of YAML strings (from Nextflow pipeline)
     * @param session The Nextflow session
     * @return Combined YAML string
     */
    static String softwareVersionsToYAML(List<String> chVersions, Session session) {
        def parsedVersions = chVersions.collect { processVersionsFromYAML(it) }
        def uniqueVersions = parsedVersions.findAll { it }.unique()
        def workflowYaml = workflowVersionToYAML(session)
        return ([*uniqueVersions, workflowYaml].join("\n")).trim()
    }

    /**
     * Helper to combine YAMLs from a list of version YAMLs.
     *
     * @param versionsList List of YAML strings
     * @param session The Nextflow session
     * @return Combined YAML string
     */
    static String softwareVersionsToYAMLFromChannel(List<String> versionsList, Session session) {
        return softwareVersionsToYAML(versionsList, session)
    }

    /**
     * Process versions from topic channels (new approach)
     * Supports both legacy versions.yml files and new topic channel format
     * 
     * @param topicVersions List of topic channel data [process, name, version]
     * @param legacyVersions List of legacy YAML version strings (optional)
     * @param session The Nextflow session
     * @return Combined YAML string with all versions
     */
    static String processVersionsFromTopicChannels(List<List> topicVersions, List<String> legacyVersions = [], Session session) {
        def combinedVersions = []
        
        // Process topic channel versions (new format)
        if (topicVersions) {
            def topicYaml = processVersionsFromTopic(topicVersions)
            if (topicYaml) {
                combinedVersions.add(topicYaml)
            }
        }
        
        // Process legacy YAML versions (old format)
        if (legacyVersions) {
            def parsedLegacy = legacyVersions.collect { processVersionsFromYAML(it) }
            combinedVersions.addAll(parsedLegacy.findAll { it })
        }
        
        // Add workflow version info
        def workflowYaml = workflowVersionToYAML(session)
        combinedVersions.add(workflowYaml)
        
        return combinedVersions.unique().join("\n").trim()
    }

    //
    // Get channel of software versions used in pipeline in YAML format
    // This method supports the new topic channel approach
    //
    def softwareVersionsChannelToYAML() {
        return Channel.topic('versions')
                .unique()
                .mix(workflowVersionToChannel())
                .map { process, name, version ->
                    [
                            (process.tokenize(':').last()): [
                                    (name): version
                            ]
                    ]
                }
    }

    /**
     * Get channel of software versions from topic channels
     * Supports both 'versions' and 'versions_file' topics for backward compatibility
     */
    def getAllVersionsFromTopics() {
        def versionsChannel = Channel.topic('versions').ifEmpty([])
        def versionsFileChannel = Channel.topic('versions_file').ifEmpty([])
        
        return versionsChannel
            .mix(versionsFileChannel)
            .mix(workflowVersionToChannel())
    }
}
