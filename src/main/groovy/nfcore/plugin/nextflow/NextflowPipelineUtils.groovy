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

package nfcore.plugin.nextflow

import groovy.transform.CompileStatic
import org.yaml.snakeyaml.Yaml

import java.nio.file.Path

/**
 * Implements utility functions for Nextflow pipelines that were previously
 * available in the utils_nextflow_pipeline subworkflow.
 */
@CompileStatic
class NextflowPipelineUtils {


    /**
     * Dump pipeline parameters to a JSON file
     *
     * @param outdir The output directory
     * @param params The pipeline parameters
     */
    static void dumpParametersToJSON(Path outdir, Map params) {
        if (outdir == null) {
            System.err.println("WARN: Cannot dump parameters - output directory is null")
            return
        }

        if (params == null) {
            System.err.println("WARN: Cannot dump parameters - parameters map is null")
            return
        }

        try {
            def timestamp = new java.util.Date().format('yyyy-MM-dd_HH-mm-ss')
            def filename = "params_${timestamp}.json"
            
            // Create a temp file in the system temp directory
            def temp_pf = File.createTempFile("params_", ".json")
            def jsonStr = groovy.json.JsonOutput.toJson(params ?: [:])
            temp_pf.text = groovy.json.JsonOutput.prettyPrint(jsonStr)

            // Ensure pipeline_info directory exists
            def pipeline_info_dir = outdir.resolve("pipeline_info")
            java.nio.file.Files.createDirectories(pipeline_info_dir)

            nextflow.extension.FilesEx.copyTo(temp_pf.toPath(), pipeline_info_dir.resolve(filename))
            temp_pf.delete()
        } catch (Exception e) {
            System.err.println("ERROR: Failed to dump parameters to JSON: ${e.message}")
        }
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
            def process = "conda config --show channels".execute()
            process.waitFor()
            
            // Check if conda command was successful
            if (process.exitValue() != 0) {
                System.err.println("WARN: Conda command failed - conda may not be installed or available in PATH")
                return true  // Return true to not block pipeline if conda is not available
            }
            
            def result = process.text?.trim()
            if (result && result != "null") {
                def config = parser.load(result)
                if (config && config instanceof Map && config.containsKey('channels')) {
                    def rawChannels = config['channels']
                    if (rawChannels instanceof List) {
                        channels = rawChannels as List
                    } else if (rawChannels != null) {
                        channels = [rawChannels] as List
                    }
                }
            }
        }
        catch (Exception e) {
            System.err.println("WARN: Could not verify conda channel configuration: ${e.message}")
            return true  // Return true to not block pipeline on conda config check failures
        }

        // If channels is null or empty, return true to avoid blocking
        if (channels == null || channels.isEmpty()) {
            System.err.println("WARN: No conda channels found - conda configuration may not be set up")
            return true
        }

        // Check that all channels are present
        // This channel list is ordered by required channel priority.
        def required_channels_in_order = ['conda-forge', 'bioconda']
        def channels_as_set = (channels ?: []) as Set
        def required_as_set = required_channels_in_order as Set

        def channels_missing = !required_as_set.every { ch -> 
            channels_as_set.contains(ch)
        }

        // Check that they are in the right order
        def channel_subset = channels.findAll { ch -> 
            ch != null && ch in required_channels_in_order 
        } ?: []
        def channel_priority_violation = !channel_subset.equals(required_channels_in_order)

        if (channels_missing || channel_priority_violation) {
            System.err.println("""\
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                There is a problem with your Conda configuration!
                You will need to set-up the conda-forge and bioconda channels correctly.
                Please refer to https://bioconda.github.io/
                The observed channel order is
                ${channels ?: 'None'}
                but the following channel order is required:
                ${required_channels_in_order}
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~""".stripIndent(true))
            return false
        }

        return true
    }
} 