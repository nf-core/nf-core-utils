# DESIGN.md — Design Philosophy & Patterns

> Companion to [`ARCHITECTURE.md`](../ARCHITECTURE.md) and [`docs/design-docs/core-beliefs.md`](design-docs/core-beliefs.md).
> ARCHITECTURE.md describes structure; this file describes *how and why* we make design choices.

---

## 1. Design Principles (Short Form)

1. **Thin extension, fat utilities** — `NfUtilsExtension` is a switchboard; logic lives in testable utility classes.
2. **No surprises** — functions do exactly what their names say; no hidden side effects.
3. **Fail loudly, fail early** — throw informative exceptions rather than silently returning null or continuing in a broken state.
4. **Minimal dependencies** — every external library is a risk; justify additions in a design doc.
5. **Community standards first** — nf-core conventions override local preferences.

Full rationale: [`docs/design-docs/core-beliefs.md`](design-docs/core-beliefs.md).

---

## 2. Patterns in Use

### 2.1 Stateless Utility Classes

All business logic lives in classes with only `static` methods (or methods on stateless instances). This makes every function independently unit-testable with Spock without needing a live Nextflow session.

```groovy
// ✅ Correct pattern
class NfcoreVersionUtils {
    static String formatVersionYaml(Map versions) { … }
}

// ❌ Avoid
class NfcoreVersionUtils {
    Map state   // shared mutable state — don't do this
    String formatVersionYaml() { state.versions }
}
```

### 2.2 Extension as Delegate

`NfUtilsExtension` annotates methods with `@Function` for Nextflow discovery and immediately delegates to a utility class. This keeps the extension surface stable while allowing utility classes to evolve independently.

```groovy
@Function
String getWorkflowVersion(String manifestVersion, String commitId) {
    return NextflowPipelineUtils.getWorkflowVersion(manifestVersion, commitId)
}
```

### 2.3 Observer Pattern for Lifecycle Events

Pipeline lifecycle events (start, complete, process complete) are handled by `NfcorePipelineObserver` via Nextflow's trace subsystem. The observer delegates notifications and reporting to utility classes rather than implementing logic inline.

### 2.4 Non-blocking Validation

Validation functions (`checkCondaChannels`, `checkConfigProvided`) are designed to be non-blocking where failures are advisory rather than fatal. Return `boolean`; let the caller decide whether to abort.

---

## 3. Adding a New Feature — Design Checklist

- [ ] Does the feature belong in an existing utility class, or does it need a new one?
- [ ] Is the new function independently testable without a Nextflow session?
- [ ] Does it introduce a new external dependency? If yes → create a design doc first.
- [ ] Is the public API signature stable? If it might change → mark as `@Incubating` in Groovydoc.
- [ ] Does the function name follow verb-noun camelCase convention?
- [ ] Are there unit tests covering happy path + at least one failure mode?
- [ ] Is the function documented in `docs/`?

---

## 4. What Belongs in This Plugin vs. Elsewhere

| Belongs here | Does NOT belong here |
|---|---|
| Functions needed by ≥ 3 nf-core pipelines | Pipeline-specific business logic |
| Cross-cutting nf-core conventions | Workflow process definitions |
| Utilities that replace legacy subworkflows | nf-core schema validation (use nf-core CLI) |
| Reference file resolution (igenomes) | HPC/cloud-specific configuration |
