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
import groovy.transform.PackageScope
import nextflow.exception.AbortOperationException
import nfcore.plugin.nextflow.NextflowPipelineUtils

/**
 * Resolves reference params from a references datasheet, named by a references schema.
 * The genomes config is read only until the igenomes config files are dropped.
 */
class ReferenceDatasheet {

    /**
     * Determine which reference path to use: explicit --references or built from genome.
     *
     * @param args Named arguments: references, genome, igenomesIgnore, datasheetPath (all default null)
     * @return The datasheet path to load, or null if neither references nor genome is provided
     * @throws IllegalArgumentException if both references and genome are provided
     */
    static String getReferencePath(Map args) {
        args = checkArgs(args, 'getReferencePath', ['references', 'genome', 'igenomesIgnore', 'datasheetPath'])
        String references = cliValue(args.references) ?: null
        String genome = cliValue(args.genome) ?: null
        Boolean igenomesIgnore = args.igenomesIgnore
        String datasheetPath = args.datasheetPath

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
     * A null param is unset, false turns the reference off, and any other value wins.
     * Otherwise the datasheet rows are the only source when given, and the genomes config when not:
     * a datasheet never falls back to config.
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
            source = datasheetMeta(args.rows as List)
        } else {
            genomeExistsError(genome: args.genome, genomes: args.genomes)
            source = ((args.genomes as Map)?.get(args.genome) ?: [:]) as Map
        }
        Map params = (args.params ?: [:]) as Map
        return names.collectEntries { name ->
            def value = cliValue(params[name])
            [name, value == null ? source[name] : value == false ? null : value]
        }
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
        List ignore = args.ignore != null ? args.ignore as List : ['genome', 'id']
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
        if (!cliValue(args.genome) || !genomes || genomes.containsKey(args.genome)) {
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

    private static String referenceDatasheet(String genomeKey, String basePath) {
        if (!basePath) {
            throw new IllegalArgumentException("references_datasheets_path is empty; cannot build datasheet URL for genome '${genomeKey}'")
        }
        basePath = basePath.replaceAll(/\/+$/, '')
        return "${basePath}/${genomeKey}.yml"
    }

    /**
     * Non-empty values of the single genome record in datasheet rows (output of samplesheetToList).
     *
     * @throws IllegalArgumentException if the rows are empty or span multiple genomes
     */
    @PackageScope
    static Map datasheetMeta(List rows) {
        return referenceMeta(selectReferenceRow(rows))
    }

    /**
     * Read the text 'null' and 'false' that the command line passes as null and false.
     */
    @PackageScope
    static Object cliValue(Object value) {
        return value == 'null' ? null : value == 'false' ? false : value
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
        def meta = row instanceof List && row && row[0] instanceof Map ? row[0] : row
        if (!(meta instanceof Map)) {
            throw new IllegalArgumentException("Reference datasheet record is not a genome mapping: ${row}")
        }
        return ((Map) meta).findAll { _key, value -> !(value in [null, '', []]) }
    }

    /**
     * Check named arguments: an unknown name throws, since a typo would otherwise read as null.
     * Required names must be passed; any other missing argument defaults to null.
     */
    @PackageScope
    static Map checkArgs(Map args, String method, List<String> known, List<String> required = []) {
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
