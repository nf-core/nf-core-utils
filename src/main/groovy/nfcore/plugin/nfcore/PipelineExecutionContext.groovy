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

import nextflow.Session

/**
 * Concentrates the Nextflow runtime facts that nf-core utility modules need.
 *
 * Callers depend on plain domain properties instead of reaching into
 * {@link nextflow.Session}, {@code nextflow.Nextflow.session}, or
 * {@code WorkflowMetadata} directly.
 *
 * The {@link #fromSession} factory is the only place that knows how to
 * read a live Session; everything else works with this value object,
 * which is trivial to construct in tests.
 *
 * @see docs/adr/0001-prioritize-deep-modules-around-execution-context-and-reporting.md §1
 */
class PipelineExecutionContext {

    /** Pipeline name from manifest (e.g. "nf-core/rnaseq"). Defaults to "unknown". */
    final String workflowName

    /** Pipeline version from manifest (e.g. "3.14.0"). */
    final String workflowVersion

    /** Project name from WorkflowMetadata, when available. */
    final String projectName

    /** Active Nextflow profile string (e.g. "test,docker"). */
    final String profile

    /** Config files loaded by Nextflow. */
    final List<String> configFiles

    /** Full Nextflow config map. */
    final Map config

    /** Nextflow engine version (from config or NXF_VER). */
    final String nextflowVersion

    /** Manifest key-value pairs as a plain map. */
    final Map manifestMap

    /** Workflow metadata key-value pairs as a plain map. */
    final Map workflowMap

    /**
     * Construct from explicit properties. Missing keys get safe defaults.
     * Intended for test construction and internal use.
     */
    PipelineExecutionContext(Map props = [:]) {
        this.workflowName = (props.workflowName as String) ?: 'unknown'
        this.workflowVersion = props.workflowVersion as String
        this.projectName = props.projectName as String
        this.profile = props.profile as String
        this.configFiles = (props.configFiles as List<String>) ?: []
        this.config = (props.config as Map) ?: [:]
        this.nextflowVersion = props.nextflowVersion as String
        this.manifestMap = (props.manifestMap as Map) ?: [:]
        this.workflowMap = (props.workflowMap as Map) ?: [:]
    }

    /**
     * Session adapter — the single place that reads a live Nextflow Session.
     *
     * @param session a Nextflow Session, or null for safe defaults
     * @return a fully populated context
     */
    static PipelineExecutionContext fromSession(Session session) {
        if (session == null) {
            return new PipelineExecutionContext()
        }

        def manifest = session.getManifest()
        def cfg = session.getConfig() ?: [:]

        def workflowMetadata = null
        try {
            workflowMetadata = session.getWorkflowMetadata()
        } catch (Exception ignored) {}

        // Extract projectName safely — property may not exist on all metadata versions
        String projectName = null
        if (workflowMetadata instanceof Map) {
            projectName = workflowMetadata.get('projectName')?.toString()
        } else if (workflowMetadata != null && workflowMetadata.metaClass?.hasProperty(workflowMetadata, 'projectName')) {
            projectName = workflowMetadata.projectName?.toString()
        }

        // Nextflow version: config > NXF_VER env var
        def nfVer = (cfg.get('nextflow') as Map)?.get('version')?.toString()
            ?: System.getenv('NXF_VER')

        return new PipelineExecutionContext(
            workflowName: manifest?.getName() ?: 'unknown',
            workflowVersion: manifest?.getVersion()?.toString(),
            projectName: projectName,
            profile: session.getProfile(),
            configFiles: (cfg.get('configFiles') instanceof List) ? (List<String>) cfg.get('configFiles') : [],
            config: cfg,
            nextflowVersion: nfVer,
            manifestMap: safeManifestMap(manifest),
            workflowMap: safeWorkflowMap(workflowMetadata)
        )
    }

    private static Map safeManifestMap(def manifest) {
        if (manifest == null) {
            return [:]
        }
        try {
            return manifest.toMap() ?: [:]
        } catch (Exception ignored) {
            return [:]
        }
    }

    private static Map safeWorkflowMap(def meta) {
        try {
            if (meta instanceof Map) {
                return meta as Map
            }
            return meta?.toMap() ?: [:]
        } catch (Exception ignored) {
            return [:]
        }
    }
}
