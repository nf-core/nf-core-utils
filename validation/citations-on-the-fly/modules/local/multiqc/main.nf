process MULTIQC {
    label 'process_medium'

    input:
    path fastqc_files

    output:
    path "*.html", emit: report
    tuple val("${task.process}"), val('multiqc'), val('1.21'), topic: versions

    script:
    """
    echo "<html>MultiQC Report</html>" > multiqc_report.html
    """
}
