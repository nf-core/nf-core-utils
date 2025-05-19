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

import groovy.transform.CompileStatic
import nextflow.Session
import nextflow.plugin.extension.Function
import nextflow.plugin.extension.PluginExtensionPoint
import nfcore.plugin.nfcore.NfcoreVersionUtils


/**
 * Implements a custom function which can be imported by
 * Nextflow scripts.
 */
@CompileStatic
class NfUtilsExtension extends PluginExtensionPoint {

    private Session session

    @Override
    protected void init(Session session) {
        this.session = session
    }

    /**
     * Say hello to the given target.
     *
     * @param target
     */
    @Function
    void sayHello(String target) {
        println "Hello, ${target}!"
    }

    // --- Methods from NfcoreExtension ---
    /**
     * Generate methods description text for MultiQC report
     * @param workflow Workflow metadata
     * @param mqcMethodsYaml Path to MultiQC methods YAML template
     * @param modulePaths List of paths to module directories containing meta.yml files
     * @return HTML formatted methods description
     */
    // TODO This is still in the works
    // @Function
    // String methodsDescriptionText(nextflow.script.WorkflowMetadata workflow, String mqcMethodsYaml, List modulePaths) {
    //     return NfcoreExtension.methodsDescriptionText(workflow, mqcMethodsYaml, modulePaths)
    // }

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
    @Function
    void completionEmail(Map summaryParams, String email, String emailOnFail, boolean plaintextEmail, String outdir, boolean monochromeLogs, List multiqcReports) {
        nfcore.NfcoreNotificationUtils.completionEmail(summaryParams, email, emailOnFail, plaintextEmail, outdir, monochromeLogs, multiqcReports)
    }

    /**
     * Send completion summary to log
     * @param monochromeLogs Whether to use monochrome logs
     */
    @Function
    void completionSummary(boolean monochromeLogs) {
        nfcore.NfcoreNotificationUtils.completionSummary(monochromeLogs)
    }

    /**
     * Send Instant Messenger notification
     * @param summaryParams Map of parameter groups and their parameters
     * @param hookUrl Hook URL for the IM service
     */
    @Function
    void imNotification(Map summaryParams, String hookUrl) {
        nfcore.NfcoreNotificationUtils.imNotification(summaryParams, hookUrl)
    }

    /**
     * Check if the profile string is valid and warn about positional arguments
     * @param args The command line arguments (as List)
     */
    @Function
    void checkProfileProvided(List args) {
        nfcore.plugin.nfcore.NfcorePipelineUtils.checkProfileProvided(args)
    }

    /**
     * Check if a custom config or profile has been provided, logs a warning if not.
     * @return true if config is valid, false otherwise
     */
    @Function
    boolean checkConfigProvided() {
        return nfcore.plugin.nfcore.NfcorePipelineUtils.checkConfigProvided()
    }

    // --- Methods from ReferencesExtension ---
    /**
     * Get references file from a references list or parameters
     * @param referencesList The references list (List of [meta, _readme])
     * @param param The file parameter
     * @param attribute The attribute to look for in metadata
     * @param basepath The base path for igenomes
     * @return A list of reference files
     */
    @Function
    List getReferencesFile(List<List> referencesList, String param, String attribute, String basepath) {
        return ReferencesUtils.getReferencesFile(referencesList, param, attribute, basepath)
    }

    /**
     * Get references value from a references list or parameters
     * @param referencesList The references list (List of [meta, _readme])
     * @param param The value parameter
     * @param attribute The attribute to look for in metadata
     * @return A list of reference values
     */
    @Function
    List getReferencesValue(List<List> referencesList, String param, String attribute) {
        return ReferencesUtils.getReferencesValue(referencesList, param, attribute)
    }

    // --- Methods from NextflowPipelineExtension ---
    /**
     * Generate version string for a workflow
     * @param version The workflow version (optional)
     * @param commitId The workflow commit ID (optional)
     * @return A formatted version string
     */
    @Function
    String getWorkflowVersion(String version = null, String commitId = null) {
        return NfcoreVersionUtils.getWorkflowVersion(this.session, version, commitId)
    }

    /**
     * Dump pipeline parameters to a JSON file
     * @param outdir The output directory
     * @param params The pipeline parameters
     */
    @Function
    void dumpParametersToJSON(String outdir, Map params) {
        if (outdir == null) return
        java.nio.file.Path outdirPath = java.nio.file.Paths.get(outdir)
        NextflowPipelineUtils.dumpParametersToJSON(outdirPath, params)
    }

    /**
     * Check if conda channels are set up correctly
     * @return True if channels are set up correctly, false otherwise
     */
    @Function
    boolean checkCondaChannels() {
        return NextflowPipelineUtils.checkCondaChannels()
    }

}
