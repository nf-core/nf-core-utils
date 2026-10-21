# src/test Agent Guide

Fast Spock specs for Groovy modules and public Nextflow extension adapters.

## Use this layer for

- Deep-module behavior in `src/main/groovy`.
- Public `@Function` adapter behavior and deprecated-method compatibility.
- Unit-testable regressions found by validation workflows.

## Spec metadata

New or substantially changed specs should use report-friendly Spock metadata:

- `@Title` — behavior under test.
- `@Narrative` — why it matters.
- `@Issue` — implementation PR or tracking issue.
- `@See` — exact ADR/design section anchor, not just the file URL.

## Style

- Prefer spec first, then production change.
- Keep extension specs thin; cover logic in the deep-module spec.
- Prefer explicit inputs/plain objects over global Nextflow state.
- Preserve public `@Function` behavior unless migration is planned.

## Checks

```bash
./gradlew test --tests 'nfcore.plugin.nfcore.PipelineExecutionContextTest'
./gradlew test
```
