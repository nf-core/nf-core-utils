# ADR-0001: Prioritize deep modules around execution context and reporting

## Status

Proposed

## Context

`nf-core-utils` exposes Nextflow plugin functions for nf-core pipeline utilities: configuration validation, software version collection, citation/report generation, notifications, references, and workflow summaries.

Current architectural friction clusters around hidden or overly broad interfaces:

- Multiple modules reach directly into `nextflow.Session` or `nextflow.Nextflow.session` to extract workflow name, manifest data, profile, config files, command line, run status, and metadata.
- Reporting and notification helpers have small-looking method signatures but depend implicitly on global Nextflow runtime state.
- `NfcoreVersionUtils` has accumulated many supported input shapes, deprecated compatibility methods, YAML normalization rules, and workflow metadata concerns.
- `NfcoreReportingOrchestrator` exists as a possible reporting seam, but orchestration is still split across extension methods and utility modules.
- `NfUtilsExtension` is a large public adapter mixing current functions, deprecated functions, channel handling, file conversion, citations, reporting, and references.
- `ReferencesUtils` exposes low-level reference data shapes such as `params.genomes`, `referencesList` pairs, metadata maps, and `igenomes_base` substitution.

The project architecture already says to keep extension functions thin and delegate to utility classes. However, some utility modules are shallow: their interface requires callers and tests to know almost as much about Nextflow runtime objects and nf-core data shapes as the implementation does.

## Decision

We will prioritize architectural work by reducing hidden coupling first, then deepening higher-level reporting and compatibility seams.

Priority order:

1. Introduce a deep **pipeline execution context** module.
2. Replace global-session reporting helpers with explicit report inputs.
3. Deepen software version collection around a **software version report** module.
4. Decide whether reporting orchestration becomes the main report seam or is deleted.
5. Move extension compatibility into focused adapter modules.
6. Create a reference selection module.

This order is chosen because the execution context seam unlocks safer refactors in reporting, notifications, validation, citations, and tests. Version collection is a major complexity hotspot, but it should be tackled after the runtime-context seam exists. Extension splitting should happen after the deeper domain modules exist, otherwise it risks only rearranging shallow modules.

## Rationale

### 1. Pipeline execution context module

Create a module that exposes the nf-core facts needed by the rest of the codebase, such as workflow name, project name, profile, command line, config files, manifest map, workflow metadata, run status, and selected params.

`nextflow.Session` should become one adapter behind this seam.

Benefits:

- Improves locality by concentrating Nextflow runtime knowledge in one place.
- Improves leverage because callers use nf-core/domain facts rather than Nextflow object structure.
- Makes tests simpler: most tests can use a fake execution context instead of mocking `Session` or global `Nextflow.session`.

### 2. Explicit report inputs

Reporting, citation, and notification helpers should accept explicit inputs or execution context instead of reading `nextflow.Nextflow.session` directly.

Benefits:

- Makes hidden interfaces visible.
- Reduces global-state test setup.
- Allows report generation to become mostly pure behaviour.

### 3. Software version report module

`NfcoreVersionUtils` should keep compatibility methods, but the core behaviour should move behind a smaller canonical interface: collect heterogeneous software version inputs into a report and render it.

Benefits:

- Centralizes version input normalization and YAML shape rules.
- Keeps deprecated public methods as adapters.
- Makes the real test surface the version report interface rather than every historical helper.

### 4. Reporting orchestration seam

After execution context and version/citation inputs are clearer, decide whether `NfcoreReportingOrchestrator` should become the deep report assembly module or whether it should be deleted.

Benefits:

- Avoids preserving a weak hypothetical seam.
- Keeps report assembly rules in one place if the seam proves useful.

### 5. Extension adapter modules

Once deeper modules exist, split `NfUtilsExtension` delegation by domain: version adapter, citation adapter, reference adapter, reporting adapter, and validation adapter.

Benefits:

- Keeps the public Nextflow plugin interface stable while improving internal locality.
- Separates public compatibility/deprecation behaviour from domain behaviour.

### 6. Reference selection module

Reference handling can be deepened independently around the concept of a selected reference. This module should own genome lookup, metadata extraction, basepath substitution, and null/error behaviour.

Benefits:

- Concentrates reference-shape knowledge.
- Gives callers a smaller interface for reference file/value selection.

## Consequences

Positive consequences:

- Less duplicated knowledge of `Session`, manifest, workflow metadata, and config structure.
- More stable and AI-navigable seams.
- Tests can target interfaces that match domain behaviour.
- Future utility functions can compose deeper modules instead of expanding `NfUtilsExtension`.

Tradeoffs:

- Introduces new modules before deleting old compatibility paths.
- Some refactors will temporarily add adapter layers.
- Care is needed to preserve public `@Function` behaviour and deprecated methods.

## Implementation notes

Suggested migration slices:

1. Add the pipeline execution context module and a `Session` adapter without changing existing public behaviour.
2. Convert one or two global-session helpers to accept explicit inputs/context while keeping compatibility wrappers.
3. Migrate tests to prove the seam works.
4. Extract the software version report interface from `NfcoreVersionUtils`.
5. Decide whether to deepen or delete `NfcoreReportingOrchestrator`.
6. Slim `NfUtilsExtension` into focused adapters.
7. Tackle reference selection as an independent low-risk follow-up.

## Alternatives considered

### Split `NfUtilsExtension` first

Rejected for now. This would reduce file size, but without deeper modules it would mostly move shallow delegation into more files.

### Refactor `NfcoreVersionUtils` first

Deferred. It is a large complexity hotspot, but it touches many compatibility behaviours. The execution context seam should be established first so workflow metadata and reporting concerns do not remain tangled in the version module.

### Leave `NfcoreReportingOrchestrator` as-is

Deferred. It may become useful, but today it is not clearly the single reporting seam. The decision should be made after execution context and report inputs are explicit.
