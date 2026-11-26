package nfcore.plugin.nfcore

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import nextflow.Session
import org.slf4j.LoggerFactory
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

class NfcoreCitationsOnTheFlyTest extends Specification {
    @TempDir
    Path tempDir

    def "toolsFromVersionsTopic extracts unique sorted tool names from versions tuples"() {
        given:
        def topicVersions = [
            ['NFCORE_FASTQC:FASTQC', 'fastqc', '0.12.1'],
            ['NFCORE_PIPELINE:MULTIQC', 'multiqc', '1.21'],
            ['NFCORE_FASTQC:FASTQC', 'fastqc', '0.12.1'] // duplicate process/tool
        ]

        when:
        def result = NfcoreCitationUtils.toolsFromVersionsTopic(topicVersions)

        then:
        result == ['fastqc', 'multiqc']
    }

    def "toolsFromVersionsTopic handles empty and null input"() {
        expect:
        NfcoreCitationUtils.toolsFromVersionsTopic([]) == []
        NfcoreCitationUtils.toolsFromVersionsTopic(null) == []
    }

    def "toolsFromVersionsTopic returns empty for a flattened versions list"() {
        given:
        // Plain channel.topic('versions').collect() flattens tuples into loose
        // scalars; only .collect(flat: false) preserves [process, tool, version].
        def flattened = ['NFCORE_FASTQC:FASTQC', 'fastqc', '0.12.1', 'NFCORE_PIPELINE:MULTIQC', 'multiqc', '1.21']

        expect:
        NfcoreCitationUtils.toolsFromVersionsTopic(flattened) == []
    }

    def "toolsFromVersionsTopic warns when given a flattened (non-tuple) versions list"() {
        given:
        def appender = captureLogsFor(NfcoreCitationUtils)
        def flattened = ['NFCORE_FASTQC:FASTQC', 'fastqc', '0.12.1']

        when:
        def result = NfcoreCitationUtils.toolsFromVersionsTopic(flattened)

        then:
        result == []
        logMessages(appender).any { it.contains('flat: false') }

        cleanup:
        detachAppender(appender)
    }

    def "toolsFromVersionsTopic does not warn for properly collected tuples"() {
        given:
        def appender = captureLogsFor(NfcoreCitationUtils)

        when:
        NfcoreCitationUtils.toolsFromVersionsTopic([['PROC', 'fastqc', '0.12.1']])

        then:
        logMessages(appender).every { !it.contains('flat: false') }

        cleanup:
        detachAppender(appender)
    }

    def "toolsFromVersionsTopic reads records alone and alongside tuples"() {
        given:
        def appender = captureLogsFor(NfcoreCitationUtils)
        def preseq = new TestRecord(process: 'NFCORE:PRESEQ_LCEXTRAP', tool: 'preseq', version: '3.2.0')

        expect:
        NfcoreCitationUtils.toolsFromVersionsTopic([preseq]) == ['preseq']
        NfcoreCitationUtils.toolsFromVersionsTopic([preseq, ['NFCORE:FASTQC', 'fastqc', '0.12.1']]) == ['fastqc', 'preseq']
        logMessages(appender).every { !it.contains('flat: false') }

        cleanup:
        detachAppender(appender)
    }

    def "toolsFromVersionsTopic ignores plain maps with versions keys"() {
        expect:
        NfcoreCitationUtils.toolsFromVersionsTopic([
            [process: 'NFCORE:FASTQC', tool: 'fastqc', version: '0.12.1']
        ]) == []
    }

    def "toolsFromVersionsTopic warns about records without versions fields, not about flattening"() {
        given:
        def appender = captureLogsFor(NfcoreCitationUtils)
        def incomplete = new TestRecord(id: 'sample1')

        when:
        def result = NfcoreCitationUtils.toolsFromVersionsTopic([incomplete])

        then:
        result == []
        logMessages(appender).any { it.contains('skipped 1 versions record(s)') }
        logMessages(appender).every { !it.contains('flat: false') }

        cleanup:
        detachAppender(appender)
    }

    def "toolsFromVersionsTopic skips incomplete records and keeps complete ones"() {
        given:
        def topicVersions = [
            new TestRecord(id: 'sample1'),
            new TestRecord(process: 'NFCORE:PRESEQ_LCEXTRAP', tool: 'preseq', version: '3.2.0')
        ]

        expect:
        NfcoreCitationUtils.toolsFromVersionsTopic(topicVersions) == ['preseq']
        NfcoreCitationUtils.toolVersionsFromTopic(topicVersions) == [preseq: '3.2.0']
    }

    def "filterCitationsByTools keeps only tools that ran, matching case-insensitively"() {
        given:
        def allCitations = [
            'fastqc'  : [citation: 'fastqc (...)', bibliography: '<li>FastQC</li>'],
            'multiqc' : [citation: 'multiqc (...)', bibliography: '<li>MultiQC</li>'],
            'samtools': [citation: 'samtools (...)', bibliography: '<li>SAMtools</li>']
        ]
        // multiqc did not run; FastQC differs from the meta.yml key only in case
        def toolsUsed = ['FastQC', 'samtools']

        when:
        def result = NfcoreCitationUtils.filterCitationsByTools(allCitations, toolsUsed)

        then:
        result.keySet() == ['fastqc', 'samtools'] as Set
        !result.containsKey('multiqc')
    }

    def "filterCitationsByTools omits tools that ran but have no citation entry"() {
        given:
        def allCitations = [
            'fastqc': [citation: 'fastqc (...)', bibliography: '<li>FastQC</li>']
        ]
        // mysterytool ran but has no meta.yml citation entry
        def toolsUsed = ['fastqc', 'mysterytool']

        when:
        def result = NfcoreCitationUtils.filterCitationsByTools(allCitations, toolsUsed)

        then:
        result.keySet() == ['fastqc'] as Set
        !result.containsKey('mysterytool')
    }

    def "citationsOnTheFly intersects versions topic with meta.yml citations"() {
        given:
        def fastqcMeta = new File(tempDir.toFile(), 'fastqc_meta.yml')
        fastqcMeta << '''
        name: fastqc
        tools:
          - fastqc:
              doi: "10.1093/bioinformatics/btv033"
              homepage: "https://www.bioinformatics.babraham.ac.uk/projects/fastqc/"
        '''.stripIndent()
        def samtoolsMeta = new File(tempDir.toFile(), 'samtools_meta.yml')
        samtoolsMeta << '''
        name: samtools
        tools:
          - samtools:
              doi: "10.1093/bioinformatics/btp352"
        '''.stripIndent()

        // Only FASTQC actually ran in this pipeline execution
        def topicVersions = [
            ['NFCORE_TEST:FASTQC', 'fastqc', '0.12.1']
        ]
        def metaPaths = [fastqcMeta.absolutePath, samtoolsMeta.absolutePath]

        when:
        def result = NfcoreCitationUtils.citationsOnTheFly(topicVersions, metaPaths)

        then:
        result.containsKey('fastqc')
        !result.containsKey('samtools') // present in meta.yml but did not run
        result.fastqc.citation.contains('fastqc')
    }

    // --- Publication-style short citations (version + short author) ---

    def "toolVersionsFromTopic maps tool names to versions"() {
        given:
        def topicVersions = [
            ['NFCORE:FASTQC', 'fastqc', '0.12.1'],
            ['NFCORE:MULTIQC', 'multiqc', '1.21'],
            ['NFCORE:FASTQC', 'fastqc', '0.12.1'] // duplicate
        ]

        expect:
        NfcoreCitationUtils.toolVersionsFromTopic(topicVersions) == ['fastqc': '0.12.1', 'multiqc': '1.21']
    }

    def "toolVersionsFromTopic maps versions from records and tuples"() {
        given:
        def topicVersions = [
            new TestRecord(process: 'NFCORE:PRESEQ_LCEXTRAP', tool: 'preseq', version: '3.2.0'),
            ['NFCORE:MULTIQC', 'multiqc', '1.21']
        ]

        expect:
        NfcoreCitationUtils.toolVersionsFromTopic(topicVersions) == ['preseq': '3.2.0', 'multiqc': '1.21']
    }

    def "citationsOnTheFly cites tools that reported versions as records"() {
        given:
        def preseqMeta = new File(tempDir.toFile(), 'preseq_meta.yml')
        preseqMeta << '''
        name: preseq_lcextrap
        tools:
          - preseq:
              doi: "10.1038/nmeth.2375"
        '''.stripIndent()

        when:
        def result = NfcoreCitationUtils.citationsOnTheFly(
            [new TestRecord(process: 'NFCORE:PRESEQ_LCEXTRAP', tool: 'preseq', version: '3.2.0')], [preseqMeta.absolutePath])

        then:
        result.keySet() == ['preseq'] as Set
        result.preseq.version == '3.2.0'
    }

    def "citationsOnTheFly produces publication-style short citations with version"() {
        given:
        def fastqcMeta = new File(tempDir.toFile(), 'fastqc_meta.yml')
        fastqcMeta << '''
        name: fastqc
        tools:
          - fastqc:
              publication:
                author: "Andrews S"
                year: 2010
              doi: "10.1093/bioinformatics/btv033"
              homepage: "https://www.bioinformatics.babraham.ac.uk/projects/fastqc/"
        '''.stripIndent()
        def topicVersions = [['NFCORE:FASTQC', 'fastqc', '0.12.1']]

        when:
        def result = NfcoreCitationUtils.citationsOnTheFly(topicVersions, [fastqcMeta.absolutePath])

        then: 'short citation is copy-paste ready: tool (version, Author (year)) with DOI link'
        result.fastqc.citation == 'fastqc (0.12.1, <a href=\'https://doi.org/10.1093/bioinformatics/btv033\'>Andrews (2010)</a>)'

        and: 'the full bibliography is unchanged'
        result.fastqc.bibliography.contains('<li>')
        result.fastqc.bibliography.contains('Andrews S')
    }

    def "citationsOnTheFly can manually add a tool not in the versions topic"() {
        given:
        // multiqcsav is a MultiQC plugin: it has a meta.yml but never emits a version
        def multiqcsavMeta = new File(tempDir.toFile(), 'multiqcsav_meta.yml')
        multiqcsavMeta << '''
        name: multiqcsav
        tools:
          - multiqcsav:
              description: "MultiQC plugin for Illumina Sequencing Analysis Viewer"
              homepage: "https://github.com/MultiQC/MultiQC_SAV/"
        '''.stripIndent()
        def fastqcMeta = new File(tempDir.toFile(), 'fastqc_meta.yml')
        fastqcMeta << '''
        name: fastqc
        tools:
          - fastqc:
              doi: "10.1093/bioinformatics/btv033"
        '''.stripIndent()
        def topicVersions = [['NFCORE:FASTQC', 'fastqc', '0.12.1']]

        when:
        def result = NfcoreCitationUtils.citationsOnTheFly(
            topicVersions,
            [fastqcMeta.absolutePath, multiqcsavMeta.absolutePath],
            ['multiqcsav']
        )

        then: 'the manually-added tool is cited (no version segment, resolved from meta.yml)'
        result.containsKey('fastqc')
        result.containsKey('multiqcsav')
        result.multiqcsav.citation == 'multiqcsav (<a href=\'https://github.com/MultiQC/MultiQC_SAV/\'>https://github.com/MultiQC/MultiQC_SAV/</a>)'
    }

    def "citationsOnTheFly entries carry raw info and version, from meta.yml Paths"() {
        given:
        def meta = new File(tempDir.toFile(), 'multiqc_meta.yml')
        meta << '''
        tools:
          - multiqc:
              doi: "10.1093/bioinformatics/btw354"
        '''.stripIndent()

        when:
        def result = NfcoreCitationUtils.citationsOnTheFly([['MULTIQC', 'multiqc', '1.21']], [meta.toPath()])

        then:
        result.multiqc.info == [doi: '10.1093/bioinformatics/btw354']
        result.multiqc.version == '1.21'
    }

    def "citationOutputs returns MultiQC HTML by default and the requested formats"() {
        given:
        def citations = [seqtk: [citation: 'seqtk', bibliography: '<li>seqtk.</li>', info: [homepage: 'https://github.com/lh3/seqtk']]]

        expect:
        NfcoreCitationUtils.citationOutputs(citations).keySet() == ['tool_citations', 'tool_bibliography'] as Set
        NfcoreCitationUtils.citationOutputs(citations, ['bibtex']) == [bibtex: NfcoreCitationFormats.bibtex(citations)]
        NfcoreCitationUtils.citationOutputs(citations, ['html', 'bibtex', 'csl_json']).keySet() ==
            ['tool_citations', 'tool_bibliography', 'bibtex', 'csl_json'] as Set
    }

    def "citationOutputs rejects unknown formats"() {
        when:
        NfcoreCitationUtils.citationOutputs([:], ['html', 'ris'])

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains('ris')
    }

    def "citationOutputs renders the MultiQC methods description from a template"() {
        given:
        def template = tempDir.resolve('methods_description_template.yml')
        template.text = 'Tools: ${tool_citations} Refs: ${tool_bibliography}'
        def citations = [seqtk: [citation: 'seqtk', bibliography: '<li>seqtk.</li>']]
        def mockSession = Mock(Session) {
            getManifest() >> Mock(nextflow.config.Manifest) { toMap() >> [:] }
            getWorkflowMetadata() >> Mock(nextflow.script.WorkflowMetadata) { toMap() >> [:] }
        }
        def originalSession = nextflow.Nextflow.metaClass.static.getSession
        nextflow.Nextflow.metaClass.static.getSession = { -> mockSession }

        when:
        def outputs = NfcoreCitationUtils.citationOutputs(citations, ['html', 'bibtex'], template)

        then:
        outputs.keySet() == ['tool_citations', 'tool_bibliography', 'methods_description', 'bibtex'] as Set
        outputs.methods_description == 'Tools: Tools used in the workflow included: seqtk. Refs: <li>seqtk.</li>'

        cleanup:
        nextflow.Nextflow.metaClass.static.getSession = originalSession
    }

    def "citationOutputs rejects a template without the html format"() {
        when:
        NfcoreCitationUtils.citationOutputs([:], ['bibtex'], Path.of('methods.yml'))

        then:
        thrown(IllegalArgumentException)
    }

    // --- log capture helpers ---

    private ListAppender<ILoggingEvent> captureLogsFor(Class clazz) {
        def appender = new ListAppender<ILoggingEvent>()
        def logger = (Logger) LoggerFactory.getLogger("nextflow.nfcore.${clazz.simpleName}")
        appender.start()
        logger.addAppender(appender)
        return appender
    }

    private List<String> logMessages(ListAppender<ILoggingEvent> appender) {
        appender.list.collect { it.formattedMessage }
    }

    private void detachAppender(ListAppender<ILoggingEvent> appender) {
        ((Logger) LoggerFactory.getLogger("nextflow.nfcore.NfcoreCitationUtils")).detachAppender(appender)
    }
}
