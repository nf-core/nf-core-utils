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
class ReferencesUtils {

    private Session session

    protected void init(Session session) {
        this.session = session
    }

    /**
     * Instance convenience method that reads params from the initialized Session
     * and delegates to the static getGenomeAttribute(Map, String) implementation.
     *
     * Usage (when ReferencesUtils has been initialized with a Session):
     *   def utils = new ReferencesUtils()
     *   utils.init(session)
     *   utils.getGenomeAttribute('fasta')
     */
    Object getGenomeAttribute(String attribute) {
        Map params = session?.getConfig()?.get('params') as Map
        // Fallback: some Nextflow Session implementations expose params directly
        if (!params) params = session?.params as Map
        if (params) return ReferencesUtils.getGenomeAttribute(params, attribute)
        return null
    }

    /**
     * Return the named attribute for the selected genome in params, or null.
     * Returns a single value (not wrapped in a List).
     *
     * Example:
     *   def fasta = ReferencesUtils.getGenomeAttribute(params, 'fasta')
     */
    static Object getGenomeAttribute(Map params, String attribute) {
        if (params == null) return null

        final Object genomesObj = params.get('genomes')
        final Object genomeKeyObj = params.get('genome')

        if (!(genomesObj instanceof Map) || !(genomeKeyObj instanceof String)) {
            return null
        }

        final Map genomes = (Map) genomesObj
        final String genomeKey = (String) genomeKeyObj

        if (!genomes.containsKey(genomeKey)) return null

        final Object genomeObj = genomes.get(genomeKey)
        if (!(genomeObj instanceof Map)) return null

        final Map genome = (Map) genomeObj
        if (!genome.containsKey(attribute)) return null

        return genome.get(attribute)
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
     * Load a reference datasheet from a file path (datasheet URL or local path).
     * The datasheet is expected to be in YAML format with a schema.
     *
     * @param datasheet Path to the datasheet YAML file
     * @param schemaPath Path to the schema JSON file (optional)
     * @return Map of reference attributes [attribute: value]
     */
    static Map loadReferenceDatasheet(String datasheet, String schemaPath = null) {
        if (!datasheet) return [:]

        // Use samplesheetToList if available (expects it to be in the calling context)
        // For now, return empty map as placeholder
        return [:]
    }

    /**
     * Select the first valid reference row from a list of datasheet rows.
     *
     * @param rows List of reference rows from the datasheet
     * @return The first non-null row, or an empty map
     */
    static Map selectReferenceRow(List rows) {
        for (row in rows) {
            if (row instanceof Map && !row.isEmpty()) {
                return row as Map
            }
        }
        return [:]
    }

    /**
     * Extract reference metadata from a datasheet row, filtering nulls and empty values.
     *
     * @param row A reference row from the datasheet
     * @return Map of metadata attributes
     */
    static Map referenceMeta(Map row) {
        if (!row) return [:]
        return row.findAll { _key, value -> value != null && value != '' }
    }

    /**
     * Build the datasheet URL for a genome key from the references datasheets path.
     * The catalogue tree is source/assembly.yml; keys are used verbatim (no dot transformation).
     *
     * @param genomeKey The genome key (e.g., 'Ensembl/GRCh38')
     * @param basePath The base URL for references datasheets (defaults to params.references_datasheets_path)
     * @return The datasheet URL
     */
    static String referencesDatasheetUrl(String genomeKey, String basePath = null) {
        if (!basePath) {
            basePath = System.getProperty('nextflow.params.references_datasheets_path') ?: ''
        }
        if (!basePath) {
            throw new IllegalArgumentException("references_datasheets_path is empty; cannot build datasheet URL for genome '${genomeKey}'")
        }
        basePath = basePath.toString().replaceAll(/\/+$/, '')
        return "${basePath}/${genomeKey}.yml"
    }
}
