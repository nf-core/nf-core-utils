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
 * Utility functions for nf-core pipeline reporting
 */
@Slf4j
class NfcoreReportingUtils {

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
        yamlFileText += "description: ' - this information is collected when the pipeline is started.'\n"
        yamlFileText += "section_name: '${workflowName} Workflow Summary'\n"
        yamlFileText += "section_href: 'https://github.com/${workflowName}'\n"
        yamlFileText += "plot_type: 'html'\n"
        yamlFileText += "data: |\n"
        yamlFileText += "${summarySection}"

        return yamlFileText
    }

    /**
     * Create workflow summary template for MultiQC
     * @param summary Map of parameters
     * @param nfMetadataList List of metadata fields to include
     * @param results Map of pipeline results
     * @return Map with HTML summaries for MultiQC
     */
    static Map workflowSummaryMQC(Map summary, List nfMetadataList, Map results) {
        def session = (Session) nextflow.Nextflow.session
        def manifest = session.getManifest()
        def workflowName = manifest?.getName() ?: 'unknown'

        String reportHeaderMD = """
        # ${workflowName}

        Pipeline Summary
        ---------------
        """
        String reportSectionMD = """
        | Section | Description |
        |---------|-------------|
        """

        String reportMD = reportHeaderMD

        // Add workflow summary section
        reportMD += "## Workflow Summary\n\n"
        def summarySection = summary.collect { k, v -> " - **$k:** $v" }.join("\n")
        reportMD += summarySection + "\n\n"

        // Add Nextflow metadata
        reportMD += "## Nextflow Metadata\n\n"
        def metadataSection = nfMetadataList.collect { k ->
            def v = ""
            if (session.metaClass.hasProperty(session, k)) {
                v = session."$k"
            }
            return " - **$k:** " + (v ?: 'N/A')
        }.join("\n")
        reportMD += metadataSection + "\n\n"

        // Add results summary if provided
        if (results) {
            reportMD += "## Results Summary\n\n"
            reportMD += reportSectionMD
            def resultSection = results.collect { k, v -> "| $k | $v |" }.join("\n")
            reportMD += resultSection + "\n\n"
        }

        return [
                "workflow_summary_txt" : reportMD,
                "workflow_summary_html": "<pre>${reportMD}</pre>"
        ]
    }

    /**
     * Generate summary logs for each section of a pipeline
     * @param sections Map of section names with their log messages
     * @param monochrome Whether to use colors in logs
     * @return Map of colored section logs
     */
    static Map sectionLogs(Map sections, boolean monochrome = false) {
        def colors = NfcoreNotificationUtils.logColours(monochrome)
        def result = [:]

        sections.each { name, content ->
            def header = "-${colors.purple}[${name}]${colors.reset}- "
            def formattedContent = content.replaceAll(/\n/, "\n${header}")
            result[name] = "${header}${formattedContent}"
        }

        return result
    }
} 