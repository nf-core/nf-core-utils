package nfcore.plugin.nfcore

import groovy.json.JsonSlurper
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
}
