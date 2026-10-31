/*
 * Copyright 2025, nf-core
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nfcore.plugin

import groovy.util.logging.Slf4j
import nextflow.Session
import nextflow.trace.TraceObserver

/**
 * Implements an observer that allows implementing custom
 * logic on nextflow execution events.
 */
@Slf4j(category = 'nextflow.nfcore.NfcorePipelineObserver')
class NfcorePipelineObserver implements TraceObserver {

    @Override
    void onFlowCreate(Session session) {
        // Pipelines own startup validation, using exported functions or template-local checks.
        // Observer checks duplicate their warnings, and Nextflow swallows callback exceptions.
        log.trace "Pipeline is starting! 🚀"
    }

    @Override
    void onFlowComplete() {
        log.trace "Pipeline complete! 👋"
        // TODO dumpParametersToJSON(session.params.outdir, session.params)
    }
}
