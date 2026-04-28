# PLANS.md — Roadmap & Execution Plans

> High-level roadmap. Detailed execution plans live in [`docs/exec-plans/active/`](exec-plans/active/).
> This file is the navigational index — keep it short and current.

---

## Current Focus (Active)

| Plan | Summary | Status |
|------|---------|--------|
| (none active) | — | — |

Add a row here when you create a file in `docs/exec-plans/active/`.

---

## Near-Term Priorities

1. **Integration test in CI** — Run a real `nextflow run hello` smoke test against the installed plugin in GitHub Actions. (See [TD-001](exec-plans/tech-debt-tracker.md))
2. **Observer unit test coverage** — Add Spock specs for `NfcorePipelineObserver` lifecycle methods. (See [TD-003](exec-plans/tech-debt-tracker.md))
3. **Quick-start onboarding** — Implement acceptance criteria from [PS-001](product-specs/new-user-onboarding.md).
4. **Auto-generated function reference** — Generate the public function table in `docs/FRONTEND.md` from source rather than maintaining it manually.

---

## Completed Plans

| Plan | Summary | Shipped version |
|------|---------|-----------------|
| (none yet) | — | — |

Move completed plans from `docs/exec-plans/active/` → `docs/exec-plans/completed/` and add a row here.

---

## How to Create an Execution Plan

1. Create `docs/exec-plans/active/YYYY-MM-<kebab-title>.md`.
2. Use this template:

```markdown
# Exec Plan: <Title>

**Status**: In Progress | Blocked | Review  
**Target version**: <semver>  
**Created**: YYYY-MM-DD  

## Objective
One-sentence goal.

## Tasks
- [ ] Task 1
- [ ] Task 2

## Definition of Done
- Acceptance criteria (what "complete" means)

## Risks / Blockers
- Known risks or dependencies
```

3. Link the plan from the "Current Focus" table above.
4. When shipped: move the file to `docs/exec-plans/completed/`, update `CHANGELOG.md`, and add a row to "Completed Plans".
