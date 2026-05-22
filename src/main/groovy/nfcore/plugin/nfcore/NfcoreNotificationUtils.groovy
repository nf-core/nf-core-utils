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
import nextflow.extension.FilesEx

import java.nio.file.Path

/**
 * Utility functions for nf-core pipeline notifications
 */
@Slf4j
class NfcoreNotificationUtils {

    // Reusable template engine instance to avoid creating multiple instances
    private static final groovy.text.GStringTemplateEngine TEMPLATE_ENGINE = new groovy.text.GStringTemplateEngine()

    /**
     * Process summary parameters from a Map, extracting nested group data
     * @param summary_params Map of summary parameters with nested groups
     * @return Processed summary map with flattened group data
     */
    private static Map processSummaryParams(Map summary_params) {
        def summary = [:]
        if (summary_params != null && summary_params instanceof Map) {
            summary_params
                    .keySet()
                    .sort()
                    .each { group ->
                        def groupData = summary_params[group]
                        if (groupData instanceof Map) {
                            summary << groupData
                        }
                    }
        }
        return summary
    }

    /**
     * ANSII colour codes used for terminal logging
     * @param monochrome_logs Boolean indicating whether to use monochrome logs
     * @return Map of colour codes
     */
    static Map logColours(boolean monochrome_logs = true) {
        def colorcodes = [:] as Map

        // Reset / Meta
        colorcodes['reset'] = monochrome_logs ? '' : "\033[0m"
        colorcodes['bold'] = monochrome_logs ? '' : "\033[1m"
        colorcodes['dim'] = monochrome_logs ? '' : "\033[2m"
        colorcodes['underlined'] = monochrome_logs ? '' : "\033[4m"
        colorcodes['blink'] = monochrome_logs ? '' : "\033[5m"
        colorcodes['reverse'] = monochrome_logs ? '' : "\033[7m"
        colorcodes['hidden'] = monochrome_logs ? '' : "\033[8m"

        // Regular Colors
        colorcodes['black'] = monochrome_logs ? '' : "\033[0;30m"
        colorcodes['red'] = monochrome_logs ? '' : "\033[0;31m"
        colorcodes['green'] = monochrome_logs ? '' : "\033[0;32m"
        colorcodes['yellow'] = monochrome_logs ? '' : "\033[0;33m"
        colorcodes['blue'] = monochrome_logs ? '' : "\033[0;34m"
        colorcodes['purple'] = monochrome_logs ? '' : "\033[0;35m"
        colorcodes['cyan'] = monochrome_logs ? '' : "\033[0;36m"
        colorcodes['white'] = monochrome_logs ? '' : "\033[0;37m"

        // Bold
        colorcodes['bblack'] = monochrome_logs ? '' : "\033[1;30m"
        colorcodes['bred'] = monochrome_logs ? '' : "\033[1;31m"
        colorcodes['bgreen'] = monochrome_logs ? '' : "\033[1;32m"
        colorcodes['byellow'] = monochrome_logs ? '' : "\033[1;33m"
        colorcodes['bblue'] = monochrome_logs ? '' : "\033[1;34m"
        colorcodes['bpurple'] = monochrome_logs ? '' : "\033[1;35m"
        colorcodes['bcyan'] = monochrome_logs ? '' : "\033[1;36m"
        colorcodes['bwhite'] = monochrome_logs ? '' : "\033[1;37m"

        // Underline
        colorcodes['ublack'] = monochrome_logs ? '' : "\033[4;30m"
        colorcodes['ured'] = monochrome_logs ? '' : "\033[4;31m"
        colorcodes['ugreen'] = monochrome_logs ? '' : "\033[4;32m"
        colorcodes['uyellow'] = monochrome_logs ? '' : "\033[4;33m"
        colorcodes['ublue'] = monochrome_logs ? '' : "\033[4;34m"
        colorcodes['upurple'] = monochrome_logs ? '' : "\033[4;35m"
        colorcodes['ucyan'] = monochrome_logs ? '' : "\033[4;36m"
        colorcodes['uwhite'] = monochrome_logs ? '' : "\033[4;37m"

        // High Intensity
        colorcodes['iblack'] = monochrome_logs ? '' : "\033[0;90m"
        colorcodes['ired'] = monochrome_logs ? '' : "\033[0;91m"
        colorcodes['igreen'] = monochrome_logs ? '' : "\033[0;92m"
        colorcodes['iyellow'] = monochrome_logs ? '' : "\033[0;93m"
        colorcodes['iblue'] = monochrome_logs ? '' : "\033[0;94m"
        colorcodes['ipurple'] = monochrome_logs ? '' : "\033[0;95m"
        colorcodes['icyan'] = monochrome_logs ? '' : "\033[0;96m"
        colorcodes['iwhite'] = monochrome_logs ? '' : "\033[0;97m"

        // Bold High Intensity
        colorcodes['biblack'] = monochrome_logs ? '' : "\033[1;90m"
        colorcodes['bired'] = monochrome_logs ? '' : "\033[1;91m"
        colorcodes['bigreen'] = monochrome_logs ? '' : "\033[1;92m"
        colorcodes['biyellow'] = monochrome_logs ? '' : "\033[1;93m"
        colorcodes['biblue'] = monochrome_logs ? '' : "\033[1;94m"
        colorcodes['bipurple'] = monochrome_logs ? '' : "\033[1;95m"
        colorcodes['bicyan'] = monochrome_logs ? '' : "\033[1;96m"
        colorcodes['biwhite'] = monochrome_logs ? '' : "\033[1;97m"

        return colorcodes
    }

    /**
     * Return a single report from an object that may be a Path or List.
     * Explicit-input overload: accepts workflowName directly, no global session read.
     *
     * @param multiqc_reports The reports object
     * @param workflowName Pipeline name for log messages
     * @return A single report Path or null
     * @see docs/adr/0001-prioritize-deep-modules-around-execution-context-and-reporting.md §2
     */
    static Path getSingleReport(def multiqc_reports, String workflowName) {
        def toPath = { it instanceof Path ? it : (it != null ? java.nio.file.Paths.get(it.toString()) : null) }

        if (multiqc_reports instanceof Path) {
            return multiqc_reports
        } else if (multiqc_reports instanceof CharSequence) {
            return toPath(multiqc_reports)
        } else if (multiqc_reports instanceof List) {
            if (multiqc_reports.size() == 0) {
                log.warn("[${workflowName}] No reports found from process 'MULTIQC'")
                return null
            } else if (multiqc_reports.size() == 1) {
                return toPath(multiqc_reports.first())
            } else {
                log.warn("[${workflowName}] Found multiple reports from process 'MULTIQC', will use only one")
                return toPath(multiqc_reports.first())
            }
        } else {
            return null
        }
    }

    /**
     * @deprecated Use {@link #getSingleReport(Object, String)} with explicit workflowName
     */
    @Deprecated
    static Path getSingleReport(def multiqc_reports) {
        def session = (Session) nextflow.Nextflow.session
        def manifest = session.getManifest()
        def workflowName = manifest?.getName() ?: 'unknown'
        return getSingleReport(multiqc_reports, workflowName)
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
                                boolean monochrome_logs = true, def multiqc_report = null) {
        def session = (Session) nextflow.Nextflow.session
        if (session == null) {
            log.error("Cannot send completion email - Nextflow session is null")
            return
        }

        def manifest = session.getManifest()
        def workflowName = manifest?.getName() ?: 'unknown'
        def config = session.config
        def wf = session.getWorkflowMetadata()

        // Set up the e-mail variables
        def subject = "[${workflowName}] Successful: ${wf?.runName ?: 'unknown'}"
        if (!wf?.success) {
            subject = "[${workflowName}] FAILED: ${wf?.runName ?: 'unknown'}"
        }

        def summary = processSummaryParams(summary_params)

        def misc_fields = [:]
        misc_fields['Date Started'] = wf?.start
        misc_fields['Date Completed'] = wf?.complete
        misc_fields['Pipeline script file path'] = wf?.scriptFile
        misc_fields['Pipeline script hash ID'] = wf?.scriptId
        if (wf?.repository) {
            misc_fields['Pipeline repository Git URL'] = wf.repository
        }
        if (wf?.commitId) {
            misc_fields['Pipeline repository Git Commit'] = wf.commitId
        }
        if (wf?.revision) {
            misc_fields['Pipeline Git branch/tag'] = wf.revision
        }
        misc_fields['Nextflow Version'] = wf?.nextflow?.version
        misc_fields['Nextflow Build'] = wf?.nextflow?.build
        misc_fields['Nextflow Compile Timestamp'] = wf?.nextflow?.timestamp

        def email_fields = [:]
        email_fields['version'] = NfcoreVersionUtils.getWorkflowVersion(session)
        email_fields['runName'] = wf?.runName
        email_fields['success'] = wf?.success
        email_fields['dateComplete'] = wf?.complete
        email_fields['duration'] = wf?.duration
        email_fields['exitStatus'] = wf?.exitStatus
        email_fields['errorMessage'] = (wf?.errorMessage ?: 'None')
        email_fields['errorReport'] = (wf?.errorReport ?: 'None')
        email_fields['commandLine'] = wf?.commandLine
        email_fields['projectDir'] = wf?.projectDir
        email_fields['summary'] = summary << misc_fields

        // On success try attach the multiqc report
        def mqc_report = getSingleReport(multiqc_report)

        // Check if we are only sending emails on failure
        def email_address = email
        if (!email && email_on_fail && !wf?.success) {
            email_address = email_on_fail
        }

        // Render the TXT template
        def email_txt = "Default email content"
        def email_html = "<html><body>Default email content</body></html>"

        try {
            def tf = new File("${wf?.projectDir}/assets/email_template.txt")
            if (tf.exists()) {
                def txt_template = TEMPLATE_ENGINE.createTemplate(tf).make(email_fields)
                email_txt = txt_template.toString()
            } else {
                log.warn("Email template not found: ${tf.absolutePath}")
            }

            // Render the HTML template
            def hf = new File("${wf?.projectDir}/assets/email_template.html")
            if (hf.exists()) {
                def html_template = TEMPLATE_ENGINE.createTemplate(hf).make(email_fields)
                email_html = html_template.toString()
            } else {
                log.warn("HTML email template not found: ${hf.absolutePath}")
            }
        } catch (Exception e) {
            log.warn("Failed to render email templates: ${e.message}")
        }

        // Render the sendmail template
        def max_multiqc_email_size = 0
        if (config?.params?.containsKey('max_multiqc_email_size')) {
            try {
                max_multiqc_email_size = (config.params.max_multiqc_email_size as nextflow.util.MemoryUnit).toBytes()
            } catch (Exception e) {
                log.warn("Failed to parse max_multiqc_email_size: ${e.message}")
            }
        }

        def smail_fields = [email: email_address, subject: subject, email_txt: email_txt, email_html: email_html, projectDir: "${wf?.projectDir}", mqcFile: mqc_report, mqcMaxSize: max_multiqc_email_size]
        def sendmail_html = email_html  // Fallback content

        try {
            def sf = new File("${wf?.projectDir}/assets/sendmail_template.txt")
            if (sf.exists()) {
                def sendmail_template = TEMPLATE_ENGINE.createTemplate(sf).make(smail_fields)
                sendmail_html = sendmail_template.toString()
            } else {
                log.warn("Sendmail template not found: ${sf.absolutePath}")
            }
        } catch (Exception e) {
            log.warn("Failed to render sendmail template: ${e.message}")
        }

        // Send the HTML e-mail
        def colors = logColours(monochrome_logs) as Map
        if (email_address) {
            try {
                if (plaintext_email) {
                    new org.codehaus.groovy.GroovyException('Send plaintext e-mail, not HTML')
                }
                // Try to send HTML e-mail using sendmail
                def sendmail_tf = new File(wf?.launchDir.toString(), ".sendmail_tmp.html")
                sendmail_tf.withWriter { w -> w << sendmail_html }
                ['sendmail', '-t'].execute() << sendmail_html
                log.info("-${colors.purple}[${workflowName}]${colors.green} Sent summary e-mail to ${email_address} (sendmail)-")
            }
            catch (Exception msg) {
                log.debug(msg.toString())
                log.debug("Trying with mail instead of sendmail")
                // Catch failures and try with plaintext
                def mail_cmd = ['mail', '-s', subject, '--content-type=text/html', email_address]
                mail_cmd.execute() << email_html
                log.info("-${colors.purple}[${workflowName}]${colors.green} Sent summary e-mail to ${email_address} (mail)-")
            }
        }

        // Write summary e-mail HTML to a file
        if (outdir != null) {
            try {
                def output_hf = new File(wf?.launchDir.toString(), ".pipeline_report.html")
                output_hf.withWriter { w -> w << email_html }

                // Ensure pipeline_info directory exists
                def pipeline_info_dir = new File("${outdir}/pipeline_info")
                pipeline_info_dir.mkdirs()

                FilesEx.copyTo(output_hf.toPath(), "${outdir}/pipeline_info/pipeline_report.html")
                output_hf.delete()

                // Write summary e-mail TXT to a file
                def output_tf = new File(wf?.launchDir.toString(), ".pipeline_report.txt")
                output_tf.withWriter { w -> w << email_txt }
                FilesEx.copyTo(output_tf.toPath(), "${outdir}/pipeline_info/pipeline_report.txt")
                output_tf.delete()
            } catch (Exception e) {
                log.warn("Failed to write email report files: ${e.message}")
            }
        } else {
            log.warn("Cannot write email reports - output directory is null")
        }
    }

    /**
     * Print pipeline summary on completion.
     * Explicit-input overload: accepts domain facts directly, no global session read.
     *
     * @param workflowName Pipeline name for log messages
     * @param success Whether the pipeline succeeded
     * @param ignoredCount Number of errored/ignored processes (0 = fully clean)
     * @param monochrome_logs Whether to use monochrome logs
     * @see docs/adr/0001-prioritize-deep-modules-around-execution-context-and-reporting.md §2
     */
    static void completionSummary(String workflowName, boolean success, int ignoredCount, boolean monochrome_logs = true) {
        def colors = logColours(monochrome_logs) as Map
        if (success) {
            if (ignoredCount == 0) {
                log.info("-${colors.purple}[${workflowName}]${colors.green} Pipeline completed successfully${colors.reset}-")
            } else {
                log.info("-${colors.purple}[${workflowName}]${colors.yellow} Pipeline completed successfully, but with errored process(es) ${colors.reset}-")
            }
        } else {
            log.info("-${colors.purple}[${workflowName}]${colors.red} Pipeline completed with errors${colors.reset}-")
        }
    }

    /**
     * Print pipeline summary on completion.
     * Reads global Nextflow session for backward compatibility.
     * @param monochrome_logs Whether to use monochrome logs
     * @deprecated Use {@link #completionSummary(String, boolean, int, boolean)} with explicit inputs
     */
    @Deprecated
    static void completionSummary(boolean monochrome_logs = true) {
        def session = (Session) nextflow.Nextflow.session
        def manifest = session.getManifest()
        def workflowName = manifest?.getName() ?: 'unknown'
        def statsObserver = session.getStatsObserver()
        def stats = statsObserver ? statsObserver.getStats() : null
        def ignoredCount = (stats != null) ? stats.getIgnoredCount() : 0
        completionSummary(workflowName, session.success, ignoredCount, monochrome_logs)
    }

    /**
     * Construct and send a notification to a web server as JSON e.g. Microsoft Teams and Slack
     * @param summary_params Map of summary parameters
     * @param hook_url Webhook URL
     */
    static void imNotification(Map summary_params, String hook_url) {
        def session = (Session) nextflow.Nextflow.session
        if (session == null) {
            log.error("Cannot send IM notification - Nextflow session is null")
            return
        }

        if (hook_url == null || hook_url.trim().isEmpty()) {
            log.warn("Cannot send IM notification - hook URL is null or empty")
            return
        }

        def wf = session.getWorkflowMetadata()
        def summary = processSummaryParams(summary_params)

        def misc_fields = [:]
        misc_fields['start'] = wf?.start
        misc_fields['complete'] = wf?.complete
        misc_fields['scriptfile'] = wf?.scriptFile
        misc_fields['scriptid'] = wf?.scriptId
        if (wf?.repository) {
            misc_fields['repository'] = wf.repository
        }
        if (wf?.commitId) {
            misc_fields['commitid'] = wf.commitId
        }
        if (wf?.revision) {
            misc_fields['revision'] = wf.revision
        }
        misc_fields['nxf_version'] = wf?.nextflow?.version
        misc_fields['nxf_build'] = wf?.nextflow?.build
        misc_fields['nxf_timestamp'] = wf?.nextflow?.timestamp

        def msg_fields = [:]
        msg_fields['version'] = NfcoreVersionUtils.getWorkflowVersion(session)
        msg_fields['runName'] = wf?.runName
        msg_fields['success'] = wf?.success
        msg_fields['dateComplete'] = wf?.complete
        msg_fields['duration'] = wf?.duration
        msg_fields['exitStatus'] = wf?.exitStatus
        msg_fields['errorMessage'] = (wf?.errorMessage ?: 'None')
        msg_fields['errorReport'] = (wf?.errorReport ?: 'None')
        msg_fields['commandLine'] = wf?.commandLine?.replaceFirst(/ +--hook_url +[^ ]+/, "") ?: ''
        msg_fields['projectDir'] = wf?.projectDir
        msg_fields['summary'] = summary << misc_fields

        // Render the JSON template
        def json_message = ""
        try {
            // Different JSON depending on the service provider
            // Defaults to "Adaptive Cards" (https://adaptivecards.io), except Slack which has its own format
            def json_path = hook_url.contains("hooks.slack.com") ? "slackreport.json" : "adaptivecard.json"
            def hf = new File("${wf?.projectDir}/assets/${json_path}")

            if (hf.exists()) {
                def json_template = TEMPLATE_ENGINE.createTemplate(hf).make(msg_fields)
                json_message = json_template.toString()
            } else {
                log.warn("IM notification template not found: ${hf.absolutePath}")
                // Create a basic JSON message as fallback
                json_message = groovy.json.JsonOutput.toJson([
                    text: "Pipeline ${msg_fields.runName ?: 'unknown'} ${msg_fields.success ? 'completed successfully' : 'failed'}"
                ])
            }
        } catch (Exception e) {
            log.warn("Failed to render IM notification template: ${e.message}")
            return
        }

        // POST
        try {
            def post = new URL(hook_url).openConnection()
            post.setRequestMethod("POST")
            post.setDoOutput(true)
            post.setRequestProperty("Content-Type", "application/json")
            post.getOutputStream().write(json_message.getBytes("UTF-8"))
            def postRC = post.getResponseCode()
            if (postRC < 200 || postRC >= 300) {
                def errorText = ""
                try {
                    errorText = post.getErrorStream()?.getText() ?: "Unknown error"
                } catch (Exception ignored) {
                    errorText = "Could not read error stream"
                }
                log.warn("IM notification failed with response code ${postRC}: ${errorText}")
            } else {
                log.info("IM notification sent successfully to ${hook_url}")
            }
        } catch (Exception e) {
            log.warn("Failed to send IM notification: ${e.message}")
        }
    }
}
