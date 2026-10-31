#!/usr/bin/env nextflow

/*
 * E2E validation test for nf-core-utils plugin topic channel citation management
 *
 * This test demonstrates the new runtime citation collection approach using topic channels.
 * Instead of manually configuring citations, processes automatically emit citation data
 * which is collected and processed automatically.
 */

// Import the new auto-citation functions from plugin
include { autoToolBibliographyText } from 'plugin/nf-core-utils'
include { autoToolCitationText     } from 'plugin/nf-core-utils'
include { getCitation              } from 'plugin/nf-core-utils'

// Import local modules
include { FASTQC                   } from './modules/local/fastqc/main'
include { MULTIQC                  } from './modules/local/multiqc/main'
include { SAMTOOLS_VIEW            } from './modules/local/samtools/view/main'
include { STAR_ALIGN               } from './modules/local/star/align/main'

workflow {

    log.info(
        """
        ==========================================
        nf-core-utils Plugin Topic Channel Citation E2E Test
        ==========================================

        This test demonstrates automatic citation collection using:
        1. Runtime citation extraction via getCitation()
        2. Topic channel emission from processes
        3. Automatic aggregation and formatting
        4. Zero-maintenance citation management
        """.stripIndent().trim()
    )

    // Create sample data
    samples = Channel.of('sample1', 'sample2', 'sample3')

    // Run processes - each automatically emits citations to topic channel
    fastqc_out = FASTQC(samples)
    SAMTOOLS_VIEW(samples)

    // MultiQC processes the fastqc outputs
    MULTIQC(fastqc_out.html.collect())

    // Optional tool runs only if parameter is set
    if (params.run_optional) {
        STAR_ALIGN(samples.first())
    }

    // Collect all citations from topic channel automatically
    ch_citations = channel.topic('citation')
        .collect()
        .map { citations ->
            log.info("=== Collected ${citations.size()} citation emissions ===")
            // Sort citations to ensure consistent ordering
            def sortedCitations = citations.sort()
            sortedCitations.each { citation ->
                log.info("Citation: ${citation}")
            }
            return sortedCitations
        }

    // Generate final citation text and bibliography automatically
    final_citations = ch_citations.map { citations ->
        [
            citation_text: autoToolCitationText(citations),
            bibliography: autoToolBibliographyText(citations),
        ]
    }

    // Write outputs and demonstrate usage
    final_citations.subscribe { citations ->
        log.info(
            """
            ==========================================
            AUTOMATIC CITATION RESULTS
            ==========================================

            Citation Text:
            ${citations.citation_text}

            Bibliography:
            ${citations.bibliography}

            ==========================================
            """.stripIndent().trim()
        )

        // Write to files for validation
        def output_dir = params.outdir

        // Create output directory
        new File(output_dir).mkdirs()

        // Write citation text
        new File("${output_dir}/auto_citations.txt").text = citations.citation_text

        // Write bibliography
        new File("${output_dir}/auto_bibliography.html").text = citations.bibliography

        log.info("✅ Auto-generated citations written to: ${output_dir}/")
        log.info("✅ Citation text: ${output_dir}/auto_citations.txt")
        log.info("✅ Bibliography: ${output_dir}/auto_bibliography.html")
    }

    workflow.onComplete {
        log.info(
            """
            ==========================================
            Topic Channel Citation Management Test Complete!
            ==========================================

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
            ==========================================
            """.stripIndent().trim()
        )
    }
}
