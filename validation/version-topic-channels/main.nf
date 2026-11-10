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

    file(params.outdir).mkdirs()

    // Test 1: collectVersions with a versions topic fed by real processes: tuples, a repeat, and a versions.yml path
    log.info("=== Testing collectVersions() with topic tuples ===")

    TOPIC_TOOL(channel.of(['samtools', '1.17'], ['fastqc', '0.12.1'], ['samtools', '1.17']))
    LEGACY_TOOL('sample')

    channel.topic('versions')
        .toList()
        .map { versions ->
            log.info("Processing ${versions.size()} topic entries")
            collectVersions(versions)
        }
        .subscribe { yaml ->
            file("${params.outdir}/topic_versions.yml").text = yaml
            log.info("=== Topic tuple result ===")
            log.info(yaml)
            assert yaml.contains('fastqc')
            assert yaml.contains('legacy')
            assert yaml.contains('samtools')
            assert yaml.contains('Workflow:')
            assert yaml.contains('Nextflow:')
            log.info("✅ Topic tuple test passed")
        }

    // A single Path, with the Nextflow version as positional override
    LEGACY_TOOL.out.versions
        .map { versions -> collectVersions(versions, '99.1.0') }
        .subscribe { yaml -> file("${params.outdir}/single_versions.yml").text = yaml }

    // A topic no process emits to: toList() still emits, so the workflow versions are written
    channel.topic('no_versions')
        .toList()
        .map { versions -> collectVersions(versions) }
        .subscribe { yaml -> file("${params.outdir}/empty_versions.yml").text = yaml }

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
        .toList()
        .map { files ->
            log.info("Processing ${files.size()} version files")
            // collectVersions handles File objects directly
            collectVersions(files)
        }
        .subscribe { yaml ->
            file("${params.outdir}/file_versions.yml").text = yaml
            log.info("=== File path result ===")
            log.info(yaml)
            assert yaml.contains('star')
            assert yaml.contains('salmon')
            log.info("✅ File path test passed")
        }

    // Test 3: collectVersions with mixed inputs
    log.info("=== Testing collectVersions() with mixed inputs ===")

    ch_mixed = channel.of(
        ['BWA', 'bwa', '0.7.17'],
        'hisat2: 2.2.1',
        [bowtie2: '2.4.5'],
    )

    ch_mixed
        .toList()
        .map { mixed ->
            log.info("Processing mixed inputs: ${mixed.size()} items")
            collectVersions(mixed)
        }
        .subscribe { yaml ->
            file("${params.outdir}/mixed_versions.yml").text = yaml
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

process TOPIC_TOOL {
    input:
    tuple val(tool), val(version)

    output:
    tuple val(task.process), val(tool), eval("printf '%s' '${version}'"), topic: versions

    script:
    """
    true
    """
}

process LEGACY_TOOL {
    input:
    val sample

    output:
    path 'versions.yml', topic: versions, emit: versions

    script:
    """
    cat > versions.yml <<'END_VERSIONS'
    PIPELINE:LEGACY_TOOL:
      legacy: '2.0'
    END_VERSIONS
    """
}
