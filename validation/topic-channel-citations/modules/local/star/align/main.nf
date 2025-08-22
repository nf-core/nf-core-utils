include { getCitation } from 'plugin/nf-core-utils'

process STAR_ALIGN {
    tag "$sample_id"
    label 'process_high'
    
    when:
    params.run_optional == true
    
    input:
    val sample_id
    
    output:
    path "*.txt", emit: result
    val citation_data, topic: citation
    
    script:
    citation_data = getCitation("${moduleDir}/meta.yml")
    """
    echo "STAR alignment output for ${sample_id}" > ${sample_id}_star.txt
    """
}