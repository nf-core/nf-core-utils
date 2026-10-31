process FASTQC {
    tag "${sample_id}"
    label 'process_medium'

    input:
    val sample_id

    output:
    path "*.html", emit: html
    tuple val("${task.process}"), val('fastqc'), val('0.12.1'), topic: versions

    script:
    """
    echo "<html>FastQC Report for ${sample_id}</html>" > ${sample_id}_fastqc.html
    """
}
