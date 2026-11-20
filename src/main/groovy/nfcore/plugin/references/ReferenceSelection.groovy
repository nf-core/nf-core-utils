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

import nextflow.exception.AbortOperationException

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
     * Get a reference attribute from a loaded reference datasheet, falling back
     * to the genome configuration.
     *
     * @param args Named arguments: param (required), datasheet, genomeMap, genomeKey (default null)
     * @return The attribute value from the datasheet or genome config, or null
     */
    static Object getReferenceAttribute(Map args) {
        args = ReferenceDatasheet.checkArgs(args, 'getReferenceAttribute', ['param', 'datasheet', 'genomeMap', 'genomeKey'], ['param'])
        def value = args.datasheet ? args.datasheet[args.param] : null
        return value ?: new ReferenceSelection(args.genomeKey, args.genomeMap as Map).getAttribute(args.param)
    }

    /**
     * Abort if genome is set but is not a key of genomes, with the message of the nf-core template's genomeExistsError().
     *
     * @param args Named arguments: genome, genomes (all default null)
     * @throws AbortOperationException listing the available genome keys
     */
    static void genomeExistsError(Map args) {
        args = ReferenceDatasheet.checkArgs(args, 'genomeExistsError', ['genome', 'genomes'])
        Map genomes = args.genomes as Map
        if (ReferenceDatasheet.unset(args.genome) || !genomes || genomes.containsKey(args.genome)) {
            return
        }
        throw new AbortOperationException(
            "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "  Genome '${args.genome}' not found in any config files provided to the pipeline.\n" +
            "  Currently, the available genome keys are:\n" +
            "  ${genomes.keySet().join(', ')}\n" +
            "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
        )
    }

    /**
     * Extract reference metadata from parsed datasheet rows.
     *
     * @param args Named argument: rows (output of samplesheetToList)
     * @return Map of reference metadata attributes
     * @throws IllegalArgumentException if the rows are empty or span multiple genomes
     */
    static Map getReferenceMetadata(Map args) {
        args = ReferenceDatasheet.checkArgs(args, 'getReferenceMetadata', ['rows'], ['rows'])
        return ReferenceDatasheet.datasheetMeta(args.rows as List)
    }
}
