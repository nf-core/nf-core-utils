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

package nfcore.plugin.nfcore

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j

/**
 * Reference fields of citations-map entries ({@code info}, the raw meta.yml tool map,
 * and {@code version}), and the BibTeX and CSL-JSON files built from them.
 */
@Slf4j(category = 'nextflow.nfcore.NfcoreCitationFormats')
class NfcoreCitationFormats {

    // Same pattern as the doi field of the nf-core/modules meta-schema.json
    private static final String DOI_PATTERN = /10\.\d{4,9}\/[^,\s]+/

    /**
     * BibTeX: an {@code @article} when the publication has a title and source,
     * else a {@code @misc} entry for the tool.
     */
    static String bibtex(Map citations) {
        return citations.collect { tool, entry ->
            def ref = referenceOf(tool.toString(), entry.info, entry.version)
            def fields = [
                // shortcut: double braces keep the meta.yml author string verbatim; split it into an "and" list if styles need initials
                author : ref.author ? "{${ref.author}}" : null,
                title  : ref.title,
                journal: ref.source,
                year   : ref.year,
                doi    : ref.doi,
                url    : ref.url,
                version: ref.version
            ].findAll { it.value }
            def key = tool.toString().replaceAll(/[^A-Za-z0-9_-]/, '_')
            "@${ref.article ? 'article' : 'misc'}{${key},\n" + fields.collect { k, v -> "  ${k} = {${v}}" }.join(',\n') + "\n}"
        }.join('\n\n') + '\n'
    }

    /** CSL-JSON array, readable by Zotero, Pandoc and citeproc. */
    static String cslJson(Map citations) {
        def items = citations.collect { tool, entry ->
            def ref = referenceOf(tool.toString(), entry.info, entry.version)
            [
                id               : tool.toString(),
                type             : ref.article ? 'article-journal' : 'software',
                title            : ref.title,
                'container-title': ref.source,
                author           : ref.author ? [[literal: ref.author]] : null,
                issued           : ref.year?.isInteger() ? ['date-parts': [[ref.year.toInteger()]]] : null,
                DOI              : ref.doi,
                URL              : ref.url,
                version          : ref.version
            ].findAll { it.value }
        }
        return JsonOutput.prettyPrint(JsonOutput.toJson(items)) + '\n'
    }

    /**
     * Publication fields for a tool: the nested publication object, else the
     * pre-0.7 flat fields (author/year/title/journal) so older meta.yml still cite.
     * Null fields are dropped so a tool without any yields an empty (falsy) map.
     */
    static Map publicationOf(Map info) {
        if (info.publication instanceof Map) return info.publication as Map
        return [author: info.author, year: info.year, title: info.title, source: info.journal].findAll { it.value }
    }

    /**
     * Reference fields of a tool's raw meta.yml info: trimmed strings, null when
     * absent, title falling back to the tool name. A {@code doi} that is not a
     * bare DOI, such as the old placeholder "no DOI available", is dropped with
     * a warning.
     */
    static Map referenceOf(String tool, Object info, Object version = null) {
        def toolInfo = info instanceof Map ? info as Map : [:]
        def pub = publicationOf(toolInfo)
        def str = { it?.toString()?.trim() ?: null }
        def doi = str(toolInfo.doi)
        if (doi && !(doi ==~ DOI_PATTERN)) {
            log.warn1("Ignoring doi '${doi}' of tool '${tool}': not a bare DOI such as 10.1093/bioinformatics/btw354")
            doi = null
        }
        return [
            article: pub.title && pub.source,
            author : str(pub.author),
            title  : str(pub.title) ?: tool,
            source : str(pub.source),
            year   : str(pub.year),
            doi    : doi,
            url    : str(toolInfo.homepage),
            version: str(version)
        ]
    }
}
