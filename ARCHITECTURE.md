# ARCHITECTURE.md

High-level architecture of `nf-core-utils`.

## System Shape
- **Plugin layer**: PF4J + Nextflow extension entry points.
- **Utility layer**: stateless domain utilities (versioning, reporting, notifications, references).
- **Integration layer**: Nextflow runtime hooks/observers.

## Source Layout
- `src/main/groovy/nfcore/plugin/` — plugin + extension entry points
- `src/main/groovy/nfcore/plugin/nfcore/` — nf-core utility modules
- `src/main/groovy/nfcore/plugin/nextflow/` — Nextflow-specific helpers
- `src/main/groovy/nfcore/plugin/references/` — reference resolution
- `src/test/groovy/` — Spock tests

## Architectural Rules
- Keep extension functions thin; delegate to utility classes.
- Prefer pure/stateless helpers to simplify tests.
- Public behavior changes require tests + documentation.

## Related Docs
- Design rationale: `docs/DESIGN.md`
- Quality targets: `docs/QUALITY_SCORE.md`
- Reliability posture: `docs/RELIABILITY.md`
- Security posture: `docs/SECURITY.md`
