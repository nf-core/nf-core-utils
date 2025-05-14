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

import groovy.util.logging.Slf4j
import nextflow.Session
import nextflow.trace.TraceObserver
import org.yaml.snakeyaml.Yaml

/**
 * Implements an observer that allows implementing custom
 * logic on nextflow execution events.
 */
@Slf4j
class NfcorePipelineObserver implements TraceObserver {

    /**
     * Checks if a custom config or profile has been provided, logs a warning if not.
     * @param projectName The project name
     * @param config The config map (should have profile and configFiles)
     * @return true if config is valid, false otherwise
     */
    boolean checkConfigProvided(String projectName, Map config) {
        def profile = config?.get('profile') ?: 'standard'
        List configFiles = (config?.get('configFiles') instanceof List) ? (List)config.get('configFiles') : []
        if (profile == 'standard' && configFiles.size() <= 1) {
            log.warn("[${projectName}] You are attempting to run the pipeline without any custom configuration!\n\n" +
                "This will be dependent on your local compute environment but can be achieved via one or more of the following:\n" +
                "   (1) Using an existing pipeline profile e.g. `-profile docker` or `-profile singularity`\n" +
                "   (2) Using an existing nf-core/configs for your Institution e.g. `-profile crick` or `-profile uppmax`\n" +
                "   (3) Using your own local custom config e.g. `-c /path/to/your/custom.config`\n\n" +
                "Please refer to the quick start section and usage docs for the pipeline.\n ")
            return false
        }
        return true
    }

    /**
     * Checks if the profile string is valid and warns about positional arguments.
     *
     * @param profile The profile string (e.g. from workflow.profile)
     * @param commandLine The command line string
     */
    void checkProfileProvided(String profile, String commandLine) {
        if (profile?.endsWith(',')) {
            throw new IllegalArgumentException(
                "The `-profile` option cannot end with a trailing comma, please remove it and re-run the pipeline!\n" +
                "HINT: A common mistake is to provide multiple values separated by spaces e.g. `-profile test, docker`.\n"
            )
        }
        if (commandLine != null && !commandLine.isEmpty()) {
            // Split the command line into arguments
            def args = commandLine.split(/\s+/)
            // Find the first positional argument (not starting with '-')
            def positional = args.find { !it.startsWith('-') }
            if (positional) {
                log.warn(
                    "nf-core pipelines do not accept positional arguments. The positional argument `${positional}` has been detected.\n" +
                    "HINT: A common mistake is to provide multiple values separated by spaces e.g. `-profile test, docker`.\n"
                )
            }
        }
    }

    @Override
    void onFlowCreate(Session session) {
        def meta = session.getWorkflowMetadata()
        def config = session.config
        String projectName = null
        if (meta != null && meta.metaClass?.hasProperty(meta, 'projectName')) {
            projectName = meta.projectName
        }
        checkConfigProvided(projectName, config)
        checkProfileProvided(session.profile, session.commandLine)
        println "Pipeline is starting! 🚀"
    }

    @Override
    void onFlowComplete() {
        println "Pipeline complete! 👋"
    }

    /**
     * Generate workflow version string using session manifest
     */
    String getWorkflowVersion(Session session) {
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
    String processVersionsFromYAML(String yamlFile) {
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
    String workflowVersionToYAML(Session session) {
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
    String softwareVersionsToYAML(List<String> chVersions, Session session) {
        def uniqueVersions = chVersions.unique().collect { processVersionsFromYAML(it) }.unique()
        def workflowYaml = workflowVersionToYAML(session)
        return (uniqueVersions + workflowYaml).join("\n")
    }
} 