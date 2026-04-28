# Tech Debt Tracker

> Track known tech debt here. Each item has an ID, a severity, and a link to context.
> **Severity**: 🔴 Critical · 🟡 High · 🟢 Low  
> Move resolved items to the "Resolved" section with a date and the PR/commit that fixed it.

---

## Open Items

| ID | Severity | Summary | File(s) | Opened |
|----|----------|---------|---------|--------|
| TD-001 | 🟢 Low | No integration test that exercises the plugin against a real Nextflow run in CI | `.github/workflows/` | 2025-01 |
| TD-002 | 🟢 Low | `docs/generated/db-schema.md` is hand-maintained; should be auto-generated | `docs/generated/` | 2025-01 |
| TD-003 | 🟡 High | Observer pattern is not covered by dedicated unit tests | `NfcorePipelineObserver.groovy` | 2025-01 |

---

## Resolved Items

| ID | Summary | Resolved | PR/Commit |
|----|---------|----------|-----------|
| — | — | — | — |

---

## How to Add a New Item

1. Assign the next available `TD-NNN` ID.
2. Choose severity honestly: 🔴 if it will cause production failures, 🟡 if it degrades maintainability significantly, 🟢 for low-impact items.
3. Link to the relevant files or design docs.
4. If you fix an item, move it to "Resolved" with the date and commit/PR reference — do **not** delete it.
