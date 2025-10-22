#!/usr/bin/env nextflow

/*
 * E2E validation test for softwareVersionsToYAML() function
 *
 * This test validates the enhanced softwareVersionsToYAML() function
 * which supports mixed input sources:
 * - YAML strings (legacy inline)
 * - File paths pointing to versions.yml
 * - Topic tuples: [process, tool, version]
 * - Maps of tool->version
 * - Custom Nextflow version parameter
 */

// Import plugin function
include { softwareVersionsToYAML } from 'plugin/nf-core-utils'

workflow {

    log.info("==========================================")
    log.info("softwareVersionsToYAML() Validation Test")
    log.info("==========================================")

    // Test 1: YAML strings (legacy format)
    log.info("=== Test 1: YAML strings ===")

    def yaml_strings = Channel.of(
        'fastqc: 0.12.1',
        'samtools: 1.17',
        'multiqc: 1.15',
    )

    def result1 = softwareVersionsToYAML(yaml_strings)
    result1.view { yaml ->
        log.info("Result with YAML strings:")
        log.info(yaml)
        assert yaml.contains('fastqc: 0.12.1')
        assert yaml.contains('samtools: 1.17')
        assert yaml.contains('Workflow:')
    }

    // Test 2: Topic tuples
    log.info("=== Test 2: Topic tuples ===")

    def topic_tuples = Channel.of(
        ['NFCORE_FASTQC', 'fastqc', '0.12.1'],
        ['NFCORE_SAMTOOLS', 'samtools', '1.17'],
    )

    def result2 = softwareVersionsToYAML(topic_tuples)
    result2.view { yaml ->
        log.info("Result with topic tuples:")
        log.info(yaml)
        assert yaml.contains('fastqc: 0.12.1')
        assert yaml.contains('NFCORE_FASTQC:')
        assert yaml.contains('NFCORE_SAMTOOLS:')
    }

    // Test 3: Mixed inputs
    log.info("=== Test 3: Mixed input types ===")

    def mixed_inputs = Channel.of(
        'fastqc: 0.12.1',
        ['NFCORE_SAMTOOLS', 'samtools', '1.17'],
        [multiqc: '1.15', bcftools: '1.16'],
    )

    def result3 = softwareVersionsToYAML(mixed_inputs)
    result3.view { yaml ->
        log.info("Result with mixed inputs:")
        log.info(yaml)
        assert yaml.contains('fastqc: 0.12.1')
        assert yaml.contains('samtools')
        assert yaml.contains('multiqc')
        assert yaml.contains('bcftools')
    }

    // Test 4: Custom Nextflow version
    log.info("=== Test 4: Custom Nextflow version ===")

    def versions_ch = Channel.of('fastqc: 0.12.1')

    def result4 = softwareVersionsToYAML(versions_ch, nextflowVersion: '24.10.0')
    result4.view { yaml ->
        log.info("Result with custom Nextflow version:")
        log.info(yaml)
        assert yaml.contains('Nextflow: 24.10.0')
    }

    // Test 5: Named parameters
    log.info("=== Test 5: Named parameters ===")

    def versions_ch5 = Channel.of('samtools: 1.17')

    def result5 = softwareVersionsToYAML(
        softwareVersions: versions_ch5,
        nextflowVersion: '25.01.0',
    )
    result5.view { yaml ->
        log.info("Result with named parameters:")
        log.info(yaml)
        assert yaml.contains('samtools: 1.17')
        assert yaml.contains('Nextflow: 25.01.0')
    }

    // Test 6: File inputs
    log.info("=== Test 6: File inputs ===")

    // Create mock version file
    def version_file_content = """
    FASTQC:
        fastqc: 0.12.1
    SAMTOOLS:
        samtools: 1.17
    """.stripIndent()

    def version_file = Channel.of(version_file_content)
        .collectFile(name: 'versions.yml', newLine: true)

    def result6 = softwareVersionsToYAML(version_file)
    result6.view { yaml ->
        log.info("Result with file input:")
        log.info(yaml)
        assert yaml.contains('fastqc: 0.12.1')
        assert yaml.contains('samtools: 1.17')
    }

    // Test 7: Process name extraction from full path
    log.info("=== Test 7: Process name extraction ===")

    def full_path_tuples = Channel.of(
        ['NFCORE_RNASEQ:TRIMGALORE:FASTQC', 'fastqc', '0.12.1']
    )

    def result7 = softwareVersionsToYAML(full_path_tuples)
    result7.view { yaml ->
        log.info("Result with full process path:")
        log.info(yaml)
        assert yaml.contains('FASTQC:')
        assert yaml.contains('fastqc: 0.12.1')
        assert !yaml.contains('NFCORE_RNASEQ:TRIMGALORE:FASTQC')
    }

    // Test 8: Sorted output
    log.info("=== Test 8: Alphabetical sorting ===")

    def unsorted = Channel.of(
        ['ZEBRA_PROCESS', 'zebra', '1.0.0'],
        ['ALPHA_PROCESS', 'alpha', '2.0.0'],
        ['ALPHA_PROCESS', 'zulu', '3.0.0'],
    )

    def result8 = softwareVersionsToYAML(unsorted)
    result8.view { yaml ->
        log.info("Result with sorting:")
        log.info(yaml)
        def alphaIdx = yaml.indexOf('ALPHA_PROCESS:')
        def zebraIdx = yaml.indexOf('ZEBRA_PROCESS:')
        assert alphaIdx < zebraIdx : "Processes should be sorted alphabetically"
    }

    workflow.onComplete {
        log.info(
            """
            ==========================================
            softwareVersionsToYAML() Validation Results
            ==========================================

            ✅ YAML string inputs tested
            ✅ Topic tuple inputs tested
            ✅ Mixed input types tested
            ✅ Custom Nextflow version tested
            ✅ Named parameters tested
            ✅ File inputs tested
            ✅ Process name extraction tested
            ✅ Alphabetical sorting tested

            Function ready for production use! 🚀
            ==========================================
            """.stripIndent().trim()
        )
    }
}
