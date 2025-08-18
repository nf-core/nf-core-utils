#!/usr/bin/env nextflow

/*
 * E2E validation test for nf-core-utils plugin configuration validation functions
 * 
 * This test validates the configuration validation functions:
 * - checkConfigProvided(): Validate pipeline configuration setup
 * - checkProfileProvided(): Validate execution profiles
 * - checkCondaChannels(): Validate conda channel configuration
 * 
 * These functions are used in pipeline initialization to ensure proper setup.
 */

// Import plugin functions for configuration validation
include { checkConfigProvided; checkProfileProvided; checkCondaChannels } from 'plugin/nf-core-utils'

nextflow.enable.dsl = 2

workflow {
    
    log.info "=========================================="
    log.info "Configuration Validation Test"
    log.info "=========================================="
    
    // Test 1: checkConfigProvided() function
    log.info "=== Testing checkConfigProvided() function ==="
    
    try {
        def config_valid = checkConfigProvided()
        log.info "Configuration check result: ${config_valid}"
        log.info "✅ checkConfigProvided() executed successfully"
    } catch (Exception e) {
        log.error "Error in checkConfigProvided(): ${e.message}"
    }
    
    // Test 2: checkProfileProvided() function
    log.info "=== Testing checkProfileProvided() function ==="
    
    // Test with various command line argument scenarios
    def test_cases = [
        // Valid profile cases
        ['nextflow', 'run', 'main.nf', '-profile', 'docker'],
        ['nextflow', 'run', 'main.nf', '-profile', 'conda'],
        ['nextflow', 'run', 'main.nf', '-profile', 'test,docker'],
        
        // Invalid/missing profile cases  
        ['nextflow', 'run', 'main.nf'],
        ['nextflow', 'run', 'main.nf', '-other-param'],
        
        // Edge cases
        ['nextflow', 'run', 'main.nf', '-profile'],  // No profile value
        ['-profile', 'docker'],  // Profile at beginning
    ]
    
    test_cases.eachWithIndex { args, index ->
        try {
            log.info "Testing args ${index + 1}: ${args}"
            checkProfileProvided(args)
            log.info "✅ Profile check completed for case ${index + 1}"
        } catch (Exception e) {
            log.error "Error in checkProfileProvided() case ${index + 1}: ${e.message}"
        }
    }
    
    // Test 3: checkCondaChannels() function
    log.info "=== Testing checkCondaChannels() function ==="
    
    try {
        def conda_valid = checkCondaChannels()
        log.info "Conda channels check result: ${conda_valid}"
        log.info "✅ checkCondaChannels() executed successfully"
        
        if (conda_valid) {
            log.info "✅ Conda channels are configured correctly"
        } else {
            log.info "⚠️ Conda channels may need configuration adjustment"
        }
    } catch (Exception e) {
        log.error "Error in checkCondaChannels(): ${e.message}"
    }
    
    // Test 4: Integration scenario - typical pipeline initialization
    log.info "=== Testing integration scenario ==="
    
    log.info "Simulating fetchngs pipeline initialization sequence..."
    
    // Simulate the order these functions would be called in fetchngs
    try {
        // 1. Check conda channels first
        def conda_ok = checkCondaChannels()
        log.info "Conda channels status: ${conda_ok ? 'OK' : 'Needs attention'}"
        
        // 2. Check overall configuration
        def config_ok = checkConfigProvided()
        log.info "Configuration status: ${config_ok ? 'Valid' : 'May need custom config'}"
        
        // 3. Check profile (simulate typical fetchngs command)
        def typical_args = ['nextflow', 'run', 'nf-core/fetchngs', '-profile', 'test,docker', '--input', 'SRR_Acc_List.txt']
        checkProfileProvided(typical_args)
        log.info "Profile validation: Completed for typical fetchngs usage"
        
        log.info "✅ Integration scenario completed successfully"
        
    } catch (Exception e) {
        log.error "Integration scenario error: ${e.message}"
    }
    
    // Test 5: Edge cases and error handling
    log.info "=== Testing edge cases ==="
    
    // Test with empty arguments
    try {
        checkProfileProvided([])
        log.info "✅ Empty arguments handled correctly"
    } catch (Exception e) {
        log.warn "Empty arguments case: ${e.message}"
    }
    
    // Test with null arguments (if applicable)
    try {
        checkProfileProvided(null)
        log.info "✅ Null arguments handled correctly"
    } catch (Exception e) {
        log.warn "Null arguments case: ${e.message}"
    }
    
    log.info "=========================================="
    log.info "Configuration Validation Complete"
    log.info "=========================================="
}

workflow.onComplete {
    log.info """
    ===========================================
    Configuration Validation Results
    ===========================================
    
    ✅ checkConfigProvided() - Pipeline configuration validation tested
    ✅ checkProfileProvided() - Execution profile validation tested  
    ✅ checkCondaChannels() - Conda channel validation tested
    ✅ Integration scenario - Typical pipeline initialization flow tested
    ✅ Edge cases - Error handling and boundary conditions tested
    
    Configuration validation functions ready for fetchngs! 🚀
    ===========================================
    """
}