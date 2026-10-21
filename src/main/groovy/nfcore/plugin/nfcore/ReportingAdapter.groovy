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
 * Adapts reporting @Function calls using PipelineExecutionContext
 * instead of reaching into Session directly.
 *
 * @see docs/adr/0001-prioritize-deep-modules-around-execution-context-and-reporting.md §5
 */
class ReportingAdapter {

    private final PipelineExecutionContext ctx

    ReportingAdapter(PipelineExecutionContext ctx) {
        this.ctx = ctx
    }

    Map generateComprehensiveReport(
        List<List> topicVersions,
        List<String> legacyVersions = [],
        List<String> metaFilePaths = [],
        File mqcMethodsYaml = null
    ) {
        return NfcoreReportingOrchestrator.generateComprehensiveReport(
            ctx, topicVersions, legacyVersions, metaFilePaths, mqcMethodsYaml
        )
    }

    Map generateVersionReport(List<List> topicVersions, List<String> legacyVersions = []) {
        return NfcoreReportingOrchestrator.generateVersionReport(ctx, topicVersions, legacyVersions)
    }

    Map generateCitationReport(List<String> metaFilePaths, File mqcMethodsYaml = null) {
        return NfcoreReportingOrchestrator.generateCitationReport(ctx, metaFilePaths, mqcMethodsYaml)
    }
}
