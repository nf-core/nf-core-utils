# Product Specs — Index

> Product specs describe **what** we are building from a user/pipeline-author perspective and **why**.
> They are distinct from design docs (which describe architectural decisions) and exec plans (which describe *how* to implement something).

---

## Active Specs

| # | Title | Status | Owner |
|---|-------|--------|-------|
| PS-001 | [New User Onboarding](new-user-onboarding.md) | Draft | — |

---

## Spec Lifecycle

```
Draft → Review → Accepted → In Development → Shipped → Archived
```

- **Draft**: Author is still writing; not ready for implementation.
- **Review**: Spec is complete; seeking feedback before commit.
- **Accepted**: Approved for implementation; linked exec plan should exist in `docs/exec-plans/active/`.
- **Shipped**: Feature is live in a released version.
- **Archived**: Spec was rejected or superseded.

---

## Template

```markdown
# PS-NNN — <Feature Name>

**Status**: Draft | Review | Accepted | Shipped | Archived  
**Version target**: <semver or "TBD">  
**Author(s)**: <name>

## Problem
What user pain does this address?

## Goals
What does success look like?

## Non-Goals
What is explicitly out of scope?

## User Stories
- As a pipeline author, I want … so that …

## Acceptance Criteria
- [ ] Concrete, testable conditions for "done"

## Open Questions
- Unresolved decisions that need answers before implementation
```
