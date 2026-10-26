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
 * Concentrates reference-shape knowledge: genome lookup, attribute extraction,
 * basepath substitution, and null handling in a single deep module.
 *
 * Callers get a smaller interface than reaching into params.genomes directly.
 * {@link nfcore.plugin.ReferencesUtils} static methods remain for backward
 * compatibility.
 *
 * @see docs/adr/0001-prioritize-deep-modules-around-execution-context-and-reporting.md §6
 */
class ReferenceSelection {

    private final String genomeKey
    private final Map genomes

    ReferenceSelection(String genomeKey, Map genomes) {
        this.genomeKey = genomeKey
        this.genomes = genomes
    }

    /**
     * Create from a Nextflow params map containing 'genome' and 'genomes' keys.
     */
    static ReferenceSelection fromParams(Map params) {
        if (params == null) return new ReferenceSelection(null, null)
        return new ReferenceSelection(
            params.get('genome')?.toString(),
            params.get('genomes') as Map
        )
    }

    /**
     * Return the named attribute for the selected genome, or null.
     */
    Object getAttribute(String attribute) {
        if (genomeKey == null || genomes == null) return null
        def genome = genomes.get(genomeKey)
        if (!(genome instanceof Map)) return null
        return ((Map) genome).get(attribute)
    }

    /**
     * Return the named attribute with igenomes_base substitution applied.
     *
     * @param attribute Genome attribute name
     * @param basepath The igenomes base path to substitute
     * @return Resolved path string, or null if attribute not found
     */
    String resolveFile(String attribute, String basepath) {
        def value = getAttribute(attribute)
        if (value == null) return null
        return value.toString().replace('${params.igenomes_base}', basepath ?: '')
    }

    /**
     * Get reference files from a references list, using param override or
     * meta attribute with basepath substitution.
     *
     * Delegates to the same logic as {@link nfcore.plugin.ReferencesUtils#getReferencesFile}
     * but owned by this module.
     */
    List getReferencesFile(List<List> referencesList, String param, String attribute, String basepath) {
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
     * Get reference values from a references list, using param override or
     * meta attribute.
     *
     * Delegates to the same logic as {@link nfcore.plugin.ReferencesUtils#getReferencesValue}
     * but owned by this module.
     */
    List getReferencesValue(List<List> referencesList, String param, String attribute) {
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
     * Get a reference attribute from a loaded reference datasheet, falling back
     * to the genome configuration.
     *
     * @param args Named arguments: param, datasheet, genomeMap, genomeKey
     * @return The attribute value from the datasheet or genome config, or null
     */
    static Object getReferenceAttribute(Map args) {
        requireArgs(args, 'getReferenceAttribute', ['param', 'datasheet', 'genomeMap', 'genomeKey'])
        def value = args.datasheet ? args.datasheet[args.param] : null
        return value ?: new ReferenceSelection(args.genomeKey, args.genomeMap as Map).getAttribute(args.param)
    }

    /**
     * Determine which reference path to use: explicit --references or built from genome.
     *
     * @param args Named arguments: references, genome, igenomesIgnore, datasheetPath
     * @return The datasheet path to load, or null if neither references nor genome is provided
     * @throws IllegalArgumentException if both references and genome are provided
     */
    static String getReferencePath(Map args) {
        requireArgs(args, 'getReferencePath', ['references', 'genome', 'igenomesIgnore', 'datasheetPath'])
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
            return referenceDatasheet(genome, datasheetPath)
        }
        return null
    }

    /**
     * Extract reference metadata from parsed datasheet rows.
     *
     * @param args Named argument: datasheet (output of samplesheetToList)
     * @return Map of reference metadata attributes
     * @throws IllegalArgumentException if the datasheet is empty or has multiple records
     */
    static Map loadDatasheetFile(Map args) {
        requireArgs(args, 'loadDatasheetFile', ['datasheet'])
        return referenceMeta(selectReferenceRow(args.datasheet))
    }

    private static String referenceDatasheet(String genomeKey, String basePath) {
        if (!basePath) {
            throw new IllegalArgumentException("references_datasheets_path is empty; cannot build datasheet URL for genome '${genomeKey}'")
        }
        basePath = basePath.replaceAll(/\/+$/, '')
        return "${basePath}/${genomeKey}.yml"
    }

    private static Object selectReferenceRow(List rows) {
        if (!(rows instanceof List) || rows.isEmpty()) {
            throw new IllegalArgumentException("Reference datasheet did not contain any genome records")
        }
        if (rows.size() > 1) {
            def names = rows.collect { row -> referenceMeta(row).genome }.findAll { name -> name }
            throw new IllegalArgumentException("Reference datasheet has multiple genomes (${names.join(', ')}). Use a datasheet with one genome record.")
        }
        return rows[0]
    }

    private static Map referenceMeta(row) {
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
     * Reject missing named arguments instead of silently reading them as null.
     */
    private static void requireArgs(Map args, String method, List<String> keys) {
        def missing = args == null ? keys : keys.findAll { !args.containsKey(it) }
        if (missing) {
            throw new IllegalArgumentException("${method} is missing named argument(s): ${missing.join(', ')}")
        }
    }
}
