#!/usr/bin/env nextflow

/*
 * E2E validation of the nf-core template wiring for software versions:
 * collect the versions topic, collectVersions(), then collectFile() the report.
 */

include { collectVersions } from 'plugin/nf-core-utils'

workflow {

    FASTQC_LIKE(channel.of('sample'))
    LEGACY_TOOL('sample')

    channel.topic('versions')
        .toList()
        .map { versions -> collectVersions(versions) }
        .collectFile(name: 'nf_core_test_software_mqc_versions.yml', storeDir: "${params.outdir}/pipeline_info")
}

process FASTQC_LIKE {
    input:
    val sample

    output:
    tuple val(task.process), val('fastqc'), eval("printf '%s' '0.12.1'"), topic: versions
    tuple val(task.process), val('samtools'), eval("printf '%s' '1.17'"), topic: versions

    script:
    """
    true
    """
}

process LEGACY_TOOL {
    input:
    val sample

    output:
    path 'versions.yml', topic: versions

    script:
    """
    cat > versions.yml <<'END_VERSIONS'
    LEGACY_TOOL:
      legacy: '2.0'
    END_VERSIONS
    """
}
