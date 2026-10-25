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

/*
 * Pure reference helper functions for use in pipeline main.nf scripts.
 * These are pure functions with no dependencies on samplesheetToList or workflow context.
 * Include in your pipeline with:
 *   include { referencesDatasheetUrl; selectReferenceRow; referenceMeta; getGenomeAttribute } from 'plugin/nf-core-utils'
 */

def referencesDatasheetUrl(genomeKey: String, basePath: String = null) {
    if (!basePath) {
        basePath = params.references_datasheets_path ?: ''
    }
    if (!basePath) {
        error("references_datasheets_path is empty; cannot build datasheet URL for genome '${genomeKey}'")
    }
    basePath = basePath.toString().replaceAll(/\/+$/, '')
    return "${basePath}/${genomeKey}.yml"
}

def selectReferenceRow(rows: List) {
    if (!(rows instanceof List) || rows.isEmpty()) {
        error("Reference datasheet did not contain any genome records")
    }
    if (rows.size() > 1) {
        def names = rows.collect { row -> referenceMeta(row).genome }.findAll { name -> name }
        error("Reference datasheet has multiple genomes (${names.join(', ')}). Use a datasheet with one genome record.")
    }
    return rows[0]
}

def referenceMeta(row) {
    if (row instanceof Map) {
        return row.findAll { _key, value -> value != null && value != '' }
    }
    if (row instanceof List && row && row[0] instanceof Map) {
        def meta: Map = row[0] as Map
        return meta.findAll { _key, value -> value != null && value != '' }
    }
    return [:]
}

def getGenomeAttribute(attribute: String) {
    if (params.genomes && params.genome && params.genomes.containsKey(params.genome)) {
        if (params.genomes[params.genome].containsKey(attribute)) {
            return params.genomes[params.genome][attribute]
        }
    }
    return null
}
