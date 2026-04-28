# AGENTS.md

Guide for autonomous contributors (LLMs and humans).

## Mission
- Keep `nf-core-utils` stable, testable, and backwards compatible.
- Prefer small, reviewable changes with clear intent.

## Fast Start
1. Read `README.md`, `CLAUDE.md`, and `ARCHITECTURE.md`.
2. Check active plans in `docs/exec-plans/active/`.
3. Run:
   - `make assemble`
   - `make test`

## Guardrails
- Do not break public `@Function` behavior without migration notes.
- Add/update tests for any functional change.
- Update docs when behavior or API shape changes.
- Avoid adding dependencies without documenting rationale in `docs/design-docs/`.

## Definition of Done
- Code compiles.
- Relevant tests pass.
- Changelog/docs updated.
- Risk notes captured in the matching plan/spec if applicable.
