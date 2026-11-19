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
@Slf4j(category = 'nextflow.nfcore.NfcoreReportingUtils')
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
                                    summarySection += "        <dt>${param}</dt><dd><samp>${groupParams.get(param) ?: '<span style=\"color:#999999;\">N/A</span>'}</samp></dd>\n"
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
     * Build a Markdown workflow summary: the workflow name, a "Workflow Summary" list,
     * a "Nextflow Metadata" list and, when results are given, a "Results Summary" table.
     * Reads the workflow name and the metadata values from the global Nextflow session.
     * Internal helper: not exported as a plugin function, so a pipeline cannot import it.
     * @param summary Map of label to value, listed under "Workflow Summary"
     * @param nfMetadataList Names of Nextflow Session properties to list; a name that is not a
     *        Session property, or whose value is Groovy-false, shows as N/A
     * @param results Map of label to value for the results table; the table is left out when null or empty
     * @return Map with two keys: workflow_summary_txt (the Markdown text) and
     *         workflow_summary_html (the same text inside a {@code <pre>} element)
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
     * Prefix every line of each section's text with "-[name]- ", the bracketed name in purple
     * unless monochrome. Returns the text; it does not log anything.
     * Internal helper: not exported as a plugin function, so a pipeline cannot import it.
     * @param sections Map of section name to its text
     * @param monochrome When true no colour codes are added (default: false)
     * @return Map of section name to the prefixed text
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
