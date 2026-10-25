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

/**
 * Reference resolution helpers for use in pipeline main.nf scripts.
 * Provides pure helper functions that don't depend on Nextflow workflow context.
 *
 * Call these directly from pipelines:
 *   def url = nfcore.plugin.references.ReferenceHelpers.referencesDatasheetUrl('Ensembl/GRCh38')
 *   def row = nfcore.plugin.references.ReferenceHelpers.selectReferenceRow(rows)
 */
class ReferenceHelpers {

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

    /**
     * Select the first valid reference row from a list of datasheet rows.
     * Validates that the list is non-empty and has exactly one record.
     *
     * @param rows List of reference rows from the datasheet
     * @return The first row as a Map
     * @throws IllegalArgumentException if list is empty or has multiple records
     */
    static Map selectReferenceRow(List rows) {
        if (!(rows instanceof List) || rows.isEmpty()) {
            throw new IllegalArgumentException("Reference datasheet did not contain any genome records")
        }
        if (rows.size() > 1) {
            def names = rows.collect { row -> referenceMeta(row).genome }.findAll { name -> name }
            throw new IllegalArgumentException("Reference datasheet has multiple genomes (${names.join(', ')}). Use a datasheet with one genome record.")
        }
        return rows[0]
    }

    /**
     * Extract reference metadata from a datasheet row, filtering nulls and empty values.
     * Handles both Map and List [Map, ...] formats.
     *
     * @param row A reference row from the datasheet
     * @return Map of metadata attributes
     * @throws IllegalArgumentException if row is not a Map or List[Map]
     */
    static Map referenceMeta(row) {
        if (row instanceof Map) {
            return row.findAll { _key, value -> value != null && value != '' }
        }
        if (row instanceof List && row && row[0] instanceof Map) {
            Map meta = row[0] as Map
            return meta.findAll { _key, value -> value != null && value != '' }
        }
        throw new IllegalArgumentException("Reference datasheet record is not a genome mapping: ${row}")
    }

    /**
     * Get attribute from genome config file (params.genomes[params.genome][attribute]).
     *
     * @param attribute The attribute name (e.g., 'fasta', 'bwa')
     * @param params The Nextflow params map
     * @return The attribute value, or null if not found
     */
    static Object getGenomeAttribute(String attribute, Map params) {
        if (params.genomes && params.genome && params.genomes.containsKey(params.genome)) {
            if (params.genomes[params.genome].containsKey(attribute)) {
                return params.genomes[params.genome][attribute]
            }
        }
        return null
    }
}
