#!/usr/bin/env nextflow

/*
 * E2E validation test for "citations on the fly".
 *
 * Unlike the topic-channel-citations test, the modules here do NOT emit a
 * dedicated `citation` topic. They only emit the standard `versions` topic
 * ([process, tool, version]) that every nf-core module already produces when
 * it runs. citationsOnTheFly() intersects that list of tools-that-ran with the
 * citations parsed from each module's meta.yml, so citations reflect exactly
 * what executed — with zero per-module changes.
 */

include { citationsOnTheFly    } from 'plugin/nf-core-utils'
include { toolCitationText     } from 'plugin/nf-core-utils'
include { toolBibliographyText } from 'plugin/nf-core-utils'

include { FASTQC               } from './modules/local/fastqc/main'
include { MULTIQC              } from './modules/local/multiqc/main'
include { SAMTOOLS_VIEW        } from './modules/local/samtools/view/main'
include { STAR_ALIGN           } from './modules/local/star/align/main'

workflow {

    log.info(
        """
        ==========================================
        nf-core-utils Citations on the Fly E2E Test
        ==========================================

        This test demonstrates citations driven by the versions topic:
        1. Modules emit only [process, tool, version] to the versions topic
        2. citationsOnTheFly() resolves citation metadata from meta.yml
        3. Only tools that actually ran are cited
        """.stripIndent().trim()
    )

    // Create sample data
    samples = Channel.of('sample1', 'sample2', 'sample3')

    // Run processes - each emits its version to the `versions` topic when it runs
    fastqc_out = FASTQC(samples)
    SAMTOOLS_VIEW(samples)
    MULTIQC(fastqc_out.html.collect())

    // STAR has a meta.yml but is OFF: it must not appear in the citations
    if (params.run_optional) {
        STAR_ALIGN(samples.first())
    }

    // Citation metadata source: every module meta.yml under this pipeline.
    // files() returns an empty list when nothing matches the glob.
    def meta_yml_paths = files("${projectDir}/modules/**/meta.yml").collect { it.toString() }

    // Build citations from the tools that actually ran (the versions topic).
    // collect(flat: false) preserves each [process, tool, version] tuple.
    ch_citations = channel.topic('versions')
        .collect(flat: false)
        .map { versions ->
            def citations = citationsOnTheFly(versions, meta_yml_paths)
            log.info("=== Cited tools (ran + have meta.yml): ${citations.keySet().sort().join(', ')} ===")
            [
                citation_text: toolCitationText(citations),
                bibliography : toolBibliographyText(citations),
            ]
        }

    // Write outputs and demonstrate usage
    ch_citations.subscribe { citations ->
        log.info(
            """
            ==========================================
            CITATIONS ON THE FLY RESULTS
            ==========================================

            Citation Text:
            ${citations.citation_text}

            Bibliography:
            ${citations.bibliography}

            ==========================================
            """.stripIndent().trim()
        )

        def output_dir = params.outdir
        new File(output_dir).mkdirs()
        new File("${output_dir}/auto_citations.txt").text = citations.citation_text
        new File("${output_dir}/auto_bibliography.html").text = citations.bibliography

        log.info("✅ Citations written to: ${output_dir}/")
    }

    workflow.onComplete {
        log.info(
            """
            ==========================================
            Citations on the Fly Test Complete!
            ==========================================

            ✅ Citations derived from the versions topic (tools that ran)
            ✅ Metadata resolved from module meta.yml — no hand-maintained list
            ✅ STAR has a meta.yml but did not run, so it is not cited
            ==========================================
            """.stripIndent().trim()
        )
    }
}
