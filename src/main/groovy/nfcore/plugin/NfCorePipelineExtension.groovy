/*
 * Copyright 2025, nf-core team
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

import groovy.transform.CompileStatic
import nextflow.Session
import nextflow.plugin.extension.Function
import nextflow.plugin.extension.PluginExtensionPoint
import org.yaml.snakeyaml.Yaml
import groovy.text.GStringTemplateEngine

/**
 * Implements utility functions for nf-core pipelines that were previously
 * available in the utils_nfcore_pipeline subworkflow.
 */
@CompileStatic
class NfCorePipelineExtension extends PluginExtensionPoint {

    private Session session

    @Override
    protected void init(Session session) {
        this.session = session
    }

    /**
     * Warn if a -profile or Nextflow config has not been provided to run the pipeline
     *
     * @return Boolean indicating if a valid config was provided
     */
    @Function
    Boolean checkConfigProvided() {
        def valid_config = true
        if (session.workflowProfile == 'standard' && session.configFiles.size() <= 1) {
            def manifest = session.workflowMeta
            System.err.println(
                "[${manifest.name}] You are attempting to run the pipeline without any custom configuration!\n\n" + 
                "This will be dependent on your local compute environment but can be achieved via one or more of the following:\n" + 
                "   (1) Using an existing pipeline profile e.g. `-profile docker` or `-profile singularity`\n" + 
                "   (2) Using an existing nf-core/configs for your Institution e.g. `-profile crick` or `-profile uppmax`\n" + 
                "   (3) Using your own local custom config e.g. `-c /path/to/your/custom.config`\n\n" + 
                "Please refer to the quick start section and usage docs for the pipeline.\n "
            )
            valid_config = false
        }
        return valid_config
    }

    /**
     * Exit pipeline if --profile contains spaces
     *
     * @param nextflow_cli_args The CLI arguments
     */
    @Function
    void checkProfileProvided(List nextflow_cli_args) {
        if (session.workflowProfile.endsWith(',')) {
            throw new RuntimeException(
                "The `-profile` option cannot end with a trailing comma, please remove it and re-run the pipeline!\n" + 
                "HINT: A common mistake is to provide multiple values separated by spaces e.g. `-profile test, docker`.\n"
            )
        }
        if (nextflow_cli_args && nextflow_cli_args[0]) {
            System.err.println(
                "nf-core pipelines do not accept positional arguments. The positional argument `${nextflow_cli_args[0]}` has been detected.\n" + 
                "HINT: A common mistake is to provide multiple values separated by spaces e.g. `-profile test, docker`.\n"
            )
        }
    }

    /**
     * Generate workflow version string
     *
     * @return The workflow version string
     */
    @Function
    String getWorkflowVersion() {
        def version_string = "" as String
        def manifest = session.workflowMeta
        
        if (manifest.version) {
            def prefix_v = manifest.version.toString()[0] != 'v' ? 'v' : ''
            version_string += "${prefix_v}${manifest.version}"
        }

        if (manifest.commitId) {
            def git_shortsha = manifest.commitId.substring(0, 7)
            version_string += "-g${git_shortsha}"
        }

        return version_string
    }

    /**
     * Process versions from YAML
     *
     * @param yaml_file The YAML file
     * @return The processed versions
     */
    @Function
    String processVersionsFromYAML(String yaml_file) {
        def yaml = new org.yaml.snakeyaml.Yaml()
        def versions = yaml.load(yaml_file).collectEntries { k, v -> [k.tokenize(':')[-1], v] }
        return yaml.dumpAsMap(versions).trim()
    }

    /**
     * Get workflow version for pipeline in YAML format
     *
     * @return The workflow version in YAML format
     */
    @Function
    String workflowVersionToYAML() {
        def manifest = session.workflowMeta
        return """
        Workflow:
            ${manifest.name}: ${getWorkflowVersion()}
            Nextflow: ${session.nextflow.version}
        """.stripIndent().trim()
    }

    /**
     * Get channel of software versions used in pipeline in YAML format
     *
     * @param ch_versions Channel of versions
     * @return Channel of software versions in YAML format
     */
    @Function
    Object softwareVersionsToYAML(Object ch_versions) {
        return ch_versions.unique().map { version -> processVersionsFromYAML(version) }.unique().mix(nextflow.extension.CH.value(workflowVersionToYAML()))
    }

    /**
     * Get workflow summary for MultiQC
     *
     * @param summary_params The summary parameters
     * @return The workflow summary for MultiQC
     */
    @Function
    String paramsSummaryMultiqc(Map summary_params) {
        def summary_section = ''
        summary_params
            .keySet()
            .each { group ->
                def group_params = summary_params.get(group)
                // This gets the parameters of that particular group
                if (group_params) {
                    summary_section += "    <p style=\"font-size:110%\"><b>${group}</b></p>\n"
                    summary_section += "    <dl class=\"dl-horizontal\">\n"
                    group_params
                        .keySet()
                        .sort()
                        .each { param ->
                            summary_section += "        <dt>${param}</dt><dd><samp>${group_params.get(param) ?: '<span style=\"color:#999999;\">N/A</a>'}</samp></dd>\n"
                        }
                    summary_section += "    </dl>\n"
                }
            }

        def manifest = session.workflowMeta
        def yaml_file_text = "id: '${manifest.name.replace('/', '-')}-summary'\n" as String
        yaml_file_text     += "description: ' - this information is collected when the pipeline is started.'\n"
        yaml_file_text     += "section_name: '${manifest.name} Workflow Summary'\n"
        yaml_file_text     += "section_href: 'https://github.com/${manifest.name}'\n"
        yaml_file_text     += "plot_type: 'html'\n"
        yaml_file_text     += "data: |\n"
        yaml_file_text     += "${summary_section}"

        return yaml_file_text
    }

    /**
     * ANSII colours used for terminal logging
     *
     * @param monochrome_logs Whether to use monochrome logs
     * @return The color codes
     */
    @Function
    Map logColours(boolean monochrome_logs=true) {
        def colorcodes = [:] as Map

        // Reset / Meta
        colorcodes['reset']      = monochrome_logs ? '' : "\033[0m"
        colorcodes['bold']       = monochrome_logs ? '' : "\033[1m"
        colorcodes['dim']        = monochrome_logs ? '' : "\033[2m"
        colorcodes['underlined'] = monochrome_logs ? '' : "\033[4m"
        colorcodes['blink']      = monochrome_logs ? '' : "\033[5m"
        colorcodes['reverse']    = monochrome_logs ? '' : "\033[7m"
        colorcodes['hidden']     = monochrome_logs ? '' : "\033[8m"

        // Regular Colors
        colorcodes['black']  = monochrome_logs ? '' : "\033[0;30m"
        colorcodes['red']    = monochrome_logs ? '' : "\033[0;31m"
        colorcodes['green']  = monochrome_logs ? '' : "\033[0;32m"
        colorcodes['yellow'] = monochrome_logs ? '' : "\033[0;33m"
        colorcodes['blue']   = monochrome_logs ? '' : "\033[0;34m"
        colorcodes['purple'] = monochrome_logs ? '' : "\033[0;35m"
        colorcodes['cyan']   = monochrome_logs ? '' : "\033[0;36m"
        colorcodes['white']  = monochrome_logs ? '' : "\033[0;37m"

        // Bold
        colorcodes['bblack']  = monochrome_logs ? '' : "\033[1;30m"
        colorcodes['bred']    = monochrome_logs ? '' : "\033[1;31m"
        colorcodes['bgreen']  = monochrome_logs ? '' : "\033[1;32m"
        colorcodes['byellow'] = monochrome_logs ? '' : "\033[1;33m"
        colorcodes['bblue']   = monochrome_logs ? '' : "\033[1;34m"
        colorcodes['bpurple'] = monochrome_logs ? '' : "\033[1;35m"
        colorcodes['bcyan']   = monochrome_logs ? '' : "\033[1;36m"
        colorcodes['bwhite']  = monochrome_logs ? '' : "\033[1;37m"

        // Underline
        colorcodes['ublack']  = monochrome_logs ? '' : "\033[4;30m"
        colorcodes['ured']    = monochrome_logs ? '' : "\033[4;31m"
        colorcodes['ugreen']  = monochrome_logs ? '' : "\033[4;32m"
        colorcodes['uyellow'] = monochrome_logs ? '' : "\033[4;33m"
        colorcodes['ublue']   = monochrome_logs ? '' : "\033[4;34m"
        colorcodes['upurple'] = monochrome_logs ? '' : "\033[4;35m"
        colorcodes['ucyan']   = monochrome_logs ? '' : "\033[4;36m"
        colorcodes['uwhite']  = monochrome_logs ? '' : "\033[4;37m"

        // High Intensity
        colorcodes['iblack']  = monochrome_logs ? '' : "\033[0;90m"
        colorcodes['ired']    = monochrome_logs ? '' : "\033[0;91m"
        colorcodes['igreen']  = monochrome_logs ? '' : "\033[0;92m"
        colorcodes['iyellow'] = monochrome_logs ? '' : "\033[0;93m"
        colorcodes['iblue']   = monochrome_logs ? '' : "\033[0;94m"
        colorcodes['ipurple'] = monochrome_logs ? '' : "\033[0;95m"
        colorcodes['icyan']   = monochrome_logs ? '' : "\033[0;96m"
        colorcodes['iwhite']  = monochrome_logs ? '' : "\033[0;97m"

        // Bold High Intensity
        colorcodes['biblack']  = monochrome_logs ? '' : "\033[1;90m"
        colorcodes['bired']    = monochrome_logs ? '' : "\033[1;91m"
        colorcodes['bigreen']  = monochrome_logs ? '' : "\033[1;92m"
        colorcodes['biyellow'] = monochrome_logs ? '' : "\033[1;93m"
        colorcodes['biblue']   = monochrome_logs ? '' : "\033[1;94m"
        colorcodes['bipurple'] = monochrome_logs ? '' : "\033[1;95m"
        colorcodes['bicyan']   = monochrome_logs ? '' : "\033[1;96m"
        colorcodes['biwhite']  = monochrome_logs ? '' : "\033[1;97m"

        return colorcodes
    }

    /**
     * Return a single report from an object that may be a Path or List
     *
     * @param multiqc_reports The MultiQC reports
     * @return A single report
     */
    @Function
    Object getSingleReport(Object multiqc_reports) {
        def manifest = session.workflowMeta
        if (multiqc_reports instanceof java.nio.file.Path) {
            return multiqc_reports
        } else if (multiqc_reports instanceof List) {
            if (multiqc_reports.size() == 0) {
                System.err.println("[${manifest.name}] No reports found from process 'MULTIQC'")
                return null
            } else if (multiqc_reports.size() == 1) {
                return multiqc_reports.first()
            } else {
                System.err.println("[${manifest.name}] Found multiple reports from process 'MULTIQC', will use only one")
                return multiqc_reports.first()
            }
        } else {
            return null
        }
    }

    /**
     * Print pipeline summary on completion
     *
     * @param monochrome_logs Whether to use monochrome logs
     */
    @Function
    void completionSummary(boolean monochrome_logs=true) {
        def manifest = session.workflowMeta
        def colors = logColours(monochrome_logs) as Map
        if (session.stats.success) {
            if (session.stats.ignoredCount == 0) {
                System.out.println("-${colors.purple}[${manifest.name}]${colors.green} Pipeline completed successfully${colors.reset}-")
            }
            else {
                System.out.println("-${colors.purple}[${manifest.name}]${colors.yellow} Pipeline completed successfully, but with errored process(es) ${colors.reset}-")
            }
        }
        else {
            System.out.println("-${colors.purple}[${manifest.name}]${colors.red} Pipeline completed with errors${colors.reset}-")
        }
    }
} 