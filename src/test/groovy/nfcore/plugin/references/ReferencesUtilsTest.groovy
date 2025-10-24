package nfcore.plugin.references

import nextflow.Session
import nextflow.file.FileHelper
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

class ReferencesUtilsTest extends Specification {
    def extension

    @TempDir
    Path tempDir

    def setup() {
        extension = ReferencesUtils
    }

    def "test getReferencesFile with param and getReferencesValue without param"() {
        given:
        // Simulate references list data (List of [meta, _readme])
        def referencesList = [
                [[id: 'test', fasta: '/path/to/genome.fasta', species: 'Homo sapiens'], null],
                [[id: 'test2', fasta: '/path/to/genome2.fasta', species: 'Mus musculus'], null]
        ]
        def param_file = '/override/genome.fasta'
        def param_value = null
        def attribute_file = 'fasta'
        def attribute_value = 'species'
        def basepath = '/base/path'

        when:
        def result_file = ReferencesUtils.getReferencesFile(referencesList, param_file, attribute_file, basepath)
        def result_value = ReferencesUtils.getReferencesValue(referencesList, param_value, attribute_value)

        then:
        // getReferencesFile should use param_file for all
        result_file.size() == 2
        result_file.every { it[1].toString() == param_file }
        // getReferencesValue should use meta[attribute_value]
        result_value.size() == 2
        result_value[0][1] == 'Homo sapiens'
        result_value[1][1] == 'Mus musculus'
    }

    def "test getReferencesFile and getReferencesValue with nulls"() {
        given:
        def referencesList = [
                [[id: 'test', fasta: null, species: null], null]
        ]
        def param_file = null
        def param_value = null
        def attribute_file = 'fasta'
        def attribute_value = 'species'
        def basepath = '/base/path'

        when:
        def result_file = ReferencesUtils.getReferencesFile(referencesList, param_file, attribute_file, basepath)
        def result_value = ReferencesUtils.getReferencesValue(referencesList, param_value, attribute_value)

        then:
        result_file == [null]
        result_value == [null]
    }

    def "test updateReferencesFile with named parameters - single replacement"() {
        given:
        // Create a test YAML file
        def yamlFile = tempDir.resolve("test_references.yml")
        yamlFile.text = """
id: test_genome
fasta: \${params.igenomes_base}/Homo_sapiens/NCBI/GRCh38/Sequence/genome.fasta
gtf: \${params.igenomes_base}/Homo_sapiens/NCBI/GRCh38/Annotation/genes.gtf
""".stripIndent()

        def referencesUtils = new ReferencesUtils()
        def session = Mock(Session) {
            getWorkDir() >> tempDir
        }
        referencesUtils.init(session)

        when:
        def result = referencesUtils.updateReferencesFile(
            [basepathFinal: '/new/base/path', basepathToReplace: '${params.igenomes_base}'],
            yamlFile
        )

        then:
        result != null
        result.exists()
        def content = result.text
        content.contains('/new/base/path/Homo_sapiens/NCBI/GRCh38/Sequence/genome.fasta')
        content.contains('/new/base/path/Homo_sapiens/NCBI/GRCh38/Annotation/genes.gtf')
        !content.contains('${params.igenomes_base}')
    }

    def "test updateReferencesFile with positional parameters"() {
        given:
        def yamlFile = tempDir.resolve("test_references2.yml")
        yamlFile.text = """
id: test_genome
fasta: /old/path/genome.fasta
gtf: /old/path/genes.gtf
""".stripIndent()

        def referencesUtils = new ReferencesUtils()
        def session = Mock(Session) {
            getWorkDir() >> tempDir
        }
        referencesUtils.init(session)

        when:
        def result = referencesUtils.updateReferencesFile(yamlFile, '/new/path', '/old/path')

        then:
        result != null
        result.exists()
        def content = result.text
        content.contains('/new/path/genome.fasta')
        content.contains('/new/path/genes.gtf')
        !content.contains('/old/path')
    }

    def "test updateReferencesFile with multiple basepaths to replace"() {
        given:
        def yamlFile = tempDir.resolve("test_references3.yml")
        yamlFile.text = """
id: test_genome
fasta: \${params.igenomes_base}/genome.fasta
gtf: \${params.references_base}/genes.gtf
readme: /old/base/readme.txt
""".stripIndent()

        def referencesUtils = new ReferencesUtils()
        def session = Mock(Session) {
            getWorkDir() >> tempDir
        }
        referencesUtils.init(session)

        when:
        def result = referencesUtils.updateReferencesFile(
            [basepathFinal: '/new/unified/path',
             basepathToReplace: ['${params.igenomes_base}', '${params.references_base}', '/old/base']],
            yamlFile
        )

        then:
        result != null
        result.exists()
        def content = result.text
        content.contains('/new/unified/path/genome.fasta')
        content.contains('/new/unified/path/genes.gtf')
        content.contains('/new/unified/path/readme.txt')
        !content.contains('${params.igenomes_base}')
        !content.contains('${params.references_base}')
        !content.contains('/old/base')
    }

    def "test updateReferencesFile without replacement returns original file"() {
        given:
        def yamlFile = tempDir.resolve("test_references4.yml")
        def originalContent = """
id: test_genome
fasta: /some/path/genome.fasta
""".stripIndent()
        yamlFile.text = originalContent

        def referencesUtils = new ReferencesUtils()
        def session = Mock(Session) {
            getWorkDir() >> tempDir
        }
        referencesUtils.init(session)

        when:
        def result = referencesUtils.updateReferencesFile(
            [basepathFinal: null, basepathToReplace: null],
            yamlFile
        )

        then:
        result != null
        result.exists()
        result == yamlFile
        result.text == originalContent
    }

    def "test updateReferencesFile with empty basepathFinal returns original file"() {
        given:
        def yamlFile = tempDir.resolve("test_references5.yml")
        def originalContent = """
id: test_genome
fasta: /some/path/genome.fasta
""".stripIndent()
        yamlFile.text = originalContent

        def referencesUtils = new ReferencesUtils()
        def session = Mock(Session) {
            getWorkDir() >> tempDir
        }
        referencesUtils.init(session)

        when:
        def result = referencesUtils.updateReferencesFile(
            [basepathFinal: '', basepathToReplace: '/some/path'],
            yamlFile
        )

        then:
        result != null
        result.exists()
        result == yamlFile
        result.text == originalContent
    }

    def "test updateReferencesFile throws exception for non-existent file"() {
        given:
        def nonExistentFile = tempDir.resolve("non_existent.yml")

        def referencesUtils = new ReferencesUtils()
        def session = Mock(Session) {
            getWorkDir() >> tempDir
        }
        referencesUtils.init(session)

        when:
        referencesUtils.updateReferencesFile(
            [basepathFinal: '/new/path', basepathToReplace: '/old/path'],
            nonExistentFile
        )

        then:
        thrown(IllegalArgumentException)
    }

    def "test updateReferencesFile creates staged copy in work directory"() {
        given:
        def yamlFile = tempDir.resolve("test_references6.yml")
        yamlFile.text = """
id: test_genome
fasta: /old/path/genome.fasta
""".stripIndent()

        def referencesUtils = new ReferencesUtils()
        def session = Mock(Session) {
            getWorkDir() >> tempDir
        }
        referencesUtils.init(session)

        when:
        def result = referencesUtils.updateReferencesFile(
            [basepathFinal: '/new/path', basepathToReplace: '/old/path'],
            yamlFile
        )

        then:
        result != null
        result.exists()
        // The result should be a different file (staged copy)
        result != yamlFile
        result.toString().contains(tempDir.toString())
        result.toString().contains('tmp')
        // Original file should remain unchanged
        yamlFile.text.contains('/old/path/genome.fasta')
    }

    def "test updateReferencesFile with snake_case parameter names"() {
        given:
        def yamlFile = tempDir.resolve("test_references7.yml")
        yamlFile.text = """
id: test_genome
fasta: /old/path/genome.fasta
""".stripIndent()

        def referencesUtils = new ReferencesUtils()
        def session = Mock(Session) {
            getWorkDir() >> tempDir
        }
        referencesUtils.init(session)

        when:
        def result = referencesUtils.updateReferencesFile(
            [basepath_final: '/new/path', basepath_to_replace: '/old/path'],
            yamlFile
        )

        then:
        result != null
        result.exists()
        def content = result.text
        content.contains('/new/path/genome.fasta')
        !content.contains('/old/path')
    }
}
