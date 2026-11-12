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

## Boolean CLI parameter compatibility

The public `NfUtilsExtension` functions normalize CLI-shaped values before
delegating to boolean-typed utilities. For `completionEmail`'s `plaintextEmail`
and `monochromeLogs`, `completionSummary`'s `monochromeLogs`, and
`checkProfileProvided`'s `monochromeLogs`, `BooleanParam` accepts Booleans,
case-insensitive true/false strings (trimmed), and null defaults. It deliberately
does not use Groovy truth: the string `"false"` must remain false.

Conversion and failure policy are separate: startup validation rejects invalid
values; completion handlers warn and use defaults so malformed presentation flags
cannot suppress reports or notifications. Defaults belong to the public function:
HTML email (`plaintextEmail = false`) and monochrome logs (`monochromeLogs = true`).

Keep internal utility arguments typed as booleans rather than propagating raw
parameter values or making pipeline callers repeat coercion. Typed pipeline params
remain recommended, but legacy untyped callers are supported at this interface.
Groovy dispatch tests protect conversion and delivery behavior; the strict-CLI
validation check protects Nextflow import/registration and actual string inputs.

## Related Docs

- Design rationale: `docs/DESIGN.md` and ADRs in `docs/adr/`
- Quality targets: `docs/QUALITY_SCORE.md`
- Reliability posture: `docs/RELIABILITY.md`
- Security posture: `docs/SECURITY.md`
