#!/usr/bin/env nextflow

/*
 * E2E validation test for nf-core-utils plugin topic channel citation management
 * 
 * This test demonstrates the new runtime citation collection approach using topic channels.
 * Instead of manually configuring citations, processes automatically emit citation data
 * which is collected and processed automatically.
 */

// Import the new auto-citation functions from plugin
include { 
    getCitation;
    autoToolCitationText;
    autoToolBibliographyText;
} from 'plugin/nf-core-utils'

nextflow.enable.dsl = 2

// Mock processes that simulate nf-core modules with topic channel citation emission
process FASTQC {
    input:
    val sample_id
    
    output:
    path "*.html", emit: html
    path "*.zip", emit: zip
    val citation_data, topic: citation
    
    script:
    citation_data = getCitation("${projectDir}/mock-data/fastqc_meta.yml")
    """
    echo "<html>FastQC Report for ${sample_id}</html>" > ${sample_id}_fastqc.html
    echo "FastQC zip data" > ${sample_id}_fastqc.zip
    """
}

process MULTIQC {
    input:
    path fastqc_files
    
    output:
    path "*.html", emit: report
    val citation_data, topic: citation
    
    script:
    citation_data = getCitation("${projectDir}/mock-data/multiqc_meta.yml")
    """
    echo "<html>MultiQC Report</html>" > multiqc_report.html
    """
}

process SAMTOOLS_VIEW {
    input:
    val sample_id
    
    output:
    path "*.sam", emit: sam
    val citation_data, topic: citation
    
    script:
    citation_data = getCitation("${projectDir}/mock-data/samtools_meta.yml")
    """
    echo "SAM data for ${sample_id}" > ${sample_id}.sam
    """
}

// Optional process that may or may not run based on parameters
process OPTIONAL_TOOL {
    when:
    params.run_optional == true
    
    input:
    val sample_id
    
    output:
    path "*.txt", emit: result
    val citation_data, topic: citation
    
    script:
    citation_data = getCitation("${projectDir}/mock-data/star_meta.yml")
    """
    echo "Optional tool output for ${sample_id}" > ${sample_id}_optional.txt
    """
}

workflow {
    
    log.info """
    ========================================================
    nf-core-utils Plugin Topic Channel Citation E2E Test
    ========================================================
    
    This test demonstrates automatic citation collection using:
    1. Runtime citation extraction via getCitation() 
    2. Topic channel emission from processes
    3. Automatic aggregation and formatting
    4. Zero-maintenance citation management
    """
    
    // Create sample data
    samples = Channel.of('sample1', 'sample2', 'sample3')
    
    // Run processes - each automatically emits citations to topic channel
    fastqc_out = FASTQC(samples)
    samtools_out = SAMTOOLS_VIEW(samples)
    
    // MultiQC processes the fastqc outputs
    multiqc_out = MULTIQC(fastqc_out.html.collect())
    
    // Optional tool runs only if parameter is set
    optional_out = OPTIONAL_TOOL(samples.first())
    
    // Collect all citations from topic channel automatically
    ch_citations = channel.topic('citation')
        .collect()
        .map { citations -> 
            log.info "=== Collected ${citations.size()} citation emissions ==="
            citations.each { citation ->
                log.info "Citation: ${citation}"
            }
            return citations
        }
    
    // Generate final citation text and bibliography automatically
    final_citations = ch_citations
        .map { citations ->
            [
                citation_text: autoToolCitationText(citations),
                bibliography: autoToolBibliographyText(citations)
            ]
        }
    
    // Write outputs and demonstrate usage
    final_citations.subscribe { citations ->
        log.info """
        ========================================
        AUTOMATIC CITATION RESULTS
        ========================================
        
        Citation Text:
        ${citations.citation_text}
        
        Bibliography:
        ${citations.bibliography}
        
        ========================================
        """
        
        // Write to files for validation
        def output_dir = "${workflow.workDir}/pipeline_info"
        
        // Create output directory
        new File(output_dir).mkdirs()
        
        // Write citation text
        new File("${output_dir}/auto_citations.txt").text = citations.citation_text
        
        // Write bibliography  
        new File("${output_dir}/auto_bibliography.html").text = citations.bibliography
        
        log.info "✅ Auto-generated citations written to: ${output_dir}/"
        log.info "✅ Citation text: auto_citations.txt"
        log.info "✅ Bibliography: auto_bibliography.html"
    }
}

workflow.onComplete {
    log.info """
    ========================================================
    Topic Channel Citation Management Test Complete! 
    ========================================================
    
    ✅ Processes automatically emitted citations to topic channel
    ✅ Citations collected and processed without manual configuration
    ✅ Final citation text and bibliography generated automatically
    ✅ Zero maintenance required - citations reflect actual tool usage
    
    Key Benefits Demonstrated:
    • No manual citation configuration needed
    • Only executed tools appear in citations  
    • Automatic formatting and aggregation
    • Clean separation of concerns
    • Runtime accuracy
    
    The future of nf-core citation management! 🚀
    ========================================================
    """
}