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

/**
 * Adapts config validation @Function calls using PipelineExecutionContext
 * instead of reaching into Session directly.
 *
 * @see docs/adr/0001-prioritize-deep-modules-around-execution-context-and-reporting.md §5
 */
class ValidationAdapter {

    private final PipelineExecutionContext ctx

    ValidationAdapter(PipelineExecutionContext ctx) {
        this.ctx = ctx
    }

    boolean checkConfigProvided() {
        return NfcoreConfigValidator.checkConfigProvided(ctx.projectName, ctx.config)
    }

    void checkProfileProvided(List args, boolean monochromeLogs = true) {
        String profile = null
        for (int i = 0; i < args.size(); i++) {
            if (args[i] == '-profile' && i + 1 < args.size()) {
                profile = args[i + 1]
                break
            }
        }
        String commandLine = args.join(' ')
        NfcoreConfigValidator.checkProfileProvided(profile, commandLine, monochromeLogs)
    }
}
