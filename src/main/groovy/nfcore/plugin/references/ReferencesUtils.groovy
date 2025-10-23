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

package nfcore.plugin.references

import groovy.transform.CompileStatic
import nextflow.Session
import nextflow.file.FileHelper

/**
 * Implements utility functions for handling reference files and values
 * that were previously available in the utils_references subworkflow.
 */
@CompileStatic
class ReferencesUtils {

    private Session session

    protected void init(Session session) {
        this.session = session
    }

    /**
     * Get references file from a references list or parameters
     *
     * @param referencesList The references list (List of [meta, _readme])
     * @param param The file parameter
     * @param attribute The attribute to look for in metadata
     * @param basepath The base path for igenomes
     * @return A list of reference files
     */
    static List getReferencesFile(List<List> referencesList, String param, String attribute, String basepath) {
        return referencesList.collect { pair ->
            Map meta = (Map) pair[0]
            if (param || (meta != null && meta[attribute])) {
                [meta.subMap(['id']), param ?: (meta[attribute]?.toString()?.replace('${params.igenomes_base}', basepath))]
            } else {
                null
            }
        }
    }

    /**
     * Get references value from a references list or parameters
     *
     * @param referencesList The references list (List of [meta, _readme])
     * @param param The value parameter
     * @param attribute The attribute to look for in metadata
     * @return A list of reference values
     */
    static List getReferencesValue(List<List> referencesList, String param, String attribute) {
        return referencesList.collect { pair ->
            Map meta = (Map) pair[0]
            if (param || (meta != null && meta[attribute])) {
                [meta.subMap(['id']), param ?: meta[attribute]]
            } else {
                null
            }
        }
    }

    /**
     * Update references file by replacing base paths in the YAML file
     *
     * @param options Named parameters map (can be first positional arg when using named params)
     * @param yamlReference The path to the YAML reference file
     * @return The updated file object (either staged copy or original)
     */
    def updateReferencesFile(Map options, def yamlReference) {
        // Support named parameters: basepathFinal/basepath_final and basepathToReplace/basepath_to_replace
        def basepathFinal = options.basepathFinal ?: options.basepath_final
        def basepathToReplace = options.basepathToReplace ?: options.basepath_to_replace

        def correctYamlFile = FileHelper.asPath(yamlReference.toString())

        if (!correctYamlFile || !correctYamlFile.exists()) {
            throw new IllegalArgumentException("YAML reference file does not exist: ${yamlReference}")
        }

        if (basepathFinal) {
            // Create a staged copy in a temporary location
            def stagedYamlFile = FileHelper.asPath("${session.workDir}/tmp/${UUID.randomUUID().toString()}.${correctYamlFile.getExtension()}")

            // Ensure parent directory exists
            stagedYamlFile.parent.mkdirs()

            // Copy the file
            correctYamlFile.copyTo(stagedYamlFile)
            correctYamlFile = stagedYamlFile

            // Use a local variable to accumulate changes
            def updatedYamlContent = correctYamlFile.text

            // Handle basepathToReplace as a list or convert to list
            def pathsToReplace = basepathToReplace instanceof List ? basepathToReplace : [basepathToReplace]
            pathsToReplace.each { basepathReplacement ->
                if (basepathReplacement) {
                    updatedYamlContent = updatedYamlContent.replace(basepathReplacement.toString(), basepathFinal.toString())
                }
            }
            correctYamlFile.text = updatedYamlContent
        }

        return correctYamlFile
    }

    /**
     * Update references file by replacing base paths in the YAML file (positional parameters version)
     *
     * @param yamlReference The path to the YAML reference file
     * @param basepathFinal The final base path to use as replacement (can be null, false, or empty)
     * @param basepathToReplace List of base paths to be replaced (can be null, false, or empty)
     * @return The updated file object (either staged copy or original)
     */
    def updateReferencesFile(def yamlReference, def basepathFinal, def basepathToReplace) {
        return updateReferencesFile([basepathFinal: basepathFinal, basepathToReplace: basepathToReplace], yamlReference)
    }
}
