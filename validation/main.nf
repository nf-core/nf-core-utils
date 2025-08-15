#!/usr/bin/env nextflow

// Import plugin functions
include { 
    sayHello;
    getWorkflowVersion;
    checkCondaChannels;
    completionSummary;
    generateVersionReport;
    generateCitationReport;
    processMixedVersionSources;
    processMixedCitationSources;
} from 'plugin/nf-core-utils'

workflow {
    
    log.info """
    ================================
    nf-utils Plugin Integration Test
    ================================
    """
    
    // Test 1: Basic sayHello function
    log.info "Testing sayHello function..."
    sayHello("nf-utils integration test")
    
    // Test 2: Get workflow version
    log.info "Testing getWorkflowVersion function..."
    version = getWorkflowVersion()
    log.info "Workflow version: ${version}"
    
    // Test 3: Check conda channels
    log.info "Testing checkCondaChannels function..."
    conda_check = checkCondaChannels()
    log.info "Conda channels check: ${conda_check}"
    
    // Test 4: Test version processing with sample data
    log.info "Testing version processing..."
    
    // Sample topic channel data: [process, name, version]
    sample_versions = [
        ['FASTQC', 'fastqc', '0.11.9'],
        ['MULTIQC', 'multiqc', '1.12'],
        ['TRIMGALORE', 'trimgalore', '0.6.7'],
        ['TRIMGALORE', 'cutadapt', '4.1']
    ]
    
    // Test version report generation
    version_report = generateVersionReport(sample_versions, [])
    log.info "Version report generated successfully"
    
    // Test 5: Test citation processing with sample data
    log.info "Testing citation processing..."
    
    // Create sample meta.yml files for testing
    sample_meta_paths = []
    
    // Test mixed version sources (topic + legacy)
    log.info "Testing mixed version sources..."
    mixed_versions = processMixedVersionSources(sample_versions, [])
    log.info "Mixed versions processed successfully"
    
    // Test 6: Completion summary
    log.info "Testing completion summary..."
    completionSummary(false)
    
    log.info """
    ================================
    All integration tests completed!
    ================================
    """
}
