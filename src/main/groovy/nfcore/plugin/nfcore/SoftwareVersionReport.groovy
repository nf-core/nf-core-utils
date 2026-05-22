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
import org.yaml.snakeyaml.Yaml

/**
 * Collects heterogeneous software version inputs and renders a merged YAML report.
 *
 * This is the canonical version-report interface described in ADR-0001 §3.
 * {@link NfcoreVersionUtils#collectVersions} delegates here; deprecated methods
 * in NfcoreVersionUtils remain as thin adapters.
 *
 * Supported input types via {@link #addInput}:
 * <ul>
 *   <li>YAML strings (inline content or file path)</li>
 *   <li>File / Path objects</li>
 *   <li>Topic tuples: [process, tool, version]</li>
 *   <li>Maps (tool->version or nested process blocks)</li>
 *   <li>Mixed lists of any of the above</li>
 * </ul>
 *
 * @see docs/adr/0001-prioritize-deep-modules-around-execution-context-and-reporting.md §3
 */
@Slf4j
class SoftwareVersionReport {

    private final Map<String, Map<String, Object>> merged = [:].withDefault { [:] as Map<String, Object> }
    private String workflowYaml = null

    /**
     * Add version data from any supported input type.
     * Can be called multiple times; inputs are merged.
     */
    void addInput(Object input) {
        List inputList = normalizeInputToList(input)
        inputList.each { entry -> processVersionEntry(entry) }
    }

    /**
     * Add workflow version section to the report.
     */
    void addWorkflowVersion(String workflowName, String workflowVersion, String nextflowVersion) {
        this.workflowYaml = """\
            Workflow:
                ${workflowName ?: 'unknown'}: ${workflowVersion ?: ''}
                Nextflow: ${nextflowVersion ?: 'unknown'}
            """.stripIndent().trim()
    }

    /**
     * Render collected versions as a sorted YAML string.
     */
    String renderYaml() {
        // Sort processes and tools alphabetically
        Map<String, Map<String, Object>> sorted = merged.sort().collectEntries { processName, toolsMap ->
            [(processName): toolsMap.sort()]
        }

        String versionsYaml = sorted ? new Yaml().dumpAsMap(sorted).trim() : ''

        if (workflowYaml) {
            return ([versionsYaml, workflowYaml].findAll { it && it != '{}' }.join("\n")).trim()
        }
        return versionsYaml
    }

    // =========================================================================
    // PRIVATE: input normalization and type dispatch
    // =========================================================================

    private static List normalizeInputToList(Object input) {
        if (input == null) return []
        if (input instanceof CharSequence) return [input.toString()]
        if (input instanceof Map) return [input]
        if (input instanceof List) return input
        if (input.getClass().isArray()) return (input as Object[]).toList()
        if (input instanceof Iterable) return input.toList()
        if (input.metaClass?.respondsTo(input, 'toList')) return input.toList()
        return [input]
    }

    private void processVersionEntry(Object entry) {
        if (entry == null) return
        try {
            if (entry instanceof CharSequence) {
                processStringEntry(entry.toString().trim())
            } else if (entry instanceof File) {
                processFileEntry(entry)
            } else if (entry instanceof Map) {
                mergeParsedYaml(entry as Map)
            } else if (hasToFileMethod(entry)) {
                processPathLikeEntry(entry)
            } else if (entry instanceof List || entry.getClass().isArray()) {
                processListEntry(entry)
            } else {
                processStringEntry(entry.toString())
            }
        } catch (Exception e) {
            log.warn("Could not process version entry ${entry}: ${e.message}")
        }
    }

    private void processStringEntry(String s) {
        if (!s) return
        def f = new File(s)
        if (f.exists() && f.isFile()) {
            processYamlContent(f.text)
        } else {
            processYamlContent(s)
        }
    }

    private void processFileEntry(File file) {
        if (file.exists() && file.isFile()) {
            processYamlContent(file.text)
        }
    }

    private void processPathLikeEntry(Object entry) {
        try {
            def f = entry.toFile()
            if (f.exists() && f.isFile()) {
                processYamlContent(f.text)
            }
        } catch (Exception e) {
            def f = new File(entry.toString())
            if (f.exists() && f.isFile()) {
                processYamlContent(f.text)
            }
        }
    }

    private void processListEntry(Object entry) {
        def list = (entry instanceof List) ? (List) entry : (entry as Object[]).toList()
        if (list.isEmpty()) return

        if (list[0] instanceof List) {
            list.each { processVersionEntry(it) }
            return
        }

        // Topic tuple: [process, tool, version]
        if (list.size() >= 3) {
            def procRaw = list[0]?.toString() ?: ''
            def processName = procRaw.contains(':') ?
                procRaw.substring(procRaw.lastIndexOf(':') + 1) : procRaw
            def tool = list[1]?.toString()
            def version = list[2]
            if (processName && tool) {
                merged[processName][tool] = version instanceof CharSequence ?
                    version.toString() : version
            }
        }
    }

    private void processYamlContent(String yamlContent) {
        def processed = flattenYamlKeys(yamlContent)
        if (processed) {
            def map = new Yaml().load(processed)
            if (map instanceof Map) {
                mergeParsedYaml(map as Map)
            }
        }
    }

    /** Flatten colon-prefixed keys: "tool:foo: 1.0" -> "foo: 1.0" */
    private static String flattenYamlKeys(String yamlContent) {
        def yaml = new Yaml()
        def loaded = yaml.load(yamlContent)
        if (!(loaded instanceof Map)) return ''
        def versions = ((Map) loaded).collectEntries { k, v ->
            if (k instanceof String) {
                [k.tokenize(':')[-1], v]
            } else {
                [k, v]
            }
        }
        return yaml.dumpAsMap(versions).trim()
    }

    private void mergeParsedYaml(Map parsed) {
        if (!parsed) return
        def hasNested = parsed.values().any { it instanceof Map }
        if (hasNested) {
            parsed.each { pk, pv ->
                if (pv instanceof Map) {
                    mergeProcessMap(pk?.toString(), pv as Map)
                } else if (pk) {
                    mergeProcessMap('Software', [(pk.toString()): pv])
                }
            }
        } else {
            mergeProcessMap('Software', parsed)
        }
    }

    private void mergeProcessMap(String processName, Map toolsMap) {
        if (!processName || !toolsMap) return
        toolsMap.each { tk, tv ->
            def toolKey = tk ? (tk.toString().contains(':') ?
                tk.toString().tokenize(':').last() : tk.toString()) : null
            if (toolKey) {
                merged[processName][toolKey] = (tv instanceof CharSequence) ? tv.toString() : tv
            }
        }
    }

    private static boolean hasToFileMethod(Object o) {
        if (o == null) return false
        try {
            def m = o.getClass().getMethod('toFile' as String)
            return m != null && m.getParameterCount() == 0
        } catch (NoSuchMethodException | SecurityException ignore) {
            return false
        }
    }
}
