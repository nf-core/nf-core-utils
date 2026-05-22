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
 * Adapts version @Function calls using PipelineExecutionContext
 * instead of reaching into Session directly.
 *
 * @see docs/adr/0001-prioritize-deep-modules-around-execution-context-and-reporting.md §5
 */
class VersionAdapter {

    private final PipelineExecutionContext ctx

    VersionAdapter(PipelineExecutionContext ctx) {
        this.ctx = ctx
    }

    String getWorkflowVersion(String version = null, String commitId = null) {
        def ver = version ?: ctx.workflowVersion
        return NfcoreVersionUtils.getWorkflowVersion(null, ver, commitId)
    }

    String collectVersions(Object input, Object nextflowVersion = null) {
        def report = new SoftwareVersionReport()
        report.addInput(input)

        def wfVersion = getWorkflowVersion()
        def nfVer = nextflowVersion?.toString() ?: ctx.nextflowVersion ?: 'unknown'
        report.addWorkflowVersion(ctx.workflowName, wfVersion, nfVer)

        return report.renderYaml()
    }
}
