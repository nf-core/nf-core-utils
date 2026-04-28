# Design Docs — Index

> This directory contains Architecture Decision Records (ADRs) and design explorations for nf-core-utils.
> Add a new file here whenever you make a non-trivial architectural choice; link it in the table below.

---

## Active Design Docs

| # | Title | Status | Date |
|---|-------|--------|------|
| 001 | [Core Beliefs & Design Principles](core-beliefs.md) | Accepted | 2025-01 |

---

## How to Write a Design Doc

1. Copy the template below into a new file: `docs/design-docs/<NNN>-<short-title>.md`.
2. Fill in all sections. "Context" and "Decision" are mandatory; others are encouraged.
3. Add a row to the table above and link to the file.
4. Design docs are **append-only** — amend by adding a new ADR that supersedes, not by editing the old one.

### Template

```markdown
# <NNN> — <Title>

**Status**: Proposed | Accepted | Deprecated | Superseded by #NNN  
**Date**: YYYY-MM-DD  
**Author(s)**: <name or "team">

## Context
What forces or constraints led to this decision?

## Decision
What did we decide?

## Consequences
What becomes easier? Harder? What must change?

## Alternatives Considered
What else was evaluated and why was it rejected?
```

---

## Conventions

- File names: `<NNN>-kebab-title.md` (zero-padded to three digits).
- Status moves from **Proposed → Accepted** when merged to main.
- Mark as **Deprecated** when a decision no longer applies; link the superseding doc.
