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
 * Citations-map entries built from a tool's meta.yml info, and every format
 * rendered from them: MultiQC HTML, BibTeX and CSL-JSON.
 */
@Slf4j(category = 'nextflow.nfcore.NfcoreCitationFormats')
class NfcoreCitationFormats {

    // Same pattern as the doi field of the nf-core/modules meta-schema.json
    private static final String DOI_PATTERN = /10\.\d{4,9}\/[^,\s]+/

    /**
     * Citations-map entry for one tool: its meta.yml info, with a {@code doi} that
     * is not a bare DOI (such as the old placeholder "no DOI available") dropped
     * with a warning, the HTML citation and bibliography rendered from it, and
     * the version.
     */
    static Map citationEntry(String tool, Map info, String version = null) {
        def doi = info.doi?.toString()?.trim()
        if (doi && !(doi ==~ DOI_PATTERN)) {
            log.warn1("Ignoring doi '${doi}' of tool '${tool}': not a bare DOI such as 10.1093/bioinformatics/btw354")
            info = info.findAll { it.key != 'doi' }
        }
        def ref = referenceOf(tool, info, version)
        return [citation: shortCitation(ref), bibliography: bibliography(ref), info: info, version: version]
    }

    /**
     * Publication-style short citation, as HTML: {@code tool (version, Author et al. (year))}.
     * The reference falls back to {@code doi: ...}, then the homepage url, then the
     * description, and links to the DOI or homepage. Unknown segments are omitted.
     */
    static String shortCitation(Map ref) {
        def text = ref.author ? [shortAuthor(ref.author), ref.year ? "(${ref.year})" : null].findAll().join(' ')
            : ref.doi ? "doi: ${ref.doi}" : ref.url ?: ref.description
        def href = ref.doi ? "https://doi.org/${ref.doi}" : ref.url
        def parts = [ref.version, text && href ? "<a href='${href}'>${text}</a>" : text].findAll()
        return parts ? "${ref.tool} (${parts.join(', ')})" : ref.tool
    }

    /**
     * Bibliography entry, as an HTML list item: "Author. (year). title. source. link"
     * when the tool has publication fields, else "tool. link", linking to the DOI or
     * else the homepage.
     */
    static String bibliography(Map ref) {
        def parts = [ref.author?.replaceAll(/\.$/, ''), ref.year ? "(${ref.year})" : null, ref.title, ref.source].findAll()
        if (!parts) parts << ref.tool
        if (ref.doi) parts << "doi: <a href='https://doi.org/${ref.doi}'>${ref.doi}</a>"
        else if (ref.url) parts << "<a href='${ref.url}'>${ref.url}</a>"
        return "<li>${parts.join('. ')}.</li>"
    }

    /**
     * BibTeX: an {@code @article} when the publication has a title and source,
     * else a {@code @misc} entry for the tool.
     */
    static String bibtex(Map citations) {
        return citations.collect { tool, entry ->
            def ref = referenceOf(tool.toString(), entry.info ?: [:], entry.version)
            def fields = [
                // shortcut: double braces keep the meta.yml author string verbatim; split it into an "and" list if styles need initials
                author : ref.author ? "{${ref.author}}" : null,
                title  : ref.title ?: ref.tool,
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
            def ref = referenceOf(tool.toString(), entry.info ?: [:], entry.version)
            [
                id               : tool.toString(),
                type             : ref.article ? 'article-journal' : 'software',
                title            : ref.title ?: ref.tool,
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
     */
    private static Map publicationOf(Map info) {
        if (info.publication instanceof Map) return info.publication as Map
        return [author: info.author, year: info.year, title: info.title, source: info.journal]
    }

    /** Reference fields of a tool's meta.yml info: trimmed strings, null when absent. */
    static Map referenceOf(String tool, Map info, Object version = null) {
        def pub = publicationOf(info)
        def str = { it?.toString()?.trim() ?: null }
        return [
            tool       : tool,
            article    : pub.title && pub.source,
            author     : str(pub.author),
            title      : str(pub.title),
            source     : str(pub.source),
            year       : str(pub.year),
            doi        : str(info.doi),
            url        : str(info.homepage),
            description: str(info.description),
            version    : str(version)
        ]
    }

    /**
     * Reduce an author string to "Surname" (single author) or "Surname <em>et al.</em>"
     * (multiple). Recognize Harvard "Surname, Initials" pairs as well as legacy
     * "Surname Initials" lists. In Harvard names, preserve compound surnames and
     * do not mistake the comma before initials for an author separator.
     */
    private static String shortAuthor(String author) {
        def authors = author.split(/\s+(?:&|and)\s+/)
        def first = authors[0].split(',')*.trim()
        def harvard = first.size() > 1 && first[1] ==~ /(?:\p{Lu}\.?\s*)+/
        def surname = harvard ? first[0] : first[0].split(/\s+/)[0]
        def multiple = authors.size() > 1 || author.toLowerCase().contains('et al') ||
            first.size() > (harvard ? 2 : 1)
        return multiple ? "${surname} <em>et al.</em>" : surname
    }
}
