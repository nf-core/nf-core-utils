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
 * Parsing datasheets stays in pipelines because it depends on samplesheetToList()
 * from nf-schema, which is only available at workflow level.
 */
class ReferenceHelpers {

    /**
     * Get the datasheet path for a genome key from the references datasheets base path.
     * The catalogue tree is source/assembly.yml; keys are used verbatim (no dot transformation).
     * Path can be local (file://), S3 (s3://), HTTP (https://), or a filesystem path.
     *
     * @param genomeKey The genome key (e.g., 'Ensembl/GRCh38')
     * @param basePath The base path for references datasheets (local, S3, or HTTP)
     * @return The datasheet path
     * @throws IllegalArgumentException if basePath is empty
     */
    static String getReferenceDatasheet(Map args) {
        String genomeKey = args.genomeKey
        String basePath = args.basePath
        if (!basePath) {
            throw new IllegalArgumentException("references_datasheets_path is empty; cannot build datasheet URL for genome '${genomeKey}'")
        }
        basePath = basePath.replaceAll(/\/+$/, '')
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
    static Object selectReferenceRow(List rows) {
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
     * @param param The attribute name (e.g., 'fasta', 'bwa')
     * @param genomeMap The params.genomes map
     * @param genomeKey The params.genome name
     * @return The attribute value, or null if not found
     */
    static Object getGenomeAttribute(Map args) {
        String param = args.param
        Map genomeMap = args.genomeMap
        String genomeKey = args.genomeKey
        if (genomeMap && genomeKey && genomeMap.containsKey(genomeKey)) {
            if (genomeMap[genomeKey].containsKey(param)) {
                return genomeMap[genomeKey][param]
            }
        }
        return null
    }

    /**
     * Get a reference attribute from a loaded reference sheet, falling back to genome config.
     * Pure lookup function: tries sheet first, then genome config.
     *
     * @param param The attribute name (e.g., 'fasta', 'bwa')
     * @param datasheet The loaded reference datasheet (Map), or null if not loaded
     * @param genomeMap The params.genomes map
     * @param genomeKey The params.genome name
     * @return The attribute value from sheet or genome config, or null if not found
     */
    static Object getReferenceAttribute(Map args) {
        String param = args.param
        Map datasheet = args.datasheet
        Map genomeMap = args.genomeMap
        String genomeKey = args.genomeKey
        def value = datasheet ? datasheet[param] : null
        return value ?: getGenomeAttribute(param: param, genomeMap: genomeMap, genomeKey: genomeKey)
    }

    /**
     * Determine which reference path to use: explicit --references or built from genome.
     * Validates that both are not provided together (mutually exclusive).
     *
     * @param references The --references parameter (explicit datasheet path)
     * @param genome The --genome parameter (genome key)
     * @param igenomesIgnore The --igenomes_ignore flag
     * @param datasheetPath The --references_datasheets_path base path
     * @return The datasheet path to load, or null if neither references nor genome is provided
     * @throws IllegalArgumentException if both references and genome are provided
     */
    static String getReferencePath(Map args) {
        String references = args.references
        String genome = args.genome
        Boolean igenomesIgnore = args.igenomesIgnore
        String datasheetPath = args.datasheetPath
        genome = genome in [null, 'null'] ? null : genome

        if (references && genome) {
            throw new IllegalArgumentException("Use either --references or --genome, not both.")
        }

        if (references) {
            return references
        }
        if (genome && igenomesIgnore) {
            return getReferenceDatasheet(genomeKey: genome, basePath: datasheetPath)
        }

        return null
    }

    /**
     * Load and extract reference metadata from parsed datasheet rows.
     * Pure post-processor: selects first row and extracts non-null metadata.
     *
     * @param datasheet List of reference rows (output of samplesheetToList)
     * @return Map of reference metadata attributes
     * @throws IllegalArgumentException if datasheet is empty or has multiple records
     */
    static Map loadDatasheetFile(Map args) {
        return referenceMeta(selectReferenceRow(args.datasheet))
    }
}
