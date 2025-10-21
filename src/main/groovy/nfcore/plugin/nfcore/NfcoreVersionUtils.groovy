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
import nextflow.Const
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
     */
    static String processVersionsFromFile(List<String> versionsFileData) {
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
                System.err.println("Warning: Could not process versions file ${filePath}: ${e.message}")
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
        // Get Nextflow version from session config, fall back to environment variable
        def nextflowVersion = session?.getConfig()?.get('nextflow')?.get('version') ?: System.getenv('NXF_VER') ?: 'unknown'
        return [
            ['Workflow', workflowName, workflowVersion],
            ['Workflow', 'Nextflow', nextflowVersion]
        ]
    }

    /**
     * Get workflow version for pipeline as YAML string
     */
    static String workflowVersionToYAML(Session session, Object nextflowVersion = null) {
        def manifest = session.getManifest()
        def workflowName = manifest?.getName() ?: 'unknown'
        def workflowVersion = getWorkflowVersion(session)
        // Use provided version, or try session config, or fall back to environment variable
        def nfVer = nextflowVersion?.toString() ?:
                    session?.getConfig()?.get('nextflow')?.get('version') ?:
                    System.getenv('NXF_VER') ?:
                    'unknown'
        return """
        Workflow:
            ${workflowName}: ${workflowVersion}
            Nextflow: ${nfVer}
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
     *     println versions_yamllet'
     *   }
     *
     * Accepts mixed inputs:
     * - YAML strings (legacy inline)
     * - File paths (String), File, or Path objects pointing to legacy versions.yml
     * - Topic tuples: [process, tool, version]
     * - Maps of tool->version
     *
     * @param chVersions List of mixed version entries from the pipeline (can be ArrayBag or other iterable)
     * @param session The Nextflow session
     * @param nextflowVersion Optional Nextflow version to include in output (can be VersionNumber or string)
     * @return Combined YAML string
     */
    static String softwareVersionsToYAML(Object chVersions, Object session, Object nextflowVersion = null) {
        // Convert to proper types
        List versionsList = (chVersions instanceof List) ? chVersions : chVersions?.toList() ?: []
        Session sess = (session instanceof Session) ? session : null
        def nfVersion = nextflowVersion

        def yaml = new Yaml()
        // Collect nested: process -> tools map
        Map<String, Map<String, Object>> merged = [:].withDefault { [:] as Map<String, Object> }

        Closure<String> cleanKey = { k ->
            if (k == null) return null
            def s = k.toString()
            return (s.contains(':')) ? s.tokenize(':').last() : s
        }

        Closure mergeProcessMap = { String processName, Map toolsMap ->
            if (!processName || !toolsMap) return
            toolsMap.each { tk, tv ->
                def toolKey = cleanKey(tk)
                if (toolKey) merged[processName][toolKey] = (tv instanceof CharSequence) ? tv.toString() : tv
            }
        }

        Closure mergeParsedYaml = { Map parsed ->
            if (!parsed) return
            // If any value is a Map, treat as process blocks
            def hasNested = parsed.values().any { it instanceof Map }
            if (hasNested) {
                parsed.each { pk, pv ->
                    if (pv instanceof Map) {
                        def procName = pk?.toString()
                        mergeProcessMap(procName, pv as Map)
                    } else if (pk) {
                        // Top-level scalar, park under 'Software'
                        mergeProcessMap('Software', [(pk.toString()): pv])
                    }
                }
            } else {
                // Flat tool->version map, park under 'Software'
                mergeProcessMap('Software', parsed)
            }
        }

        Closure processEntry
        processEntry = { Object entry ->
            if (entry == null) return
            try {
                // Strings may be inline YAML or file paths
                if (entry instanceof CharSequence) {
                    def s = entry.toString().trim()
                    if (!s) return
                    def f = new File(s)
                    if (f.exists() && f.isFile()) {
                        def processed = processVersionsFromYAML(f.text)
                        if (processed) {
                            def map = yaml.load(processed)
                            if (map instanceof Map) mergeParsedYaml(map as Map)
                        }
                    } else {
                        def processed = processVersionsFromYAML(s)
                        if (processed) {
                            def map = yaml.load(processed)
                            if (map instanceof Map) mergeParsedYaml(map as Map)
                        }
                    }
                }
                // File path like objects
                else if (entry instanceof File) {
                    if (entry.exists()) {
                        def processed = processVersionsFromYAML(entry.text)
                        if (processed) {
                            def map = yaml.load(processed)
                            if (map instanceof Map) mergeParsedYaml(map as Map)
                        }
                    }
                }
                // Handle Nextflow Path or other Path-like types
                else if (entry.getClass().getName().toLowerCase().endsWith('.path')) {
                    def f = new File(entry.toString())
                    if (f.exists() && f.isFile()) {
                        def processed = processVersionsFromYAML(f.text)
                        if (processed) {
                            def map = yaml.load(processed)
                            if (map instanceof Map) mergeParsedYaml(map as Map)
                        }
                    }
                }
                // Topic tuple(s)
                else if (entry instanceof List || entry.getClass().isArray()) {
                    def list = (entry instanceof List) ? (List) entry : (entry as Object[]).toList()
                    // If we got a list of lists (e.g. collected channel), recurse
                    if (!list.isEmpty() && list[0] instanceof List) {
                        list.each { processEntry(it) }
                    } else if (list.size() >= 3) {
                        def procRaw = list[0]?.toString() ?: ''
                        def processName = procRaw.contains(':') ? procRaw.substring(procRaw.lastIndexOf(':') + 1) : procRaw
                        def tool = list[1]?.toString()
                        def version = list[2]
                        if (processName && tool) merged[processName][tool] = version instanceof CharSequence ? version.toString() : version
                    }
                }
                // Direct map
                else if (entry instanceof Map) {
                    mergeParsedYaml(entry as Map)
                }
                // Unknown type - try toString as YAML
                else {
                    def s = entry.toString()
                    if (s) {
                        def f = new File(s)
                        if (f.exists() && f.isFile()) {
                            def processed = processVersionsFromYAML(f.text)
                            if (processed) {
                                def map = yaml.load(processed)
                                if (map instanceof Map) mergeParsedYaml(map as Map)
                            }
                        } else {
                            def processed = processVersionsFromYAML(s)
                            if (processed) {
                                def map = yaml.load(processed)
                                if (map instanceof Map) mergeParsedYaml(map as Map)
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Warning: Could not process version entry ${entry}: ${e.message}")
            }
        }

        versionsList?.each { processEntry(it) }

        // Sort processes alphabetically, and within each process sort tools alphabetically
        Map<String, Map<String, Object>> sortedMerged = merged.sort().collectEntries { processName, toolsMap ->
            [(processName): toolsMap.sort()]
        }

        String versionsYaml = sortedMerged ? yaml.dumpAsMap(sortedMerged).trim() : ''
        def workflowYaml = workflowVersionToYAML(sess, nfVersion)
        return ([versionsYaml, workflowYaml].findAll { it && it != '{}' }.join("\n")).trim()
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



    /**
     * Process mixed topic channels and file-based versions
     * Combines data from both 'versions' and 'versions_file' topics
     *
     * @param topicVersions List of [process, name, version] from 'versions' topic
     * @param versionsFiles List of file paths from 'versions_file' topic
     * @param session The Nextflow session
     * @return Combined YAML string with all versions
     */
    static String processMixedVersionSources(List<List> topicVersions, List<String> versionsFiles, Session session) {
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
            System.err.println("Warning: Could not convert legacy YAML to eval syntax: ${e.message}")
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
