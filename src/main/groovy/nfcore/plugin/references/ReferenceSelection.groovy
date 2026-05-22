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
}
