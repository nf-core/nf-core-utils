# 001 — Core Beliefs & Design Principles

**Status**: Accepted  
**Date**: 2025-01  
**Author(s)**: nf-core-utils team

---

## Context

nf-core-utils exists to provide reliable, community-owned utility functions to nf-core pipelines through Nextflow's plugin system. As the project grows, we need an explicit statement of the values that shape every technical decision — so that autonomous contributors (human or AI) make consistent choices without constant hand-holding.

---

## Core Beliefs

### 1. Simplicity over cleverness
The plugin is infrastructure that hundreds of pipelines depend on. Surprising behaviour has outsized blast radius. Prefer the obvious implementation; reserve cleverness for places where it provably matters.

### 2. Testability is non-negotiable
Every public function must be independently testable without a live Nextflow session. This is why extension methods are thin delegates and business logic lives in stateless utility classes.

### 3. The plugin must not surprise its callers
- **No side effects** beyond what the function name implies.
- **No silent failures** — throw informative exceptions or return clearly typed results.
- **Backward compatibility** — existing `@Function` signatures never change without a major version bump.

### 4. Minimal runtime surface
Adding an external dependency is a tax on every pipeline that loads the plugin. A new dependency requires explicit justification in a design doc and a note in the tech-debt tracker.

Current runtime dependencies: `SnakeYAML` (required for YAML meta.yml parsing).

### 5. Document decisions, not just outcomes
Code explains *what*. Comments and docs explain *why*. ADRs capture the decision landscape so future contributors don't re-litigate resolved questions.

### 6. nf-core community standards first
The plugin's purpose is to serve the nf-core ecosystem. When community conventions conflict with local preferences, community conventions win.

---

## Consequences

- New features require at minimum: unit tests + docs entry + CHANGELOG update.
- Dependency additions require a design doc.
- Any change to a public `@Function` signature that could break existing callers requires a major version bump and a migration guide entry.

---

## Alternatives Considered

- **Merging utility logic back into subworkflows**: Rejected — subworkflows require copying into each pipeline; plugins are versioned and upgradeable centrally.
- **Single monolithic utility class**: Rejected — untestable sections would grow; separation by domain (versioning, notifications, reporting, …) keeps concerns isolated.
