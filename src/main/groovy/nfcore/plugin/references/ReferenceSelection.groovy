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

import groovy.json.JsonSlurper
import nextflow.exception.AbortOperationException
import nfcore.plugin.nextflow.NextflowPipelineUtils

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
        args = checkArgs(args, 'getReferenceAttribute', ['param', 'datasheet', 'genomeMap', 'genomeKey'], ['param'])
        def value = args.datasheet ? args.datasheet[args.param] : null
        return value ?: new ReferenceSelection(args.genomeKey, args.genomeMap as Map).getAttribute(args.param)
    }

    /**
     * Determine which reference path to use: explicit --references or built from genome.
     *
     * @param args Named arguments: references, genome, igenomesIgnore, datasheetPath (all default null)
     * @return The datasheet path to load, or null if neither references nor genome is provided
     * @throws IllegalArgumentException if both references and genome are provided
     */
    static String getReferencePath(Map args) {
        args = checkArgs(args, 'getReferencePath', ['references', 'genome', 'igenomesIgnore', 'datasheetPath'])
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
     * Resolve every reference param that getReferenceNames() lists for a references schema.
     * A param present in params, even null, wins. Otherwise the datasheet rows are the only
     * source when given, and the genomes config when not: a datasheet never falls back to config.
     * Without rows, aborts like genomeExistsError() when genome is not a key of genomes.
     *
     * @param args Named arguments: schema (required), rows (output of samplesheetToList), params, genome, genomes,
     *             ignore (passed to getReferenceNames())
     * @return Map of reference param name to resolved value
     * @throws AbortOperationException listing the available genome keys
     */
    static Map getReferences(Map args) {
        args = checkArgs(args, 'getReferences', ['schema', 'rows', 'params', 'genome', 'genomes', 'ignore'], ['schema'])
        List<String> names = getReferenceNames(args.subMap(['schema', 'ignore']))
        Map source
        if (args.rows != null) {
            source = getReferenceMetadata(rows: args.rows)
        } else {
            genomeExistsError(genome: args.genome, genomes: args.genomes)
            source = ((args.genomes as Map)?.get(args.genome) ?: [:]) as Map
        }
        return names.collectEntries { name -> [name, source[name]] } + ((args.params ?: [:]) as Map).subMap(names)
    }

    /**
     * List the reference param names in a references schema: every `meta` name not in ignore.
     *
     * @param args Named arguments: schema (required), path to the references schema (local or remote);
     *             ignore, meta names to skip (default ['genome', 'id'], which name the genome itself)
     * @throws IllegalArgumentException if the schema has no items.properties
     */
    static List<String> getReferenceNames(Map args) {
        args = checkArgs(args, 'getReferenceNames', ['schema', 'ignore'], ['schema'])
        List ignore = args.containsKey('ignore') ? (args.ignore ?: []) as List : ['genome', 'id']
        def properties = new JsonSlurper().parseText(NextflowPipelineUtils.toPath(args.schema).text)?.items?.properties
        if (!(properties instanceof Map)) {
            throw new IllegalArgumentException("References schema ${args.schema} has no items.properties")
        }
        return ((Map) properties).values().collectMany { property -> (property.meta ?: []) as List<String> }
            .findAll { name -> !(name in ignore) }
            .unique()
    }

    /**
     * Abort if genome is set but is not a key of genomes, with the message of the nf-core template's genomeExistsError().
     *
     * @param args Named arguments: genome, genomes (all default null)
     * @throws AbortOperationException listing the available genome keys
     */
    static void genomeExistsError(Map args) {
        args = checkArgs(args, 'genomeExistsError', ['genome', 'genomes'])
        Map genomes = args.genomes as Map
        if (args.genome in [null, 'null'] || !genomes || genomes.containsKey(args.genome)) {
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
        args = checkArgs(args, 'getReferenceMetadata', ['rows'], ['rows'])
        return referenceMeta(selectReferenceRow(args.rows))
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
     * Check named arguments: an unknown name throws, since a typo would otherwise read as null.
     * Required names must be passed; any other missing argument defaults to null.
     */
    private static Map checkArgs(Map args, String method, List<String> known, List<String> required = []) {
        Map checked = args ?: [:]
        def unknown = checked.keySet().findAll { !(it in known) }
        if (unknown) {
            throw new IllegalArgumentException("${method} got unknown named argument(s): ${unknown.join(', ')}. Expected: ${known.join(', ')}")
        }
        def missing = required.findAll { !checked.containsKey(it) }
        if (missing) {
            throw new IllegalArgumentException("${method} is missing named argument(s): ${missing.join(', ')}")
        }
        return checked
    }
}
