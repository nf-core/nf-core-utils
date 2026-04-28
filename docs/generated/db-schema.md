# Generated: Data / Schema Reference

> **Status**: Hand-maintained stub. See [TD-002](../exec-plans/tech-debt-tracker.md) for the plan to auto-generate this file.
>
> nf-core-utils is a stateless plugin — it has no persistent database. This file documents structured data contracts instead (YAML schemas, JSON parameter shapes, and key in-memory data structures).

---

## 1. Pipeline Parameters JSON (`dumpParametersToJSON` output)

**Path**: `<outdir>/pipeline_info/params_<timestamp>.json`

```json
{
  "<param_name>": "<value>",
  "..."
}
```

- All keys are taken directly from the Nextflow `params` map at run time.
- Values are serialised by SnakeYAML / Groovy JSON builder; types are preserved where serialisable.
- The file is timestamped (`yyyy-MM-dd_HH-mm-ss`) to allow multiple runs in the same output directory.

---

## 2. Versions YAML (MultiQC input)

Produced by `NfcoreVersionUtils`. Consumed by MultiQC for the software-versions table.

```yaml
# Example shape
<process_name>:
  <tool_name>: "<semver>"
```

---

## 3. Citations / Bibliography (meta.yml contract)

`NfcoreCitationUtils` reads citations from per-module `meta.yml` files. Expected shape:

```yaml
tools:
  - <tool_name>:
      description: "..."
      homepage: "https://..."
      documentation: "https://..."
      doi: "10.xxxx/..."
      licence: ["MIT"]
```

Fields `doi` and `licence` are used in the generated bibliography and methods section.

---

## 4. igenomes / References Config

`ReferencesUtils` resolves reference files from parameters matching the igenomes convention:

```
params.genome    → key into the igenomes map
params.<file_key> → optional override path
```

Resolved value is returned as a `Path` or `String` depending on the caller context.

---

## 5. Observer Session State

`NfcorePipelineObserver` holds transient per-session state (not persisted):

| Field | Type | Description |
|-------|------|-------------|
| `session` | `Session` | Active Nextflow session reference |
| Internal counters | `int` | Process-complete event tracking |

State is reset when the observer is re-created for a new session.
