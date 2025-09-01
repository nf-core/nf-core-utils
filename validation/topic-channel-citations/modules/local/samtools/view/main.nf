include { getCitation } from 'plugin/nf-core-utils'

process SAMTOOLS_VIEW {
    tag "${sample_id}"
    label 'process_low'

    input:
    val sample_id

    output:
    path "*.sam", emit: sam
    val citation_data, topic: citation

    script:
    citation_data = getCitation("${moduleDir}/meta.yml")
    """
    echo "SAM data for ${sample_id}" > ${sample_id}.sam
    """
}
