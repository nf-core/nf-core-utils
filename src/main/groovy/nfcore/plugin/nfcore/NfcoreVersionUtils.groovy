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
import nextflow.Const
import org.yaml.snakeyaml.Yaml

/**
 * Utility class for handling version information in nf-core pipelines.
 * Supports both legacy versions.yml files and new topic channels approach.
 *
 * This class is focused solely on version management and does not handle
 * citation-related functionality (see NfcoreCitationUtils for that).
 *
 * <h2>Recommended API</h2>
 * Use {@link #collectVersions(Object, Session, Object)} as the primary entry point.
 * It handles all input types automatically:
 * <ul>
 *   <li>YAML strings (inline content)</li>
 *   <li>File paths (String, File, or Path objects)</li>
 *   <li>Topic tuples: [process, tool, version]</li>
 *   <li>Maps of tool->version</li>
 *   <li>Mixed lists containing any combination of the above</li>
 * </ul>
 *
 * <h2>Deprecated Methods</h2>
 * The following methods are deprecated in favour of collectVersions():
 * <ul>
 *   <li>{@link #softwareVersionsToYAML} - use collectVersions() instead</li>
 *   <li>{@link #processVersionsFromYAML} - use collectVersions() instead</li>
 *   <li>{@link #processVersionsFromTopic} - use collectVersions() instead</li>
 *   <li>{@link #processVersionsFromFile} - use collectVersions() instead</li>
 *   <li>{@link #processVersionsFromTopicChannels} - use collectVersions() instead</li>
 *   <li>{@link #processMixedVersionSources} - use collectVersions() instead</li>
 * </ul>
 */
@Slf4j(category = 'nextflow.nfcore.NfcoreVersionUtils')
class NfcoreVersionUtils {

    // =========================================================================
    // PRIMARY API
    // =========================================================================

    /**
     * Collect software versions from various input sources and merge into YAML format.
     *
     * This is the recommended entry point for version collection. It intelligently
     * handles multiple input types and merges them into a single YAML output.
     *
     * <h3>Supported Input Types</h3>
     * <ul>
     *   <li><b>String</b>: YAML content or file path (auto-detected)</li>
     *   <li><b>File/Path</b>: Reads YAML content from file</li>
     *   <li><b>List&lt;List&gt;</b>: Topic channel tuples [[process, tool, version], ...]</li>
     *   <li><b>List&lt;String&gt;</b>: File paths to versions.yml files</li>
     *   <li><b>Map</b>: Direct version data (tool->version or nested process blocks)</li>
     *   <li><b>Mixed List</b>: Any combination of the above types</li>
     * </ul>
     *
     * <h3>Example Usage</h3>
     * <pre>
     * // From collected channel (most common)
     * ch_versions.toList().map { versions ->
     *     NfcoreVersionUtils.collectVersions(versions, workflow.session)
     * }
     *
     * // From topic tuples
     * def tuples = [['FASTQC', 'fastqc', '0.12.1'], ['MULTIQC', 'multiqc', '1.14']]
     * NfcoreVersionUtils.collectVersions(tuples)
     *
     * // From YAML string
     * NfcoreVersionUtils.collectVersions("fastqc: 0.12.1\nmultiqc: 1.14")
     * </pre>
     *
     * @param input Single input or List of mixed inputs (see supported types above)
     * @param session Nextflow session (optional). If provided, workflow version is included.
     * @param nextflowVersion Override Nextflow version (optional). Auto-detected if null.
     * @return YAML string with merged versions, sorted alphabetically by process and tool
     */
    static String collectVersions(Object input, Session session = null, Object nextflowVersion = null) {
        def report = new SoftwareVersionReport()
        report.addInput(input)

        if (session) {
            report.addWorkflowVersion(session.getManifest()?.getName(), getWorkflowVersion(session), resolveNextflowVersion(session, nextflowVersion))
        }

        return report.renderYaml()
    }

    // =========================================================================
    // WORKFLOW INFO METHODS (not deprecated - different concern)
    // =========================================================================

    /**
     * The Nextflow version for the version reports: the one given, else the one of the config or NXF_VER, else the
     * version of the running Nextflow
     */
    static String resolveNextflowVersion(Session session, Object nextflowVersion = null) {
        return nextflowVersion?.toString() ?:
            session?.getConfig()?.get('nextflow')?.get('version')?.toString() ?:
            System.getenv('NXF_VER') ?:
            runningNextflowVersion() ?:
            'unknown'
    }

    /**
     * The version of the running Nextflow, null when it can't be read
     */
    static String runningNextflowVersion() {
        try {
            return nextflow.BuildInfo.version
        } catch (Throwable ignored) {
            return null
        }
    }

    /**
     * Generate workflow version string
     *
     * @param session The Nextflow session (if null, version must be provided)
     * @param version The workflow version (optional if session is provided)
     * @param commitId The workflow commit ID (optional)
     * @return A formatted version string
     */
    static String getWorkflowVersion(Session session = null, String version = null, String commitId = null) {
        // Get the version, and the commit of the run when neither is given, from the session
        if (session && !version) {
            if (!commitId) {
                commitId = session.getWorkflowMetadata()?.commitId
            }
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
            def gitShortsha = commitId.take(7)
            versionString += "-g${gitShortsha}"
        }

        return versionString
    }

    // =========================================================================
    // DEPRECATED METHODS - use collectVersions() instead
    // =========================================================================

    /**
     * Parses a YAML string of software versions and flattens keys.
     * Example: "tool:foo: 1.0.0\nbar: 2.0.0" -> "foo: 1.0.0\nbar: 2.0.0"
     *
     * @deprecated Use {@link #collectVersions(Object, Session, Object)} instead.
     */
    @Deprecated
    static String processVersionsFromYAML(String yamlFile) {
        log.debug("processVersionsFromYAML() is deprecated. Use collectVersions() instead.")
        return SoftwareVersionReport.flattenYamlKeys(yamlFile)
    }

    /**
     * Process version information from topic channel format
     * Handles the new eval syntax: [process, name, version]
     *
     * @param topicData List containing [process, name, version] tuples
     * @return YAML string with processed versions
     * @deprecated Use {@link #collectVersions(Object, Session, Object)} instead.
     */
    @Deprecated
    static String processVersionsFromTopic(List<List> topicData) {
        log.debug("processVersionsFromTopic() is deprecated. Use collectVersions() instead.")
        // Maintain flat tool->version map for compatibility
        Map<String, Object> versions = [:]
        topicData.each { tuple ->
            if (tuple.size() >= 3) {
                def tool = tuple[1]?.toString()
                def version = tuple[2]
                if (tool) versions[tool] = (version instanceof CharSequence) ? version.toString() : version
            }
        }

        def yaml = new Yaml()
        return yaml.dumpAsMap(versions).trim()
    }

    /**
     * Process version information from versions_file topic (legacy YAML files)
     * Handles the old versions.yml path output style
     *
     * @param versionsFileData List containing file paths to versions.yml files
     * @return YAML string with processed versions
     * @deprecated Use {@link #collectVersions(Object, Session, Object)} instead.
     */
    @Deprecated
    static String processVersionsFromFile(List<String> versionsFileData) {
        log.debug("processVersionsFromFile() is deprecated. Use collectVersions() instead.")
        def allVersions = [:]
        versionsFileData.each { filePath ->
            try {
                def file = new File(filePath)
                if (file.exists()) {
                    def yamlContent = file.text
                    def processedYaml = processVersionsFromYAML(yamlContent)
                    if (processedYaml) {
                        def yaml = new Yaml()
                        def parsed = yaml.load(processedYaml)
                        if (parsed instanceof Map) {
                            allVersions.putAll(parsed)
                        }
                    }
                }
            } catch (Exception e) {
                // Log warning but continue processing other files
                log.warn("Could not process versions file ${filePath}: ${e.message}")
            }
        }

        def yaml = new Yaml()
        return yaml.dumpAsMap(allVersions).trim()
    }

    /**
     * Get workflow version for pipeline as channel data
     * For use with topic channels
     *
     * @param session The Nextflow session
     * @return List of [process, name, version] tuples for workflow info
     */
    static List<List> workflowVersionToChannel(Session session) {
        def manifest = session.getManifest()
        def workflowName = manifest?.getName() ?: 'Workflow'
        def workflowVersion = getWorkflowVersion(session)
        return [
            ['Workflow', workflowName, workflowVersion],
            ['Workflow', 'Nextflow', resolveNextflowVersion(session)]
        ]
    }

    /**
     * Get workflow version for pipeline as YAML string
     */
    static String workflowVersionToYAML(Session session, Object nextflowVersion = null) {
        return collectVersions(null, session, nextflowVersion)
    }

    /**
     * Combines a list of YAML strings (from Nextflow pipeline) into a single YAML string,
     * deduplicating entries and appending workflow version info.
     *
     * @param chVersions List of mixed version entries from the pipeline (can be ArrayBag or other iterable)
     * @param session The Nextflow session
     * @param nextflowVersion Optional Nextflow version to include in output (can be VersionNumber or string)
     * @return Combined YAML string
     * @deprecated Use {@link #collectVersions(Object, Session, Object)} instead.
     */
    @Deprecated
    static String softwareVersionsToYAML(Object chVersions, Object session, Object nextflowVersion = null) {
        log.debug("softwareVersionsToYAML() is deprecated. Use collectVersions() instead.")
        return collectVersions(chVersions, session instanceof Session ? session : null, nextflowVersion)
    }

    /**
     * Helper to combine YAMLs from a list of version YAMLs.
     *
     * @param versionsList List of YAML strings
     * @param session The Nextflow session
     * @return Combined YAML string
     * @deprecated Use {@link #collectVersions(Object, Session, Object)} instead.
     */
    @Deprecated
    static String softwareVersionsToYAMLFromChannel(List<String> versionsList, Session session) {
        log.debug("softwareVersionsToYAMLFromChannel() is deprecated. Use collectVersions() instead.")
        return collectVersions(versionsList, session)
    }

    /**
     * Process versions from topic channels (new approach)
     * Supports both legacy versions.yml files and new topic channel format
     *
     * @param topicVersions List of topic channel data [process, name, version]
     * @param legacyVersions List of legacy YAML version strings (optional)
     * @param session The Nextflow session
     * @return Combined YAML string with all versions
     * @deprecated Use {@link #collectVersions(Object, Session, Object)} instead.
     */
    @Deprecated
    static String processVersionsFromTopicChannels(List<List> topicVersions, List<String> legacyVersions = [], Session session) {
        log.debug("processVersionsFromTopicChannels() is deprecated. Use collectVersions() instead.")
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



    /**
     * Process mixed topic channels and file-based versions
     * Combines data from both 'versions' and 'versions_file' topics
     *
     * @param topicVersions List of [process, name, version] from 'versions' topic
     * @param versionsFiles List of file paths from 'versions_file' topic
     * @param session The Nextflow session
     * @return Combined YAML string with all versions
     * @deprecated Use {@link #collectVersions(Object, Session, Object)} instead.
     */
    @Deprecated
    static String processMixedVersionSources(List<List> topicVersions, List<String> versionsFiles, Session session) {
        log.debug("processMixedVersionSources() is deprecated. Use collectVersions() instead.")
        def combinedVersions = []

        // Process new topic format
        if (topicVersions) {
            def topicYaml = processVersionsFromTopic(topicVersions)
            if (topicYaml && topicYaml != '{}') {
                combinedVersions.add(topicYaml)
            }
        }

        // Process legacy file format
        if (versionsFiles) {
            def fileYaml = processVersionsFromFile(versionsFiles)
            if (fileYaml && fileYaml != '{}') {
                combinedVersions.add(fileYaml)
            }
        }

        // Add workflow version info
        def workflowYaml = workflowVersionToYAML(session)
        combinedVersions.add(workflowYaml)

        return combinedVersions.unique().join("\n").trim()
    }

    /**
     * Convert legacy YAML string to new eval syntax format
     * Transforms old versions.yml content to [process, name, version] tuples
     *
     * @param yamlContent The YAML content as string
     * @param processName The process name to use (defaults to 'LEGACY')
     * @return List of [process, name, version] tuples
     */
    static List<List> convertLegacyYamlToEvalSyntax(String yamlContent, String processName = 'LEGACY') {
        try {
            def yaml = new Yaml()
            def parsed = yaml.load(yamlContent)
            def result = []

            if (parsed instanceof Map) {
                parsed.each { key, value ->
                    // Handle nested maps (like tool:foo: version)
                    if (key instanceof String && key.contains(':')) {
                        def toolName = key.tokenize(':').last()
                        result.add([processName, toolName, value?.toString()])
                    } else {
                        result.add([processName, key?.toString(), value?.toString()])
                    }
                }
            }

            return result
        } catch (Exception e) {
            log.warn("Could not convert legacy YAML to eval syntax: ${e.message}")
            return []
        }
    }

    /**
     * Generate YAML output from eval syntax data
     * Converts [process, name, version] tuples back to YAML format for reporting
     *
     * @param evalData List of [process, name, version] tuples
     * @param session The Nextflow session
     * @param includeWorkflow Whether to include workflow version info
     * @return YAML string suitable for MultiQC and reporting
     */
    static String generateYamlFromEvalSyntax(List<List> evalData, Session session, boolean includeWorkflow = true) {
        def versions = [:]

        // Process eval syntax data
        evalData.each { tuple ->
            if (tuple.size() >= 3) {
                def process = tuple[0]
                def name = tuple[1]
                def version = tuple[2]
                versions[name] = version
            }
        }

        def yamlParts = []

        // Add software versions
        if (versions) {
            def yaml = new Yaml()
            yamlParts.add(yaml.dumpAsMap(versions).trim())
        }

        // Add workflow info if requested
        if (includeWorkflow) {
            yamlParts.add(workflowVersionToYAML(session))
        }

        return yamlParts.join("\n").trim()
    }
}
