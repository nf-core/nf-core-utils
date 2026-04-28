# PRODUCT_SENSE.md — Product Intuition Guide

> Helps autonomous contributors make good product decisions without asking for approval on every choice.
> Read this before deciding whether to add a feature, change behaviour, or remove something.

---

## 1. Who Are Our Users?

### Primary: nf-core Pipeline Authors
- Write Nextflow DSL2 pipelines for bioinformatics workflows.
- Range from novice (following nf-core templates) to expert (building complex multi-step analyses).
- Care deeply about reproducibility, community standards, and not breaking their CI.
- Pain point: boilerplate functions that have to be copy-pasted into every pipeline.

### Secondary: nf-core Community Reviewers
- Review pull requests against nf-core linting standards.
- Need the plugin to enforce conventions consistently so review is predictable.

### Tertiary: Autonomous Agents / CI Bots
- Run automated pipeline checks.
- Need machine-readable, stable APIs with clear error messages.

---

## 2. What Does "Good" Look Like?

A good addition to nf-core-utils:

- **Eliminates boilerplate** that pipeline authors currently copy-paste.
- **Has at least 3 pipelines** that could immediately use it (or is mandated by nf-core standards).
- **Doesn't surprise callers** — naming is self-documenting, behaviour matches expectation.
- **Is independently testable** — doesn't require a full pipeline execution to unit test.
- **Has stable API** — if it might change in the next release, mark it `@Incubating`.

A bad addition:
- Solves a problem for one pipeline but not others.
- Requires adding a new external library with unclear maintenance story.
- Changes existing function signatures in a breaking way without a major version bump.
- Is a workaround for a Nextflow bug that should be fixed upstream.

---

## 3. Decision Heuristics

### "Should we add this function?"
> Does it replace something currently duplicated across ≥3 nf-core pipelines?
> → Yes → add it (with tests + docs).
> → No → probably belongs in the individual pipeline, not here.

### "Should we change this function's signature?"
> Will it break existing pipeline code?
> → Yes → this requires a major version bump + deprecation of the old form.
> → No → it's a compatible addition; add it with a minor version bump.

### "Should we add this dependency?"
> Is there a stdlib/SnakeYAML equivalent that covers 80% of the need?
> → Yes → use it.
> → No → write a design doc and get explicit approval before adding.

### "Should we emit a warning vs. throw an error?"
> Is the misconfiguration advisory (pipeline can still succeed)?
> → Yes → `log.warn`; return `false`; let the caller decide.
> → No → throw / `System.exit(1)`; don't let the pipeline proceed silently broken.

---

## 4. The nf-core Standard as North Star

When in doubt, ask: *"What would the nf-core linter expect here?"*  
The plugin's purpose is to make it easy to be nf-core compliant. If a feature helps with that, it belongs here. If it diverges from nf-core conventions, it almost certainly doesn't.

Reference: https://nf-co.re/docs/contributing/guidelines
