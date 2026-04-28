# FRONTEND.md — User-Facing Interface Guide

> nf-core-utils is a Nextflow plugin library — it has no web frontend or GUI.
> "Frontend" in this context means **the public API surface** that pipeline authors interact with:
> the `@Function`-annotated methods in `NfUtilsExtension.groovy`.

---

## 1. Public Function Reference

All functions below are importable via:

```nextflow
include { <functionName> } from 'plugin/nf-core-utils'
```

### 1.1 NextflowPipelineExtension functions

| Function | Parameters | Returns | Description |
|----------|-----------|---------|-------------|
| `getWorkflowVersion` | `String manifestVersion, String commitId` | `String` | Git-aware version string (e.g. `v1.2.0-gabc1234`) |
| `dumpParametersToJSON` | `String outdir, Map params, Path launchDir` | `void` | Writes timestamped params JSON to `<outdir>/pipeline_info/` |
| `checkCondaChannels` | — | `boolean` | Validates conda-forge + bioconda channel order |

### 1.2 NfCoreUtilities functions

| Function | Parameters | Returns | Description |
|----------|-----------|---------|-------------|
| `checkConfigProvided` | `Map params, List<String> required` | `void` | Exits with error if required params are missing |
| `checkProfileProvided` | `String profile` | `void` | Warns if no institutional profile is detected |
| `completionEmail` | `Map params, Map summary, String multiqcReport` | `void` | Sends completion email if configured |
| `imNotification` | `Map summary, String hook_url` | `void` | Sends Slack/Teams notification |
| `completionSummary` | `Map summary` | `void` | Prints completion summary to terminal |

### 1.3 ReferencesExtension functions

| Function | Parameters | Returns | Description |
|----------|-----------|---------|-------------|
| `getReferencesFile` | `String genome, String key, Map params` | `Path` | Resolves a reference file path (igenomes or custom) |
| `getReferencesValue` | `String genome, String key, Map params` | `String` | Resolves a reference scalar value |

---

## 2. Version Compatibility

Plugin version is declared in `build.gradle` → `version`.  
Nextflow compatibility floor: `nextflowVersion` in `build.gradle` → `nextflowPlugin {}`.

When using this plugin in a pipeline's `nextflow.config`:

```groovy
plugins {
    id 'nf-core-utils@0.5.0'
}
```

---

## 3. Deprecation Policy

- Deprecated functions are annotated with `@Deprecated` in Groovydoc and emit a `log.warn` at call time.
- They are removed only in the next **major** version.
- Replacement guidance is always documented in the deprecation warning and in `CHANGELOG.md`.

---

## 4. Import Patterns

```nextflow
// Minimal — import only what you need
include { getWorkflowVersion } from 'plugin/nf-core-utils'

// Grouped — common pattern for full nf-core compliance
include {
    checkConfigProvided
    checkProfileProvided
    completionEmail
    completionSummary
    getWorkflowVersion
    dumpParametersToJSON
} from 'plugin/nf-core-utils'
```

---

## 5. Error Handling Contract

- Functions that validate state throw `nextflow.exception.AbortOperationException` (or call `System.exit(1)`) on fatal errors — pipeline will not proceed.
- Functions that check advisory state (e.g. `checkCondaChannels`) return `boolean` — pipeline continues regardless.
- Notification functions swallow errors silently and log a warning — a misconfigured Slack hook must not crash a pipeline.
