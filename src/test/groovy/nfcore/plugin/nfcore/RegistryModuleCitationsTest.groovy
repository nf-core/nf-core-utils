package nfcore.plugin.nfcore

import spock.lang.Issue
import spock.lang.Narrative
import spock.lang.PendingFeature
import spock.lang.Requires
import spock.lang.Specification
import spock.lang.Title
import spock.lang.Unroll

@Title('Registry-module execution produces citations and bibliography for the tools that ran')
@Narrative('''
The opt-in validation/citations-remote-modules workflow runs real registry tools.
Check its generated artifacts, including MultiQC, which intentionally does not
publish to the versions topic. No registry downloads or tool executions happen
inside this spec; set NF_CORE_REMOTE_CITATIONS_OUTDIR to the all-tools run output.
''')
@Issue('https://github.com/nf-core/nf-core-utils/issues/89')
@Requires({ env.NF_CORE_REMOTE_CITATIONS_OUTDIR })
class RegistryModuleCitationsTest extends Specification {
    Map<String, String> artifacts

    def setup() {
        def outputDir = new File(System.getenv('NF_CORE_REMOTE_CITATIONS_OUTDIR'))
        // Read in setup so missing artifacts are errors, not pending features.
        artifacts = ['auto_citations.html', 'auto_bibliography.html'].collectEntries { name ->
            [(name): new File(outputDir, name).getText('UTF-8')]
        }
    }

    @Unroll
    def '#artifact cites registry #tool'() {
        expect:
        artifacts[artifact].contains(reference)

        where:
        artifact                 | tool       | reference
        'auto_citations.html'     | 'FastQC'   | 'https://www.bioinformatics.babraham.ac.uk/projects/fastqc/'
        'auto_bibliography.html'  | 'FastQC'   | 'https://www.bioinformatics.babraham.ac.uk/projects/fastqc/'
        'auto_citations.html'     | 'SAMtools' | 'https://doi.org/10.1093/bioinformatics/btp352'
        'auto_bibliography.html'  | 'SAMtools' | 'https://doi.org/10.1093/bioinformatics/btp352'
    }

    @PendingFeature(exceptions = AssertionError, reason = 'Issue #89: registry MultiQC is absent from the versions topic')
    def 'citations include executed registry MultiQC without a versions-topic emission'() {
        expect:
        artifacts['auto_citations.html'].contains('https://multiqc.info/')
    }

    @PendingFeature(exceptions = AssertionError, reason = 'Issue #89: registry MultiQC is absent from the versions topic')
    def 'bibliography includes executed registry MultiQC without a versions-topic emission'() {
        expect:
        artifacts['auto_bibliography.html'].contains('https://multiqc.info/')
    }
}
