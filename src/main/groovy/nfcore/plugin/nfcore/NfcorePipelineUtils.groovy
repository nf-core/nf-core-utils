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

import java.nio.file.Path

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
        return NfcoreReportingUtils.paramsSummaryMultiqc(summaryParams)
    }
    
    /**
     * ANSII colour codes used for terminal logging
     * @param monochrome_logs Boolean indicating whether to use monochrome logs
     * @return Map of colour codes
     */
    static Map logColours(boolean monochrome_logs=true) {
        return NfcoreNotificationUtils.logColours(monochrome_logs)
    }
    
    /**
     * Return a single report from an object that may be a Path or List
     * @param multiqc_reports The reports object
     * @return A single report Path or null
     */
    static Path getSingleReport(def multiqc_reports) {
        return NfcoreNotificationUtils.getSingleReport(multiqc_reports)
    }
    
    /**
     * Construct and send completion email
     * @param summary_params Map of summary parameters
     * @param email Email address
     * @param email_on_fail Email address for failures only
     * @param plaintext_email Whether to send plaintext email
     * @param outdir Output directory
     * @param monochrome_logs Whether to use monochrome logs
     * @param multiqc_report MultiQC report file
     */
    static void completionEmail(Map summary_params, String email, String email_on_fail, 
                               boolean plaintext_email, String outdir, 
                               boolean monochrome_logs=true, def multiqc_report=null) {
        NfcoreNotificationUtils.completionEmail(summary_params, email, email_on_fail, 
                                             plaintext_email, outdir, monochrome_logs, multiqc_report)
    }
    
    /**
     * Print pipeline summary on completion
     * @param monochrome_logs Whether to use monochrome logs
     */
    static void completionSummary(boolean monochrome_logs=true) {
        NfcoreNotificationUtils.completionSummary(monochrome_logs)
    }
    
    /**
     * Construct and send a notification to a web server as JSON e.g. Microsoft Teams and Slack
     * @param summary_params Map of summary parameters
     * @param hook_url Webhook URL
     */
    static void imNotification(Map summary_params, String hook_url) {
        NfcoreNotificationUtils.imNotification(summary_params, hook_url)
    }
    
    /**
     * Create workflow summary template for MultiQC
     * @param summary Map of parameters
     * @param nfMetadataList List of metadata fields to include
     * @param results Map of pipeline results
     * @return Map with HTML summaries for MultiQC
     */
    static Map workflowSummaryMQC(Map summary, List nfMetadataList, Map results) {
        return NfcoreReportingUtils.workflowSummaryMQC(summary, nfMetadataList, results)
    }
    
    /**
     * Generate summary logs for each section of a pipeline
     * @param sections Map of section names with their log messages
     * @param monochrome Whether to use colors in logs
     * @return Map of colored section logs
     */
    static Map sectionLogs(Map sections, boolean monochrome=false) {
        return NfcoreReportingUtils.sectionLogs(sections, monochrome)
    }

    /**
     * Exit pipeline if incorrect --genome key provided
     */
    static void genomeExistsError(Map params) {
        if (params.genomes && params.genome && !params.genomes.containsKey(params.genome)) {
            def error_string = """
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  Genome '${params.genome}' not found in any config files provided to the pipeline.
  Currently, the available genome keys are:
  ${params.genomes.keySet().join(", ")}
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
""".stripIndent()
            throw new IllegalArgumentException(error_string)
        }
    }

    /**
     * Check and validate pipeline parameters
     */
    static void validateInputParameters(Map params) {
        genomeExistsError(params)
    }

    /**
     * Validate channels from input samplesheet
     * @param input List of [metas, fastqs]
     * @return [meta, fastqs] if valid, throws error otherwise
     */
    static List validateInputSamplesheet(List input) {
        def metas = input[0]
        def fastqs = input[1]
        // Check that multiple runs of the same sample are of the same datatype i.e. single-end / paired-end
        def endedness_ok = metas.collect { meta -> meta.single_end }.unique().size() == 1
        if (!endedness_ok) {
            throw new IllegalArgumentException("Please check input samplesheet -> Multiple runs of a sample must be of the same datatype i.e. single-end or paired-end: ${metas[0].id}")
        }
        return [metas[0], fastqs]
    }

    /**
     * Get attribute from genome config file e.g. fasta
     */
    static Object getGenomeAttribute(Map params, String attribute) {
        if (params.genomes && params.genome && params.genomes.containsKey(params.genome)) {
            if (params.genomes[params.genome].containsKey(attribute)) {
                return params.genomes[params.genome][attribute]
            }
        }
        return null
    }
} 