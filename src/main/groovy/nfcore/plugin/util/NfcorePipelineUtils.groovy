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

import groovy.util.logging.Slf4j
import nextflow.Session

/**
 * Utility functions for nf-core pipelines
 */
@Slf4j
class NfcorePipelineUtils {

    /**
     * Checks if a custom config or profile has been provided, logs a warning if not.
     * @param projectName The project name
     * @param config The config map (should have profile and configFiles)
     * @return true if config is valid, false otherwise
     */
    static boolean checkConfigProvided() {
        def configValidator = new NfcoreConfigValidator()
        def session = (Session) nextflow.Nextflow.session
        def meta = session.getWorkflowMetadata()
        def config = session.config
        String projectName = null
        if (meta != null && meta.metaClass?.hasProperty(meta, 'projectName')) {
            projectName = meta.projectName
        }
        return configValidator.checkConfigProvided(projectName, config)
    }

    /**
     * Checks if the profile string is valid and warns about positional arguments.
     * @param args The command line arguments
     */
    static void checkProfileProvided(args) {
        def configValidator = new NfcoreConfigValidator()
        def session = (Session) nextflow.Nextflow.session
        def profile = session.profile
        def commandLine = args ? args.join(' ') : null
        configValidator.checkProfileProvided(profile, commandLine)
    }

    /**
     * Generate workflow version string from session manifest
     * @return Version string
     */
    static String getWorkflowVersion() {
        def session = (Session) nextflow.Nextflow.session
        return NfcoreVersionUtils.getWorkflowVersion(session)
    }
    
    /**
     * Generate workflow summary for MultiQC
     * @param summaryParams Map of parameter groups and their parameters
     * @return YAML formatted string for MultiQC
     */
    static String paramsSummaryMultiqc(Map<String, Map<String, Object>> summaryParams) {
        def session = (Session) nextflow.Nextflow.session
        def manifest = session.getManifest()
        def workflowName = manifest?.getName() ?: 'unknown'
        
        def summarySection = ''
        summaryParams
            .keySet()
            .each { group ->
                def groupParams = summaryParams.get(group)
                // This gets the parameters of that particular group
                if (groupParams) {
                    summarySection += "    <p style=\"font-size:110%\"><b>${group}</b></p>\n"
                    summarySection += "    <dl class=\"dl-horizontal\">\n"
                    groupParams
                        .keySet()
                        .sort()
                        .each { param ->
                            summarySection += "        <dt>${param}</dt><dd><samp>${groupParams.get(param) ?: '<span style=\"color:#999999;\">N/A</a>'}</samp></dd>\n"
                        }
                    summarySection += "    </dl>\n"
                }
            }

        def yamlFileText = "id: '${workflowName.replace('/', '-')}-summary'\n" as String
        yamlFileText     += "description: ' - this information is collected when the pipeline is started.'\n"
        yamlFileText     += "section_name: '${workflowName} Workflow Summary'\n"
        yamlFileText     += "section_href: 'https://github.com/${workflowName}'\n"
        yamlFileText     += "plot_type: 'html'\n"
        yamlFileText     += "data: |\n"
        yamlFileText     += "${summarySection}"

        return yamlFileText
    }
} 