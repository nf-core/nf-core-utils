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

package nfcore.plugin.util

import nextflow.Session
import org.yaml.snakeyaml.Yaml

/**
 * Utility class for handling version information in nf-core pipelines
 */
class NfcoreVersionUtils {

    /**
     * Generate workflow version string using session manifest
     */
    static String getWorkflowVersion(Session session) {
        def manifest = session.getManifest()
        def version = manifest?.getVersion()
        def versionString = ""
        if (version) {
            def prefixV = version[0] != 'v' ? 'v' : ''
            versionString += "${prefixV}${version}"
        }
        return versionString
    }

    /**
     * Get software versions for pipeline from YAML string
     */
    static String processVersionsFromYAML(String yamlFile) {
        def yaml = new Yaml()
        def loaded = yaml.load(yamlFile)
        if (!(loaded instanceof Map)) return ''
        def versions = ((Map)loaded).collectEntries { k, v ->
            if (k instanceof String) {
                [k.tokenize(':')[-1], v]
            } else {
                [k, v]
            }
        }
        return yaml.dumpAsMap(versions).trim()
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
     * Get YAML string of software versions used in pipeline
     * @param chVersions List of YAML strings
     * @param session The Nextflow session
     * @return YAML string
     */
    static String softwareVersionsToYAML(List<String> chVersions, Session session) {
        def uniqueVersions = chVersions.unique().collect { processVersionsFromYAML(it) }.unique()
        def workflowYaml = workflowVersionToYAML(session)
        return (uniqueVersions + workflowYaml).join("\n")
    }
} 