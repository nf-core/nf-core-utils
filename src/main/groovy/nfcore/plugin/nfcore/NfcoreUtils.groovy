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

import nextflow.plugin.extension.PluginExtensionPoint
import nextflow.Session
import nextflow.script.ScriptBinding
import nextflow.script.WorkflowMetadata
import nfcore.plugin.util.NfcoreReportingUtils
import nfcore.plugin.util.NfcoreNotificationUtils
import nfcore.plugin.util.NfcoreVersionUtils
import nfcore.plugin.util.NfcoreConfigValidator

import groovy.util.logging.Slf4j

/**
 * Plugin extension providing nf-core utilities for Nextflow pipelines
 */
class NfcoreUtils {

    void onLoad(Session session) {
        // Called when the plugin is loaded
        log.debug "Loading nf-core plugin extension"
    }

    void onFlowComplete(ScriptBinding binding) {
        // Called when the workflow is complete
        log.debug "nf-core plugin: workflow completed"
    }

    /**
     * Send completion email
     * @param summaryParams Map of parameter groups and their parameters
     * @param email Email address to send to
     * @param emailOnFail Email address to send to on failure
     * @param plaintextEmail Whether to send plaintext email
     * @param outdir Output directory
     * @param monochromeLogs Whether to use monochrome logs
     * @param multiqcReports List of MultiQC report paths
     */
    static void completionEmail(Map summaryParams, String email, String emailOnFail, boolean plaintextEmail, String outdir, boolean monochromeLogs, List multiqcReports) {
        NfcoreNotificationUtils.completionEmail(
            summaryParams,
            email,
            emailOnFail,
            plaintextEmail,
            outdir,
            monochromeLogs,
            multiqcReports
        )
    }

    /**
     * Send completion summary to log
     * @param monochromeLogs Whether to use monochrome logs
     */
    static void completionSummary(boolean monochromeLogs) {
        NfcoreNotificationUtils.completionSummary(monochromeLogs)
    }

    /**
     * Send Instant Messenger notification
     * @param summaryParams Map of parameter groups and their parameters
     * @param hookUrl Hook URL for the IM service
     */
    static void imNotification(Map summaryParams, String hookUrl) {
        NfcoreNotificationUtils.imNotification(summaryParams, hookUrl)
    }

    /**
     * Initialize pipeline utilities
     * @param version Whether to display version and exit
     * @param dumpParameters Whether to dump parameters to JSON file
     * @param outdir Output directory
     * @param condaEnabled Whether conda/mamba is enabled
     */
    static void initializeNextflowPipeline(boolean version, boolean dumpParameters, String outdir, boolean condaEnabled) {
        NfcoreVersionUtils.printVersionAndExit(version)
        if (dumpParameters) {
            NfcoreReportingUtils.dumpParametersToJson(outdir)
        }
        NfcoreNotificationUtils.checkCondaChannels(condaEnabled)
    }

    /**
     * Initialize nf-core specific pipeline utilities
     * @param nextflowCliArgs List of positional nextflow CLI args
     */
    static void initializeNfcorePipeline(List nextflowCliArgs) {
        NfcoreConfigValidator.validateConfig(nextflowCliArgs)
    }
} 