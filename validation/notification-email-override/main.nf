#!/usr/bin/env nextflow

/*
 * E2E validation: completionEmail renders the pipeline's own assets/email_template.* when present.
 * No recipient is given, so nothing is sent; only the report files are written.
 */

include { completionEmail } from 'plugin/nf-core-utils'

workflow {
    workflow.onComplete {
        def summary_params = ['Group': ['input': 'override_input.csv']]
        completionEmail(summary_params, null, null, false, './results', false, [])
    }
}
