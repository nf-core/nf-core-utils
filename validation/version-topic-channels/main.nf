#!/usr/bin/env nextflow

/*
 * E2E validation test for nf-core-utils plugin version utilities
 *
 * This test demonstrates the new simplified collectVersions() API that
 * handles all input types automatically: YAML strings, file paths, topic tuples,
 * maps, and mixed inputs.
 */

// Import plugin functions - collectVersions() is the recommended API
include { collectVersions } from 'plugin/nf-core-utils'

workflow {

    // Test 1: collectVersions with topic tuples (most common pattern)
    log.info("=== Testing collectVersions() with topic tuples ===")

    ch_topic_versions = channel.of(
        ['FASTQC', 'fastqc', '0.11.9'],
        ['MULTIQC', 'multiqc', '1.12'],
        ['SAMTOOLS', 'samtools', '1.17'],
    )

    // Collect all versions and process with single call
    ch_topic_versions
        .collect()
        .map { versions ->
            log.info("Processing ${versions.size()} topic tuples")
            collectVersions(versions)
        }
        .subscribe { yaml ->
            log.info("=== Topic tuple result ===")
            log.info(yaml)
            assert yaml.contains('fastqc')
            assert yaml.contains('multiqc')
            assert yaml.contains('samtools')
            assert yaml.contains('Workflow:')
            assert yaml.contains('Nextflow:')
            log.info("✅ Topic tuple test passed")
        }

    // Test 2: collectVersions with file paths (legacy pattern)
    log.info("=== Testing collectVersions() with file paths ===")

    ch_mock_content = channel.of(
        """
        STAR:
            star: 2.7.10a
        """.stripIndent(),
        """
        SALMON:
            salmon: 1.9.0
        """.stripIndent(),
    )

    ch_version_files = ch_mock_content.collectFile { content ->
        def filename = "versions_${Math.abs(content.hashCode())}.yml"
        [filename, content]
    }

    ch_version_files
        .collect()
        .map { files ->
            log.info("Processing ${files.size()} version files")
            // collectVersions handles File objects directly
            collectVersions(files)
        }
        .subscribe { yaml ->
            log.info("=== File path result ===")
            log.info(yaml)
            assert yaml.contains('star')
            assert yaml.contains('salmon')
            log.info("✅ File path test passed")
        }

    // Test 3: collectVersions with mixed inputs
    log.info("=== Testing collectVersions() with mixed inputs ===")

    ch_mixed = channel.of(
        ['BWA', 'bwa', '0.7.17'],                    // topic tuple
        'hisat2: 2.2.1',                             // YAML string
        [bowtie2: '2.4.5'],                          // Map
    )

    ch_mixed
        .collect()
        .map { mixed ->
            log.info("Processing mixed inputs: ${mixed.size()} items")
            collectVersions(mixed)
        }
        .subscribe { yaml ->
            log.info("=== Mixed input result ===")
            log.info(yaml)
            assert yaml.contains('bwa')
            assert yaml.contains('hisat2')
            assert yaml.contains('bowtie2')
            log.info("✅ Mixed input test passed")
        }

    workflow.onComplete {
        log.info(
            """
            ==========================================
            collectVersions() API Validation Complete
            ==========================================

            ✅ Topic tuple handling verified
            ✅ File path handling verified
            ✅ Mixed input handling verified
            ✅ Workflow version auto-included

            The new collectVersions() API:
            - Accepts any input type (tuples, files, strings, maps)
            - Automatically includes workflow version
            - Sorts output alphabetically
            - Replaces all deprecated process* methods

            ==========================================
            """.stripIndent().trim()
        )
    }
}
