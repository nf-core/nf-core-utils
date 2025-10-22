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
}
