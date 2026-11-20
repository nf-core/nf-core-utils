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
}
