#!/usr/bin/env nextflow

/*
 * collectVersions() on a versions topic fed by a typed process (records), an untyped
 * process (tuples) and a legacy versions.yml.
 */

include { collectVersions } from 'plugin/nf-core-utils'
include { RECORD_TOOL     } from './modules/record_tool'

workflow {

    file(params.outdir).mkdirs()

    RECORD_TOOL(channel.of('preseq'), '3.2.0')
    TUPLE_TOOL('fastqc', '0.12.1')
    LEGACY_TOOL('sample')

    channel.topic('versions')
        .toList()
        .map { versions -> collectVersions(versions) }
        .subscribe { yaml -> file("${params.outdir}/versions.yml").text = yaml }
}

process TUPLE_TOOL {
    input:
    val tool
    val version

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
    path 'versions.yml', topic: versions

    script:
    """
    cat > versions.yml <<'END_VERSIONS'
    PIPELINE:LEGACY_TOOL:
      legacy: '2.0'
    END_VERSIONS
    """
}
