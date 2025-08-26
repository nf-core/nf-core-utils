#!/usr/bin/env nextflow

/*
 * Error condition test for nf-core-utils plugin color formatting
 * 
 * This test specifically validates error conditions and color formatting
 * It is expected to fail with specific error messages to test the color functionality
 */

// Import plugin functions for configuration validation
include { checkProfileProvided } from 'plugin/nf-core-utils'

nextflow.enable.dsl = 2

workflow {
    
    log.info "=========================================="
    log.info "Color Formatting Error Test"
    log.info "=========================================="
    
    // This test is designed to fail to test error formatting
    log.info "Testing profile with trailing comma (expected to fail)"
    
    // Test with the monochrome_logs parameter from params
    def invalid_args = ['nextflow', 'run', 'main.nf', '-profile', 'docker,']
    checkProfileProvided(invalid_args, params.monochrome_logs ?: true)
}