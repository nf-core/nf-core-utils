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

package nfcore.plugin.nfcore

import groovy.util.logging.Slf4j
import nextflow.Session

/**
 * Validates pipeline configurations and provides feedback
 */
@Slf4j
class NfcoreConfigValidator {

    /**
     * Checks if a custom config or profile has been provided, logs a warning if not.
     * @param projectName The project name
     * @param config The config map (should have profile and configFiles)
     * @return true if config is valid, false otherwise
     */
    static boolean checkConfigProvided(String projectName, Map config) {
        def profile = config?.get('profile') ?: 'standard'
        List configFiles = (config?.get('configFiles') instanceof List) ? (List) config.get('configFiles') : []
        if (profile == 'standard' && configFiles.size() <= 1) {
            log.warn("[${projectName}] You are attempting to run the pipeline without any custom configuration!\n\n" +
                    "This will be dependent on your local compute environment but can be achieved via one or more of the following:\n" +
                    "   (1) Using an existing pipeline profile e.g. `-profile docker` or `-profile singularity`\n" +
                    "   (2) Using an existing nf-core/configs for your Institution e.g. `-profile crick` or `-profile uppmax`\n" +
                    "   (3) Using your own local custom config e.g. `-c /path/to/your/custom.config`\n\n" +
                    "Please refer to the quick start section and usage docs for the pipeline.\n ")
            return false
        }
        return true
    }

    /**
     * Checks if the profile string is valid and warns about positional arguments.
     *
     * @param profile The profile string (e.g. from workflow.profile)
     * @param commandLine The command line string
     * @param monochromeLogs Whether to use monochrome logs (default: true)
     */
    static void checkProfileProvided(String profile, String commandLine, boolean monochromeLogs = true) {
        if (profile?.endsWith(',')) {
            def colors = nfcore.plugin.nfcore.NfcoreNotificationUtils.logColours(monochromeLogs)
            throw new IllegalArgumentException(
                    "${colors.red}ERROR${colors.reset} ~ The `-profile` option cannot end with a trailing comma, please remove it and re-run the pipeline!\n" +
                            "${colors.yellow}HINT${colors.reset}: A common mistake is to provide multiple values separated by spaces e.g. `-profile test, docker`.\n"
            )
        }
        if (commandLine != null && !commandLine.isEmpty()) {
            // Split the command line into arguments
            def args = commandLine.split(/\s+/)
            // Find the first positional argument (not starting with '-')
            def positional = args.find { !it.startsWith('-') }
            if (positional) {
                log.warn(
                        "nf-core pipelines do not accept positional arguments. The positional argument `${positional}` has been detected.\n" +
                                "HINT: A common mistake is to provide multiple values separated by spaces e.g. `-profile test, docker`.\n"
                )
            }
        }
    }

    /**
     * Validate nf-core CLI configuration
     * @param nextflowCliArgs List of positional nextflow CLI args
     */
    static void validateConfig(List nextflowCliArgs) {
        // Get session info
        def session = (Session) nextflow.Nextflow.session
        def profile = session.profile
        def commandLine = nextflowCliArgs ? nextflowCliArgs.join(' ') : null

        // Check profile
        checkProfileProvided(profile, commandLine, true)

        // Check config
        def meta = session.getWorkflowMetadata()
        def config = session.config
        String projectName = null
        if (meta != null && meta.metaClass?.hasProperty(meta, 'projectName')) {
            projectName = meta.projectName
        }
        checkConfigProvided(projectName, config)
    }
}
