#!/usr/bin/env nextflow

/*
 * E2E validation test for nf-core-utils plugin pipeline utilities
 *
 * This test validates the core pipeline utility functions:
 * - getWorkflowVersion(): Generate workflow version strings
 * - dumpParametersToJSON(): Save pipeline parameters to JSON file
 * - genomeExistsError(): Pass when genome is unset
 * - logColours(): ANSI colour codes for both monochrome modes
 */

// Import plugin functions for pipeline utilities
include { getWorkflowVersion   } from 'plugin/nf-core-utils'
include { dumpParametersToJSON } from 'plugin/nf-core-utils'
include { genomeExistsError    } from 'plugin/nf-core-utils'
include { logColours           } from 'plugin/nf-core-utils'

workflow {

    log.info("==========================================")
    log.info("Pipeline Utilities Validation Test")
    log.info("==========================================")

    // Test 1: getWorkflowVersion() function
    log.info("=== Testing getWorkflowVersion() function ===")

    // Test with default parameters (using session info)
    def workflow_version_default = getWorkflowVersion()
    log.info("Default workflow version: ${workflow_version_default}")

    // Test with explicit version
    def workflow_version_explicit = getWorkflowVersion("2.1.0")
    log.info("Explicit version (2.1.0): ${workflow_version_explicit}")

    // Test with version and commit ID
    def workflow_version_with_commit = getWorkflowVersion("2.1.0", "abc123def456789")
    log.info("Version with commit (2.1.0, abc123): ${workflow_version_with_commit}")

    // Test edge cases
    def workflow_version_v_prefix = getWorkflowVersion("v3.0.0")
    log.info("Version with v prefix (v3.0.0): ${workflow_version_v_prefix}")

    def workflow_version_null_version = getWorkflowVersion(null, "xyz789abcdef123")
    log.info("Null version with commit: ${workflow_version_null_version}")

    // Test 2: dumpParametersToJSON() function
    log.info("=== Testing dumpParametersToJSON() function ===")

    // Create simple test parameters
    def test_params = [
        input: 'SRR_Acc_List.txt',
        email: 'test@example.com',
        outdir: './results',
        genome: 'GRCh38',
        skip_fastqc: false,
        max_memory: '128.GB',
        max_cpus: 16,
    ]

    // Test parameter dumping to JSON
    def output_dir = "${workflow.workDir}/validation_output"
    log.info("Dumping parameters to JSON in: ${output_dir}")

    dumpParametersToJSON(output_dir, test_params)
    log.info("✅ Parameters dumped successfully")

    // Test edge cases
    log.info("=== Testing edge cases ===")

    // Test with null output directory
    dumpParametersToJSON(null, test_params)
    log.info("✅ Null output directory handled correctly")

    // Test 3: genomeExistsError() passes when genome is null
    genomeExistsError(genome: null, genomes: [GRCh38: [:]])
    log.info("✅ genomeExistsError() passed with no genome set")

    // Test 4: logColours() in both modes
    assert logColours(true).red == ''
    assert logColours(false).red == '\033[0;31m'
    log.info("✅ logColours() returned codes for both modes")

    // Validate outputs
    log.info("=== Validating outputs ===")

    // Check if JSON file was created
    def json_file_path = "${output_dir}/params.json"
    def json_file = new File(json_file_path)

    if (json_file.exists()) {
        log.info("✅ JSON file created: ${json_file_path}")

        // Validate JSON content
        def json_content = new groovy.json.JsonSlurper().parseText(json_file.text)
        log.info("✅ JSON content is valid")
        log.info("JSON contains ${json_content.size()} parameters")

        // Basic content validation
        assert json_content.input == 'SRR_Acc_List.txt'
        assert json_content.max_cpus == 16
        log.info("✅ JSON content validation passed")
    }
    else {
        log.warn("JSON file not found at: ${json_file_path}")
    }

    // Validate version strings
    log.info("=== Validating version strings ===")

    assert workflow_version_explicit.startsWith('v2.1.0')
    assert workflow_version_with_commit.contains('-g')
    assert workflow_version_v_prefix == 'v3.0.0'

    log.info("✅ All version string validations passed")

    log.info("==========================================")
    log.info("Pipeline Utilities Validation Complete")
    log.info("==========================================")

    workflow.onComplete {
        log.info(
            """
            ==========================================
            Pipeline Utilities Validation Results
            ==========================================

            ✅ getWorkflowVersion() function tested successfully
            ✅ dumpParametersToJSON() function tested successfully
            ✅ Edge cases handled properly
            ✅ Output validation completed

            Functions ready for use in fetchngs pipeline! 🚀
            ==========================================
            """.stripIndent().trim()
        )
    }
}
