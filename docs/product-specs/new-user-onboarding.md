# PS-001 — New User Onboarding

**Status**: Draft  
**Version target**: TBD  
**Author(s)**: nf-core-utils team

---

## Problem

Pipeline authors who are new to nf-core must currently read multiple scattered documents to understand how to adopt the nf-core-utils plugin. There is no single "getting started" path that:

1. Explains what the plugin provides.
2. Shows the minimal import to make a pipeline nf-core-compliant.
3. Validates that the setup is correct before a full pipeline run.

This results in misconfigured plugins, missed features, and community support burden.

---

## Goals

- A new pipeline author can go from zero to a working plugin import in under 10 minutes.
- The onboarding path is self-serve; no community support needed for the happy path.
- The plugin surface feels discoverable — authors can find additional features without reading every doc page.

---

## Non-Goals

- This spec does not cover migration from legacy subworkflows (see migration guide in `docs/NextflowPipelineExtension.md`).
- It does not address CI/CD pipeline configuration or nf-core linting rules.

---

## User Stories

- As a **new pipeline author**, I want a single page that shows me the minimum viable import, so that I can get started without reading all the docs first.
- As an **existing nf-core contributor**, I want to verify at a glance which plugin version adds a feature I need, so that I can set the correct version constraint.
- As an **autonomous agent** contributing to a pipeline, I want a machine-readable list of available `@Function` names and their signatures, so that I can construct correct import statements.

---

## Acceptance Criteria

- [ ] A "Quick Start" section exists at the top of `README.md` that shows the minimal import and a working pipeline snippet.
- [ ] All public `@Function` names are listed in a single reference table (in `docs/` or `ARCHITECTURE.md`) with parameter types and return types.
- [ ] An automated check (e.g. a `make check-docs` target) verifies that the public function table is not stale relative to `NfUtilsExtension.groovy`.
- [ ] The onboarding path is validated with a new Spock integration test that runs the "Quick Start" snippet against a mock Nextflow session.

---

## Open Questions

1. Should the quick-start snippet live in `README.md` or in a dedicated `docs/QUICKSTART.md`?
2. Do we generate the function reference table from source (e.g. via Groovydoc) or maintain it manually?
3. Should onboarding include a `nf-core-utils validate` CLI command, or is the existing `checkConfigProvided()` function sufficient?
