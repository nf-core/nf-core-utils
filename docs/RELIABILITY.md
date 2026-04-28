# RELIABILITY.md — Reliability Standards

> nf-core-utils is infrastructure. Pipelines running in production HPC clusters and cloud environments
> depend on it. Reliability is a first-class concern.

---

## 1. Reliability Contract

| Concern | Commitment |
|---------|-----------|
| **No silent data loss** | Functions that write files (e.g. `dumpParametersToJSON`) must not silently swallow write errors |
| **No pipeline crashes from advisory checks** | Validation functions that are non-fatal (`checkCondaChannels`) must not throw unhandled exceptions |
| **Nextflow version compatibility** | The plugin must work against the declared `nextflowVersion` floor in `build.gradle` |
| **Idempotency** | Functions called multiple times with the same input must produce the same output |
| **No shared mutable state** | Utility classes must be safe to call concurrently within a Nextflow session |

---

## 2. Failure Mode Catalogue

| Function | Possible failure | Expected behaviour |
|----------|-----------------|-------------------|
| `dumpParametersToJSON` | `outdir` does not exist | Create parent dirs; if creation fails, log warning and continue |
| `checkCondaChannels` | `conda` binary not on PATH | Return `true` (non-blocking); log debug message |
| `completionEmail` | SMTP unreachable | Log warning; do not throw; do not crash pipeline |
| `imNotification` | Slack/Teams webhook misconfigured | Log warning; do not throw |
| `getReferencesFile` | genome key not found in igenomes map | Throw `AbortOperationException` with a clear message naming the missing key |

---

## 3. Testing for Reliability

- Every failure mode in the catalogue above must have a corresponding Spock test.
- Use Spock's `thrown()` assertion for expected exceptions.
- Mock external calls (SMTP, HTTP webhooks, `conda` binary) in tests — do not make network calls in unit tests.
- Use `@TempDir` or equivalent for file system tests to avoid polluting the build directory.

---

## 4. Observability

The plugin uses Nextflow's built-in `log` (SLF4J) at appropriate levels:

| Level | When to use |
|-------|-------------|
| `log.debug` | Verbose internal state useful only for debugging |
| `log.info` | Normal operational events (pipeline start, completion, version) |
| `log.warn` | Advisory issues that do not stop the pipeline |
| `log.error` | Fatal issues immediately before a controlled exit |

Do not use `println` — use `log.*` so output respects the user's Nextflow log configuration.

---

## 5. Compatibility Testing

Before releasing a new version:

1. Test against the **minimum** declared `nextflowVersion` (see `build.gradle`).
2. Test against the **latest stable** Nextflow release.
3. If either test fails, do not release until the compatibility issue is resolved or the version floor is updated with a clear CHANGELOG note.

---

## 6. Incident Process

If a released version causes pipeline failures in the wild:

1. Open a GitHub issue immediately with reproducible steps.
2. Tag the issue `severity:critical` if pipelines cannot run.
3. Prioritise a patch release over all other work.
4. Post-fix: add a regression test that would have caught the issue.
5. Document the incident in the relevant CHANGELOG entry.
