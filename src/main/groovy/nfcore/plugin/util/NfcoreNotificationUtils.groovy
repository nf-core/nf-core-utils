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
import nextflow.extension.FilesEx
import java.nio.file.Path

/**
 * Utility functions for nf-core pipeline notifications
 */
@Slf4j
class NfcoreNotificationUtils {

    /**
     * ANSII colour codes used for terminal logging
     * @param monochrome_logs Boolean indicating whether to use monochrome logs
     * @return Map of colour codes
     */
    static Map logColours(boolean monochrome_logs=true) {
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
     * @param multiqc_reports The reports object
     * @return A single report Path or null
     */
    static Path getSingleReport(def multiqc_reports) {
        def session = (Session) nextflow.Nextflow.session
        def manifest = session.getManifest()
        def workflowName = manifest?.getName() ?: 'unknown'
        
        if (multiqc_reports instanceof Path) {
            return multiqc_reports
        } else if (multiqc_reports instanceof List) {
            if (multiqc_reports.size() == 0) {
                log.warn("[${workflowName}] No reports found from process 'MULTIQC'")
                return null
            } else if (multiqc_reports.size() == 1) {
                return multiqc_reports.first()
            } else {
                log.warn("[${workflowName}] Found multiple reports from process 'MULTIQC', will use only one")
                return multiqc_reports.first()
            }
        } else {
            return null
        }
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
        def session = (Session) nextflow.Nextflow.session
        def manifest = session.getManifest()
        def workflowName = manifest?.getName() ?: 'unknown'
        def config = session.config
        
        // Set up the e-mail variables
        def subject = "[${workflowName}] Successful: ${session.runName}"
        if (!session.success) {
            subject = "[${workflowName}] FAILED: ${session.runName}"
        }

        def summary = [:]
        summary_params
            .keySet()
            .sort()
            .each { group ->
                summary << summary_params[group]
            }

        def misc_fields = [:]
        misc_fields['Date Started']              = session.start
        misc_fields['Date Completed']            = session.complete
        misc_fields['Pipeline script file path'] = session.scriptFile
        misc_fields['Pipeline script hash ID']   = session.scriptId
        if (session.repository) {
            misc_fields['Pipeline repository Git URL']    = session.repository
        }
        if (session.commitId) {
            misc_fields['Pipeline repository Git Commit'] = session.commitId
        }
        if (session.revision) {
            misc_fields['Pipeline Git branch/tag']        = session.revision
        }
        misc_fields['Nextflow Version']           = session.nextflow.version
        misc_fields['Nextflow Build']             = session.nextflow.build
        misc_fields['Nextflow Compile Timestamp'] = session.nextflow.timestamp

        def email_fields = [:]
        email_fields['version']      = NfcoreVersionUtils.getWorkflowVersion(session)
        email_fields['runName']      = session.runName
        email_fields['success']      = session.success
        email_fields['dateComplete'] = session.complete
        email_fields['duration']     = session.duration
        email_fields['exitStatus']   = session.exitStatus
        email_fields['errorMessage'] = (session.errorMessage ?: 'None')
        email_fields['errorReport']  = (session.errorReport ?: 'None')
        email_fields['commandLine']  = session.commandLine
        email_fields['projectDir']   = session.projectDir
        email_fields['summary']      = summary << misc_fields

        // On success try attach the multiqc report
        def mqc_report = getSingleReport(multiqc_report)

        // Check if we are only sending emails on failure
        def email_address = email
        if (!email && email_on_fail && !session.success) {
            email_address = email_on_fail
        }

        // Render the TXT template
        def engine       = new groovy.text.GStringTemplateEngine()
        def tf           = new File("${session.projectDir}/assets/email_template.txt")
        def txt_template = engine.createTemplate(tf).make(email_fields)
        def email_txt    = txt_template.toString()

        // Render the HTML template
        def hf            = new File("${session.projectDir}/assets/email_template.html")
        def html_template = engine.createTemplate(hf).make(email_fields)
        def email_html    = html_template.toString()

        // Render the sendmail template
        def max_multiqc_email_size = (config.params?.containsKey('max_multiqc_email_size') ? config.params.max_multiqc_email_size : 0) as nextflow.util.MemoryUnit
        def smail_fields           = [email: email_address, subject: subject, email_txt: email_txt, email_html: email_html, projectDir: "${session.projectDir}", mqcFile: mqc_report, mqcMaxSize: max_multiqc_email_size.toBytes()]
        def sf                     = new File("${session.projectDir}/assets/sendmail_template.txt")
        def sendmail_template      = engine.createTemplate(sf).make(smail_fields)
        def sendmail_html          = sendmail_template.toString()

        // Send the HTML e-mail
        def colors = logColours(monochrome_logs) as Map
        if (email_address) {
            try {
                if (plaintext_email) {
                    new org.codehaus.groovy.GroovyException('Send plaintext e-mail, not HTML')
                }
                // Try to send HTML e-mail using sendmail
                def sendmail_tf = new File(session.launchDir.toString(), ".sendmail_tmp.html")
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
        def output_hf = new File(session.launchDir.toString(), ".pipeline_report.html")
        output_hf.withWriter { w -> w << email_html }
        FilesEx.copyTo(output_hf.toPath(), "${outdir}/pipeline_info/pipeline_report.html")
        output_hf.delete()

        // Write summary e-mail TXT to a file
        def output_tf = new File(session.launchDir.toString(), ".pipeline_report.txt")
        output_tf.withWriter { w -> w << email_txt }
        FilesEx.copyTo(output_tf.toPath(), "${outdir}/pipeline_info/pipeline_report.txt")
        output_tf.delete()
    }

    /**
     * Print pipeline summary on completion
     * @param monochrome_logs Whether to use monochrome logs
     */
    static void completionSummary(boolean monochrome_logs=true) {
        def session = (Session) nextflow.Nextflow.session
        def manifest = session.getManifest()
        def workflowName = manifest?.getName() ?: 'unknown'
        
        def colors = logColours(monochrome_logs) as Map
        def statsObserver = session.getStatsObserver()
        def stats = statsObserver ? statsObserver.getStats() : null
        if (session.success) {
            if (stats && stats.getIgnoredCount() == 0) {
                log.info("-${colors.purple}[${workflowName}]${colors.green} Pipeline completed successfully${colors.reset}-")
            }
            else {
                log.info("-${colors.purple}[${workflowName}]${colors.yellow} Pipeline completed successfully, but with errored process(es) ${colors.reset}-")
            }
        }
        else {
            log.info("-${colors.purple}[${workflowName}]${colors.red} Pipeline completed with errors${colors.reset}-")
        }
    }

    /**
     * Construct and send a notification to a web server as JSON e.g. Microsoft Teams and Slack
     * @param summary_params Map of summary parameters
     * @param hook_url Webhook URL
     */
    static void imNotification(Map summary_params, String hook_url) {
        def session = (Session) nextflow.Nextflow.session
        
        def summary = [:]
        summary_params
            .keySet()
            .sort()
            .each { group ->
                summary << summary_params[group]
            }

        def misc_fields = [:]
        misc_fields['start']          = session.start
        misc_fields['complete']       = session.complete
        misc_fields['scriptfile']     = session.scriptFile
        misc_fields['scriptid']       = session.scriptId
        if (session.repository) {
            misc_fields['repository'] = session.repository
        }
        if (session.commitId) {
            misc_fields['commitid']   = session.commitId
        }
        if (session.revision) {
            misc_fields['revision']   = session.revision
        }
        misc_fields['nxf_version']    = session.nextflow.version
        misc_fields['nxf_build']      = session.nextflow.build
        misc_fields['nxf_timestamp']  = session.nextflow.timestamp

        def msg_fields = [:]
        msg_fields['version']      = NfcoreVersionUtils.getWorkflowVersion(session)
        msg_fields['runName']      = session.runName
        msg_fields['success']      = session.success
        msg_fields['dateComplete'] = session.complete
        msg_fields['duration']     = session.duration
        msg_fields['exitStatus']   = session.exitStatus
        msg_fields['errorMessage'] = (session.errorMessage ?: 'None')
        msg_fields['errorReport']  = (session.errorReport ?: 'None')
        msg_fields['commandLine']  = session.commandLine.replaceFirst(/ +--hook_url +[^ ]+/, "")
        msg_fields['projectDir']   = session.projectDir
        msg_fields['summary']      = summary << misc_fields

        // Render the JSON template
        def engine       = new groovy.text.GStringTemplateEngine()
        // Different JSON depending on the service provider
        // Defaults to "Adaptive Cards" (https://adaptivecards.io), except Slack which has its own format
        def json_path     = hook_url.contains("hooks.slack.com") ? "slackreport.json" : "adaptivecard.json"
        def hf            = new File("${session.projectDir}/assets/${json_path}")
        def json_template = engine.createTemplate(hf).make(msg_fields)
        def json_message  = json_template.toString()

        // POST
        def post = new URL(hook_url).openConnection()
        post.setRequestMethod("POST")
        post.setDoOutput(true)
        post.setRequestProperty("Content-Type", "application/json")
        post.getOutputStream().write(json_message.getBytes("UTF-8"))
        def postRC = post.getResponseCode()
        if (!postRC.equals(200)) {
            log.warn(post.getErrorStream().getText())
        }
    }
}
