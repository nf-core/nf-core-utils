package nfcore.plugin.nfcore

import groovy.json.JsonSlurper
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

class NfcoreCitationFormatsTest extends Specification {
    @TempDir
    Path tempDir

    def "bibtex writes an article with publication and a misc entry without"() {
        given:
        def citations = [
            multiqc: [info: [doi: '10.1093/bioinformatics/btw354', homepage: 'https://multiqc.info/',
                             publication: [author: 'Ewels P, Magnusson M, Lundin S, et al.', year: 2016,
                                           title: 'MultiQC: summarize analysis results', source: 'Bioinformatics']],
                      version: '1.21'],
            'samtools/view': [info: [homepage: 'http://www.htslib.org/']]
        ]

        expect:
        NfcoreCitationFormats.bibtex(citations) == '''@article{multiqc,
  author = {{Ewels P, Magnusson M, Lundin S, et al.}},
  title = {MultiQC: summarize analysis results},
  journal = {Bioinformatics},
  year = {2016},
  doi = {10.1093/bioinformatics/btw354},
  url = {https://multiqc.info/},
  version = {1.21}
}

@misc{samtools_view,
  title = {samtools/view},
  url = {http://www.htslib.org/}
}
'''
    }

    def "the meta-schema placeholder 'no DOI available' never renders as a DOI"() {
        given:
        def info = [doi: 'no DOI available', homepage: 'https://github.com/lh3/gfatools']
        def entry = NfcoreCitationUtils.processCitationsFromTopic([['GFATOOLS_STAT', 'gfatools', info]]).gfatools
        def citations = [gfatools: entry]

        expect:
        entry.info == [homepage: 'https://github.com/lh3/gfatools']
        entry.citation == "gfatools (<a href='https://github.com/lh3/gfatools'>https://github.com/lh3/gfatools</a>)"
        entry.bibliography == "<li>gfatools. <a href='https://github.com/lh3/gfatools'>https://github.com/lh3/gfatools</a>.</li>"
        !NfcoreCitationFormats.bibtex(citations).contains('doi')
        !NfcoreCitationFormats.cslJson(citations).contains('DOI')
    }

    def "cslJson writes CSL items with literal authors and year date-parts"() {
        given:
        def citations = [
            multiqc: [info: [doi: '10.1093/bioinformatics/btw354',
                             publication: [author: 'Ewels P, et al.', year: '2016',
                                           title: 'MultiQC', source: 'Bioinformatics']],
                      version: '1.21'],
            seqtk  : [info: [homepage: 'https://github.com/lh3/seqtk']]
        ]

        when:
        def items = new JsonSlurper().parseText(NfcoreCitationFormats.cslJson(citations))

        then:
        items == [
            [id: 'multiqc', type: 'article-journal', title: 'MultiQC', 'container-title': 'Bioinformatics',
             author: [[literal: 'Ewels P, et al.']], issued: ['date-parts': [[2016]]],
             DOI: '10.1093/bioinformatics/btw354', version: '1.21'],
            [id: 'seqtk', type: 'software', title: 'seqtk', URL: 'https://github.com/lh3/seqtk']
        ]
    }

    def "citation files read raw info from every citations map producer"() {
        given:
        def info = [doi: '10.1093/bioinformatics/btp352', publication: [author: 'Danecek P, et al.', year: 2021]]
        def meta = new File(tempDir.toFile(), 'samtools_meta.yml')
        meta << '''
        tools:
          - samtools:
              doi: "10.1093/bioinformatics/btp352"
              publication:
                author: "Danecek P, et al."
                year: 2021
        '''.stripIndent()

        expect:
        [
            NfcoreCitationUtils.processCitationsFromTopic([['SAMTOOLS_VIEW', 'samtools', info]]),
            NfcoreCitationUtils.generateModuleToolCitation(meta.absolutePath)
        ].every { citations ->
            NfcoreCitationFormats.bibtex(citations) == '''@misc{samtools,
  author = {{Danecek P, et al.}},
  title = {samtools},
  year = {2021},
  doi = {10.1093/bioinformatics/btp352}
}
'''
        }
    }

    @Issue("https://github.com/nf-core/nf-core-utils/pull/51")
    def "shortCitation distinguishes single and multiple authors: #author"() {
        expect:
        shortCitation([publication: [author: author, year: 2021]], '1.21') == "tool (1.21, ${reference} (2021))"
        shortCitation([author: author, year: 2021], '1.21') == "tool (1.21, ${reference} (2021))"

        where:
        author                                       | reference
        'Andrews S'                                  | 'Andrews'
        'Andrews, S.'                                | 'Andrews'
        'van der Walt, S.'                           | 'van der Walt'
        'Smith-Jones, A. B.'                         | 'Smith-Jones'
        'Einstein, A., Feynman, R. P. & Hawking, S.' | 'Einstein <em>et al.</em>'
        'Smith, J. & Jones, B.'                      | 'Smith <em>et al.</em>'
        'Smith, J., Jones, B.'                       | 'Smith <em>et al.</em>'
        'Danecek P, Bonfield JK, et al.'             | 'Danecek <em>et al.</em>'
        'Smith J & Jones B'                          | 'Smith <em>et al.</em>'
        'Smith J and Jones B'                        | 'Smith <em>et al.</em>'
        'Li H et al.'                                | 'Li <em>et al.</em>'
    }

    def "shortCitation falls back to doi, then url, then description"() {
        expect:
        shortCitation([doi: '10.1093/bioinformatics/btw354'], '1.21') == "tool (1.21, <a href='https://doi.org/10.1093/bioinformatics/btw354'>doi: 10.1093/bioinformatics/btw354</a>)"
        shortCitation([homepage: 'https://github.com/lh3/seqtk'], '1.4') == "tool (1.4, <a href='https://github.com/lh3/seqtk'>https://github.com/lh3/seqtk</a>)"
        shortCitation([description: 'A tool']) == 'tool (A tool)'
        shortCitation([:]) == 'tool'
    }

    def "shortCitation omits the version segment when no version is known"() {
        expect:
        shortCitation([publication: [author: 'Andrews S', year: 2010]]) == 'tool (Andrews (2010))'
    }

    private static String shortCitation(Map info, String version = null) {
        NfcoreCitationFormats.shortCitation(NfcoreCitationFormats.referenceOf('tool', info, version))
    }
}
