include { getCitation } from 'plugin/nf-core-utils'

process MULTIQC {
    label 'process_medium'

    input:
    path fastqc_files

    output:
    path "*.html", emit: report
    val citation_data, topic: citation

    script:
    citation_data = getCitation("${moduleDir}/meta.yml")
    """
    echo "<html>MultiQC Report</html>" > multiqc_report.html
    """
}
