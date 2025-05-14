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
import nfcore.plugin.util.NfcoreConfigValidator

/**
 * Implements an observer that allows implementing custom
 * logic on nextflow execution events.
 */
@Slf4j
class NfcorePipelineObserver implements TraceObserver {

    private final NfcoreConfigValidator configValidator
    
    NfcorePipelineObserver(NfcoreConfigValidator configValidator = new NfcoreConfigValidator()) {
        this.configValidator = configValidator
    }

    @Override
    void onFlowCreate(Session session) {
        def meta = session.getWorkflowMetadata()
        def config = session.config
        String projectName = null
        if (meta != null && meta.metaClass?.hasProperty(meta, 'projectName')) {
            projectName = meta.projectName
        }
        configValidator.checkConfigProvided(projectName, config)
        configValidator.checkProfileProvided(session.profile, session.commandLine)
        println "Pipeline is starting! 🚀"
    }

    @Override
    void onFlowComplete() {
        println "Pipeline complete! 👋"
    }
} 