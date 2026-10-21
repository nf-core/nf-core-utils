# Version utilities

Use `collectVersions` to merge software versions from topic tuples, legacy YAML files, or both into one YAML report. The plugin adds workflow metadata automatically.

## `collectVersions()`

```nextflow
include { collectVersions } from 'plugin/nf-core-utils'

// Returns a YAML String, not a channel.
collectVersions(input)
collectVersions(input, nextflowVersion)
```

| Input                                      | Example                                                    |
| ------------------------------------------ | ---------------------------------------------------------- |
| YAML string                                | `'fastqc: 0.12.1'`                                         |
| Local filename, `File`, or Nextflow `Path` | `file('versions.yml')`                                     |
| List of topic tuples                       | `[['FASTQC', 'fastqc', '0.12.1']]`                         |
| Map                                        | `[fastqc: '0.12.1']` or `[FASTQC: [fastqc: '0.12.1']]`     |
| Mixed list                                 | `[file('versions.yml'), ['SAMTOOLS', 'samtools', '1.17']]` |

Pass tuples inside a list, even for one tuple. For remote files, pass a Nextflow `Path` from `file(...)` or a process output, not a URI string. The filesystem provider must be available in the running pipeline.

### Collect from a topic

Processes can publish `[process, tool, version]` tuples to the `versions` topic:

```nextflow
process FASTQC {
    input:
    path reads

    output:
    tuple val(task.process), val('fastqc'), eval("fastqc --version | sed 's/FastQC v//'"), topic: versions

    script:
    """
    fastqc ${reads}
    """
}

workflow {
    FASTQC(channel.fromPath(params.reads))

    channel.topic('versions')
        .toList()
        .map { versions -> collectVersions(versions) }
        .collectFile(
            name: 'software_versions.yml',
            storeDir: "${params.outdir}/pipeline_info",
            newLine: true
        )
}
```

Use **`.toList()`**, not `.collect()`: it preserves tuple boundaries and emits an empty list when no tasks publish versions, so the workflow metadata is still written. The report is produced after the topic closes. A process consuming this report must not also publish to the same topic, or the workflow can deadlock.

### Mix topic tuples and legacy files

A `versions` topic may contain both tuples and YAML `Path` outputs. Pass them through the same collection without branching or converting paths to strings.

For legacy modules that expose `versions.yml` through ordinary output channels, mix those outputs into the topic stream:

```nextflow
channel.topic('versions')
    .mix(LEGACY_TOOL.out.versions)
    .toList()
    .map { versions -> collectVersions(versions) }
    .collectFile(name: 'software_versions.yml', newLine: true)
```

If your pipeline uses a separate `versions_file` topic, mix `channel.topic('versions_file')` in the same way. For a pipeline without topics, start from its existing `ch_versions` channel and use `.toList().map { collectVersions(it) }`.

### Workflow metadata and output

```nextflow
// Optional positional override; this is not a named-options Map.
collectVersions(versions, workflow.nextflow.version)
```

Without an override, the Nextflow version comes from session configuration, `NXF_VER`, or the running Nextflow version. Workflow name, manifest version, and the run's git commit come from the session.

For input `[['PIPELINE:FASTQC', 'fastqc', '0.12.1'], [samtools: '1.17']]`, a report looks like:

```yaml
FASTQC:
  fastqc: 0.12.1
Software:
  samtools: "1.17"
Workflow:
  nf-core/mypipeline: v1.0.0-gabcdef1
  Nextflow: 26.10.2
```

- Processes and tools are sorted alphabetically; the workflow section follows them.
- Topic process names and YAML process keys lose their colon-delimited prefixes. Flat tool maps appear under `Software`.
- Repeated entries for the same process and tool occupy one key. **The last value wins**, even if the versions differ. Arrival order from mixed channels is not deterministic, so this is not a conflict-resolution policy. Keep distinct process names when different tool versions must be reported separately.
- Missing or malformed entries can be skipped; read/parse failures log warnings. This function is not a strict input validator.

## `getWorkflowVersion()`

```nextflow
include { getWorkflowVersion } from 'plugin/nf-core-utils'

getWorkflowVersion()                      // e.g. v1.2.3-gabcdef1
getWorkflowVersion('2.0.0')               // v2.0.0; does not append the run's commit
getWorkflowVersion('2.0.0', 'abcdef1234') // v2.0.0-gabcdef1
```

Without an explicit version, the function uses the workflow manifest version and, unless overridden, the run's commit. Commit IDs are shortened to at most seven characters.

## Migration from older functions

Existing exports remain available. Prefer `collectVersions` for new pipelines, but review the output shape when migrating: `processVersionsFromTopic` returns a flat tool map, and `processVersionsFromFile` preserves legacy YAML structure. They do not add workflow metadata. `collectVersions` groups tools by process and includes workflow metadata.

```nextflow
// Before: channel-aware compatibility API
softwareVersionsToYAML(ch_versions)

// After: collect the channel explicitly, preserving tuples and empty input
ch_versions.toList().map { collectVersions(it) }

// Before: already-collected lists of tuples and filenames
processMixedVersionSources(topicVersions, legacyFiles)

// After: one list containing both formats
collectVersions(topicVersions + legacyFiles)
```

| Deprecated export                                          | Replacement for already-collected input          |
| ---------------------------------------------------------- | ------------------------------------------------ |
| `softwareVersionsToYAML(versions, options?)`               | `collectVersions(versions, nextflowVersion?)`    |
| `processMixedVersionSources(topicVersions, versionsFiles)` | `collectVersions(topicVersions + versionsFiles)` |
| `processVersionsFromTopic(topicData)`                      | `collectVersions(topicData)`                     |
| `processVersionsFromFile(versionsFiles)`                   | `collectVersions(versionsFiles)`                 |

The conversion helpers `convertLegacyYamlToEvalSyntax(yamlContent, processName?)`, `generateYamlFromEvalSyntax(evalData, includeWorkflow?)`, and `workflowVersionToChannel()` also remain exported. The first two use flat tool mappings rather than `collectVersions`' process-grouped report; they are not required for mixed-input collection. `workflowVersionToChannel()` returns a list of tuples, not a channel.

`processVersionsFromYAML` and `workflowVersionToYAML` are internal utilities, **not plugin exports**. Do not import them from `plugin/nf-core-utils`.
