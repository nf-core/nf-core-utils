process SAMTOOLS_VIEW {
    tag "${sample_id}"
    label 'process_low'

    input:
    val sample_id

    output:
    path "*.sam", emit: sam
    tuple val("${task.process}"), val('samtools'), val('1.21'), topic: versions

    script:
    """
    echo "SAM data for ${sample_id}" > ${sample_id}.sam
    """
}
