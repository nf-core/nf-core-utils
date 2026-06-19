# Version Utilities

Software version collection and reporting for nf-core pipelines.

## 1. Overview

The `NfcoreVersionUtils` utility handles collecting, merging, and formatting software versions from pipeline processes. It supports legacy YAML files, modern topic channel tuples, and any combination of the two.

### 1.1. Recommended API

**`collectVersions(input, nextflowVersion?)`** is the single entry point for version collection. It accepts any mix of input formats and returns a sorted YAML string with workflow metadata included.

For workflow version strings (e.g. `v1.2.3-gabcdef1`), use **`getWorkflowVersion(version?, commitId?)`**.

## 2. `collectVersions`

Collects software versions from heterogeneous sources and merges them into a single YAML string.

```nextflow
include { collectVersions } from 'plugin/nf-core-utils'
```

### 2.1. Supported Input Types

| Type | Example |
|------|---------|
| `String` | YAML content or file path (auto-detected) |
| `File` / `Path` | Reads YAML from the file |
| `List<List>` | Topic channel tuples `[[process, tool, version], ...]` |
| `List<String>` | Paths to `versions.yml` files |
| `Map` | Direct version data `[tool: version]` |
| Mixed `List` | Any combination of the above |

### 2.2. Basic Usage

```nextflow
workflow {
    ch_versions = Channel.empty()

    FASTQC(samples)
    SAMTOOLS(mapped)

    ch_versions = ch_versions
        .mix(FASTQC.out.versions)
        .mix(SAMTOOLS.out.versions)

    // Collect all versions into YAML
    ch_versions.collect().map { versions ->
        collectVersions(versions)
    }.set { ch_versions_yaml }

    ch_versions_yaml.collectFile(
        name: 'software_versions.yml',
        storeDir: "${params.outdir}/pipeline_info"
    )
}
```

### 2.3. Custom Nextflow Version

```nextflow
collectVersions(versions, nextflowVersion: '25.04.0')
```

### 2.4. Output Format

```yaml
FASTQC:
  fastqc: 0.12.1
SAMTOOLS:
  samtools: 1.17
Workflow:
  nf-core/mypipeline: v1.0.0
  Nextflow: 25.04.0
```

Processes and tools are sorted alphabetically. Duplicates are merged automatically. Workflow name, version, and Nextflow version are appended.

## 3. `getWorkflowVersion`

Returns a formatted version string combining the manifest version with the git commit ID.

```nextflow
include { getWorkflowVersion } from 'plugin/nf-core-utils'

def version = getWorkflowVersion()
// => "v1.2.3-gabcdef1"
```

Both parameters are optional — defaults come from the workflow session metadata.

```nextflow
// Explicit override
getWorkflowVersion('2.0.0', 'abc123')
```

## 4. Helper Functions

These are public `@Function` methods that may be useful during migration but are not needed for most pipelines.

| Function | Purpose |
|----------|---------|
| `convertLegacyYamlToEvalSyntax(yamlContent, processName?)` | Converts YAML string to `[process, tool, version]` tuples |
| `generateYamlFromEvalSyntax(evalData, includeWorkflow?)` | Converts tuples back to YAML for reporting |
| `workflowVersionToChannel()` | Returns workflow metadata as `[process, name, version]` tuples |

## 5. Migration from Legacy APIs

### 5.1. From `softwareVersionsToYAML`

```nextflow
// Before (deprecated)
softwareVersionsToYAML(ch_versions)

// After
ch_versions.collect().map { collectVersions(it) }
```

### 5.2. From `processVersionsFromTopic` / `processVersionsFromFile`

```nextflow
// Before (deprecated)
processVersionsFromTopic(topicData)
processVersionsFromFile(fileList)

// After — collectVersions handles both formats
collectVersions(topicData)
collectVersions(fileList)
```

### 5.3. From `processMixedVersionSources`

```nextflow
// Before (deprecated)
processMixedVersionSources(topicVersions, legacyFiles)

// After — just combine the inputs
collectVersions(topicVersions + legacyFiles)
```

## 6. Deprecated Functions

The following `@Function` methods still work but are deprecated in favour of `collectVersions`:

- `softwareVersionsToYAML(versions, options?)` — use `collectVersions(input)`
- `processMixedVersionSources(topicVersions, versionsFiles)` — use `collectVersions(combined)`
- `processVersionsFromTopic(topicData)` — use `collectVersions(topicData)`
- `processVersionsFromFile(versionsFiles)` — use `collectVersions(versionsFiles)`

> **Note:** `processVersionsFromYAML` and `workflowVersionToYAML` are internal utilities, not plugin `@Function` calls. Do not import them from `plugin/nf-core-utils`.
