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
import nextflow.script.WorkflowMetadata
import nextflow.plugin.extension.Function
import nextflow.plugin.extension.PluginExtensionPoint
import org.yaml.snakeyaml.Yaml
import java.nio.file.Path

/**
 * Implements utility functions for Nextflow pipelines that were previously
 * available in the utils_nextflow_pipeline subworkflow.
 */
@CompileStatic
class NextflowPipelineExtension {

    private Session session

    protected void init(Session session) {
        this.session = session
    }

    /**
     * Generate version string for a workflow
     *
     * @param manifestVersion The workflow version from manifest
     * @param commitId The workflow commit ID (optional)
     * @return A formatted version string
     */
    static String getWorkflowVersion(String manifestVersion, String commitId = null) {
        def version_string = "" as String
        if (manifestVersion) {
            def prefix_v = manifestVersion.charAt(0) != 'v' ? 'v' : ''
            version_string += "${prefix_v}${manifestVersion}"
        }

        if (commitId) {
            def git_shortsha = commitId.substring(0, 7)
            version_string += "-g${git_shortsha}"
        }

        return version_string
    }

    /**
     * Dump pipeline parameters to a JSON file
     *
     * @param outdir The output directory
     * @param params The pipeline parameters
     * @param launchDir The launch directory
     */
    static void dumpParametersToJSON(Path outdir, Map params, Path launchDir) {
        // Skip if outdir is null
        if (outdir == null) {
            return
        }
        
        def timestamp = new java.util.Date().format('yyyy-MM-dd_HH-mm-ss')
        def filename  = "params_${timestamp}.json"
        def temp_pf   = new File(launchDir.toString(), ".${filename}")
        def jsonStr   = groovy.json.JsonOutput.toJson(params)
        temp_pf.text  = groovy.json.JsonOutput.prettyPrint(jsonStr)

        nextflow.extension.FilesEx.copyTo(temp_pf.toPath(), "${outdir}/pipeline_info/params_${timestamp}.json")
        temp_pf.delete()
    }

    /**
     * Check if conda channels are set up correctly
     * 
     * @return True if channels are set up correctly, false otherwise
     */
    static boolean checkCondaChannels() {
        def parser = new Yaml()
        def channels = [] as List
        try {
            def result = "conda config --show channels".execute()?.text
            if (result) {
                def config = parser.load(result)
                if (config && config instanceof Map && config.containsKey('channels')) {
                    channels = config['channels'] as List ?: []
                }
            }
        }
        catch (Exception e) {
            System.err.println("WARN: Could not verify conda channel configuration: ${e.message}")
            return true
        }

        // If channels is null or empty, return true to avoid NPE
        if (channels == null || channels.isEmpty()) {
            return true
        }

        // Check that all channels are present
        // This channel list is ordered by required channel priority.
        def required_channels_in_order = ['conda-forge', 'bioconda']
        def channels_as_set = channels as Set ?: [] as Set
        def required_as_set = required_channels_in_order as Set

        def channels_missing = !required_as_set.every { ch -> channels_as_set.contains(ch) }

        // Check that they are in the right order
        def channel_subset = channels.findAll { ch -> ch in required_channels_in_order } ?: []
        def channel_priority_violation = !channel_subset.equals(required_channels_in_order)

        if (channels_missing | channel_priority_violation) {
            System.err.println("""\
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                There is a problem with your Conda configuration!
                You will need to set-up the conda-forge and bioconda channels correctly.
                Please refer to https://bioconda.github.io/
                The observed channel order is
                ${channels}
                but the following channel order is required:
                ${required_channels_in_order}
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
            """.stripIndent(true))
            return false
        }
        
        return true
    }
} 