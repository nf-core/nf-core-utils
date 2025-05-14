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
import groovy.transform.TypeCheckingMode
import nextflow.Session
import nextflow.plugin.extension.Function
import nextflow.plugin.extension.PluginExtensionPoint
import org.yaml.snakeyaml.Yaml
import groovy.text.GStringTemplateEngine
import nextflow.script.WorkflowMetadata
import groovyx.gpars.dataflow.DataflowReadChannel
import groovyx.gpars.dataflow.DataflowWriteChannel

/**
 * Implements utility functions for nf-core pipelines that were previously
 * available in the utils_nfcore_pipeline subworkflow.
 */
@CompileStatic
class NfCorePipelineExtension extends PluginExtensionPoint {

    // Store the session as a private field
    private Session session

    /**
     * Initialize the extension with the current session
     *
     * @param session The Nextflow session
     */
    @Override
    void init(Session session) {
        this.session = session
    }

    /**
     * Warn if a -profile or Nextflow config has not been provided to run the pipeline
     * 
     * @param workflow The Nextflow WorkflowMetadata object
     * @return True if config was provided, false otherwise
     */
    @Function
    @CompileStatic(TypeCheckingMode.SKIP)
    boolean checkConfigProvided(WorkflowMetadata workflow) {
        if (!workflow) {
            return false
        }
        def isStandardProfile = workflow.profile == 'standard'
        def hasSingleConfigFile = workflow.configFiles?.size() <= 1
        if (isStandardProfile && hasSingleConfigFile) {
            def name = workflow.manifest?.name ?: "unknown"
            System.err.println(
                "[${name}] You are attempting to run the pipeline without any custom configuration!\n\n" +
                "This will be dependent on your local compute environment but can be achieved via one or more of the following:\n" + 
                "   (1) Using an existing pipeline profile e.g. `-profile docker` or `-profile singularity`\n" + 
                "   (2) Using an existing nf-core/configs for your Institution e.g. `-profile crick` or `-profile uppmax`\n" + 
                "   (3) Using your own local custom config e.g. `-c /path/to/your/custom.config`\n\n" + 
                "Please refer to the quick start section and usage docs for the pipeline.\n "
            )
            return false
        }
        return true
    }

    /**
     * Exit pipeline if --profile contains spaces or trailing comma, and warn about positional arguments
     * 
     * @param workflow The Nextflow WorkflowMetadata object
     */
    @Function
    @CompileStatic(TypeCheckingMode.SKIP)
    void checkProfileProvided(WorkflowMetadata workflow) {
        if (!workflow) {
            return
        }
        if (workflow.profile?.endsWith(',')) {
            throw new RuntimeException(
                "The `-profile` option cannot end with a trailing comma, please remove it and re-run the pipeline!\n" + 
                "HINT: A common mistake is to provide multiple values separated by spaces e.g. `-profile test, docker`.\n"
            )
        }
        if (workflow.commandLine && workflow.commandLine instanceof List && workflow.commandLine.size() > 0 && workflow.commandLine[0]) {
            System.err.println(
                "nf-core pipelines do not accept positional arguments. The positional argument `${workflow.commandLine[0]}` has been detected.\n" + 
                "HINT: A common mistake is to provide multiple values separated by spaces e.g. `-profile test, docker`.\n"
            )
        }
    }

    /**
     * Generate workflow version string
     *
     * @param workflow The Nextflow WorkflowMetadata object
     * @return The workflow version string
     */
    @Function
    @CompileStatic(TypeCheckingMode.SKIP)
    String getWorkflowVersion(WorkflowMetadata workflow) {
        def version_string = "" as String
        if (workflow && workflow.manifest) {
            def manifest = workflow.manifest
            if (manifest.version) {
                def v = manifest.version.toString()
                def prefix_v = v[0] != 'v' ? 'v' : ''
                version_string += "${prefix_v}${v}"
            }
            if (workflow.commitId) {
                def git_shortsha = workflow.commitId.toString().substring(0, 7)
                version_string += "-g${git_shortsha}"
            }
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
    @CompileStatic(TypeCheckingMode.SKIP)
    String processVersionsFromYAML(String yaml_file) {
        def yaml = new org.yaml.snakeyaml.Yaml()
        def versions = yaml.load(yaml_file).collectEntries { k, v -> [k.toString().tokenize(':')[-1], v] }
        return yaml.dumpAsMap(versions).trim()
    }

    /**
     * Get workflow version for pipeline in YAML format
     *
     * @param workflow The Nextflow WorkflowMetadata object
     * @return The workflow version in YAML format
     */
    @Function
    @CompileStatic(TypeCheckingMode.SKIP)
    String workflowVersionToYAML(WorkflowMetadata workflow) {
        if (!workflow || !workflow.manifest) {
            return "Workflow: Not available\nNextflow: Not available"
        }
        def manifest = workflow.manifest
        def name = manifest.name ?: 'Unknown'
        def nextflowVersion = workflow.nextflow?.version ?: 'Unknown'
        return """
        Workflow:
            ${name}: ${getWorkflowVersion(workflow)}
            Nextflow: ${nextflowVersion}
        """.stripIndent().trim()
    }

    /**
     * Get channel of software versions used in pipeline in YAML format
     *
     * @param workflow The Nextflow WorkflowMetadata object
     * @param ch_versions Channel of versions
     * @return Channel of software versions in YAML format
     */
    @Function
    @CompileStatic(TypeCheckingMode.SKIP)
    Object softwareVersionsToYAML(WorkflowMetadata workflow, Object ch_versions) {
        return ch_versions
                .unique()
                .map { version -> processVersionsFromYAML(version.toString()) }
                .unique()
                .mix(nextflow.extension.CH.value(workflowVersionToYAML(workflow)))
    }

    /**
     * Get workflow summary for MultiQC
     *
     * @param workflow The Nextflow WorkflowMetadata object
     * @param summary_params The summary parameters
     * @return The workflow summary for MultiQC
     */
    @Function
    @CompileStatic(TypeCheckingMode.SKIP)
    String paramsSummaryMultiqc(WorkflowMetadata workflow, Map summary_params) {
        def summary_section = ''
        summary_params
            .keySet()
            .each { group ->
                def group_params = summary_params.get(group)
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
        if (!workflow || !workflow.manifest) {
            return "Error: No workflow metadata available"
        }
        def manifest = workflow.manifest
        def name = manifest.name?.toString() ?: 'unknown'
        def yaml_file_text = "id: '${name.replace('/', '-')}-summary'\n" as String
        yaml_file_text     += "description: ' - this information is collected when the pipeline is started.'\n"
        yaml_file_text     += "section_name: '${name} Workflow Summary'\n"
        yaml_file_text     += "section_href: 'https://github.com/${name}'\n"
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
    @CompileStatic(TypeCheckingMode.SKIP)
    Object getSingleReport(Object multiqc_reports) {
        if (multiqc_reports instanceof java.nio.file.Path) {
            return multiqc_reports
        } else if (multiqc_reports instanceof List) {
            if (multiqc_reports.size() == 0) {
                System.err.println("No reports found from process 'MULTIQC'")
                return null
            } else if (multiqc_reports.size() == 1) {
                return multiqc_reports.first()
            } else {
                System.err.println("Found multiple reports from process 'MULTIQC', will use only one")
                return multiqc_reports.first()
            }
        } else {
            return null
        }
    }

    /**
     * Print pipeline summary on completion
     *
     * @param workflow The Nextflow WorkflowMetadata object
     * @param monochrome_logs Whether to use monochrome logs
     */
    @Function
    @CompileStatic(TypeCheckingMode.SKIP)
    void completionSummary(WorkflowMetadata workflow, boolean monochrome_logs=true) {
        if (!workflow || !workflow.manifest || !workflow.stats) {
            System.out.println("Cannot generate completion summary - missing workflow data")
            return
        }
        def manifest = workflow.manifest
        def name = manifest.name?.toString() ?: 'Unknown'
        def stats = workflow.stats
        def colors = logColours(monochrome_logs) as Map
        if (stats.success) {
            if (stats.ignoredCount == 0) {
                System.out.println("-${colors.purple}[${name}]${colors.green} Pipeline completed successfully${colors.reset}-")
            }
            else {
                System.out.println("-${colors.purple}[${name}]${colors.yellow} Pipeline completed successfully, but with errored process(es) ${colors.reset}-")
            }
        }
        else {
            System.out.println("-${colors.purple}[${name}]${colors.red} Pipeline completed with errors${colors.reset}-")
        }
    }
}
