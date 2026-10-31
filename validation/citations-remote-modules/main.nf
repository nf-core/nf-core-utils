#!/usr/bin/env nextflow

include { citationsOnTheFly ; toolCitationText ; toolBibliographyText } from 'plugin/nf-core-utils'

// Registry names, not relative includes of hand-written local substitutes.
include { FASTQC               } from 'nf-core/fastqc'
include { MULTIQC              } from 'nf-core/multiqc'
include { SAMTOOLS_VIEW        } from 'nf-core/samtools/view'

workflow {
    FASTQC(channel.of(tuple([id: 'reads', single_end: true], file("${projectDir}/reads.fastq"))))
    SAMTOOLS_VIEW(
        channel.of(tuple([id: 'alignment'], file("${projectDir}/reads.sam"), [])),
        tuple([:], [], []),
        tuple([:], []),
        tuple([:], []),
        'bai',
    )
    if (params.run_multiqc) {
        MULTIQC(FASTQC.out.zip.map { meta, zip -> tuple(meta, [zip], [], [], [], []) })
        // Evidence only: do not inject this output into the versions topic.
        MULTIQC.out.versions.view { "NON_TOPIC_VERSION: ${it}" }
    }

    def metadata = files("${projectDir}/modules/**/meta.yml").collect { it.toString() }.sort()
    log.info("DISCOVERED_METADATA: ${metadata}")
    channel.topic('versions')
        .collect(flat: false)
        .subscribe { versions ->
            def citations = citationsOnTheFly(versions, metadata)
            def output = new File(params.outdir)
            output.mkdirs()
            new File(output, 'versions.tsv').text = versions.collect { it.join('\t') }.sort().join('\n') + '\n'
            new File(output, 'metadata-paths.txt').text = metadata.join('\n') + '\n'
            new File(output, 'cited-tools.txt').text = citations.keySet().sort().join('\n') + '\n'
            new File(output, 'auto_citations.html').text = toolCitationText(citations)
            new File(output, 'auto_bibliography.html').text = toolBibliographyText(citations)
            log.info("CITED_TOOLS: ${citations.keySet().sort()}")
        }
}
