# AGENTS.md — Autonomous Contributor Guide

> **Audience**: AI agents (Claude, Codex, Gemini, etc.) and autonomous contributors.
> **Purpose**: Tell every agent exactly how to orient, navigate, and deliver work in this repo without human hand-holding.

---

## 1. What This Repo Is

**nf-core-utils** is a [Nextflow PF4J plugin](https://www.nextflow.io/docs/latest/plugins.html) that ships utility functions used by nf-core community pipelines. It replaces the legacy `utils_nextflow_pipeline` and related subworkflows with a clean, versioned plugin surface.

Key domains:

| Domain | Entry point | Purpose |
|--------|-------------|---------|
| Pipeline utilities | `NfUtilsExtension.groovy` | Version strings, param export, conda validation |
| Configuration validation | `NfcoreConfigValidator` | Profile & config correctness |
| Notifications | `NfcoreNotificationUtils` | Email, Slack, Teams, terminal summaries |
| Reporting / MultiQC | `NfcoreReportingOrchestrator` | Versions, citations, bibliography, methods |
| References | `ReferencesUtils` | igenomes + custom reference resolution |

See [`ARCHITECTURE.md`](ARCHITECTURE.md) for the structural map and [`docs/DESIGN.md`](docs/DESIGN.md) for design rationale.

---

## 2. Orientation Checklist

Before touching code, an agent SHOULD:

1. Read [`CLAUDE.md`](CLAUDE.md) — build commands, test commands, key config files.
2. Read [`ARCHITECTURE.md`](ARCHITECTURE.md) — layer diagram and module map.
3. Read [`docs/DESIGN.md`](docs/DESIGN.md) — design constraints and patterns.
4. Skim [`CHANGELOG.md`](CHANGELOG.md) — understand recent velocity.
5. Check [`docs/exec-plans/active/`](docs/exec-plans/active/) — are there in-flight plans that affect your task?

---

## 3. Development Workflow

```
make assemble    # compile
make test        # run Spock unit tests
make install     # install plugin locally for integration testing
nextflow run hello -plugins nf-core-utils@<version>   # smoke test
```

All tests live in `src/test/groovy/nfcore/plugin/` and use **Spock** framework.

---

## 4. Code Conventions

- **Language**: Groovy (JVM); Nextflow DSL2 in examples.
- **Style**: Static utility methods preferred; extension methods delegate to utility classes for testability.
- **Statefulness**: Utility classes are intentionally stateless.
- **No breaking changes** to public `@Function`-annotated methods without a major version bump.
- Follow existing package naming: `nfcore.plugin.*`.

---

## 5. PR / Commit Norms

- Atomic commits; one logical change per commit.
- Commit message: `<type>(<scope>): <short description>` (Conventional Commits).
- Update `CHANGELOG.md` under `[Unreleased]` for every user-visible change.
- New public functions require at minimum a Spock unit test and a docs entry under `docs/`.

---

## 6. Doc Locations

| Question | Where to look |
|----------|---------------|
| What are we building next? | [`docs/exec-plans/active/`](docs/exec-plans/active/) |
| Why does X work this way? | [`docs/design-docs/`](docs/design-docs/) |
| What are tech-debt items? | [`docs/exec-plans/tech-debt-tracker.md`](docs/exec-plans/tech-debt-tracker.md) |
| Security posture | [`docs/SECURITY.md`](docs/SECURITY.md) |
| Quality standards | [`docs/QUALITY_SCORE.md`](docs/QUALITY_SCORE.md) |
| Reliability targets | [`docs/RELIABILITY.md`](docs/RELIABILITY.md) |

---

## 7. Things Agents Must NOT Do

- Do **not** delete or rename existing `docs/` files without explicit instruction.
- Do **not** modify `version` in `build.gradle` unless executing a release plan.
- Do **not** add external runtime dependencies without updating `docs/DESIGN.md` and the tech-debt tracker.
- Do **not** merge work that reduces test coverage below the current baseline.
