process STAR_ALIGN {
    tag "${sample_id}"
    label 'process_high'

    input:
    val sample_id

    output:
    path "*.txt", emit: result
    tuple val("${task.process}"), val('star'), val('2.7.11b'), topic: versions

    script:
    """
    echo "STAR alignment output for ${sample_id}" > ${sample_id}_star.txt
    """
}
