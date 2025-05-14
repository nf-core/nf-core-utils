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

package nfcore.plugin.util

import groovy.util.logging.Slf4j
import nextflow.Session

/**
 * Utility functions for nf-core pipelines
 */
@Slf4j
class NfcorePipelineUtils {

    /**
     * Checks if a custom config or profile has been provided, logs a warning if not.
     * @param projectName The project name
     * @param config The config map (should have profile and configFiles)
     * @return true if config is valid, false otherwise
     */
    static boolean checkConfigProvided() {
        def configValidator = new NfcoreConfigValidator()
        def session = (Session) nextflow.Nextflow.session
        def meta = session.getWorkflowMetadata()
        def config = session.config
        String projectName = null
        if (meta != null && meta.metaClass?.hasProperty(meta, 'projectName')) {
            projectName = meta.projectName
        }
        return configValidator.checkConfigProvided(projectName, config)
    }

    /**
     * Checks if the profile string is valid and warns about positional arguments.
     * @param args The command line arguments
     */
    static void checkProfileProvided(args) {
        def configValidator = new NfcoreConfigValidator()
        def session = (Session) nextflow.Nextflow.session
        def profile = session.profile
        def commandLine = args ? args.join(' ') : null
        configValidator.checkProfileProvided(profile, commandLine)
    }

    /**
     * Generate workflow version string from session manifest
     * @return Version string
     */
    static String getWorkflowVersion() {
        def session = (Session) nextflow.Nextflow.session
        return NfcoreVersionUtils.getWorkflowVersion(session)
    }
} 