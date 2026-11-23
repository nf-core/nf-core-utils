# ADR-0003: Ship beta functions outside the SemVer promise

## Status

Proposed (2026-11-23)

## Context

1.0 locks the public API: after it, a breaking change to an exported function needs
a major release. Some new functions are useful now but their shape is not settled.
`getReferences()` and `getReferenceNames()` in
[#148](https://github.com/nf-core/nf-core-utils/pull/148) are the first case.

Holding them back until 1.1 delays pipelines that would use them today. Shipping
them as stable freezes arguments nobody has tried in a real pipeline yet.

Nextflow solves the same problem for its own language: preview features ship in
stable releases and say they may change.

## Decision

A function may ship as **beta**. A beta function is not covered by Semantic
Versioning. Its name, arguments and output may change, or it may be removed, in any
minor release.

A beta function carries three marks:

1. **Docs.** Its section opens with a beta admonition:

   ```markdown
   !!! warning "Beta"

       This function is beta. Its arguments and output may change in a minor
       release. See [ADR-0003](adr/0003-beta-functions.md).
   ```

2. **Changelog.** Its `Added` entry starts with `Beta:`.
3. **Runtime.** Its first call in a run logs one warning through `log.warn1`:

   ```text
   getReferences() is beta: its arguments and output may change in a minor release
   ```

A beta function becomes stable through a changelog entry under `Changed` that says
so, and by removing the three marks. Pipelines change nothing when it does.

Only new functions may ship as beta. A function that is already stable cannot move
back to beta: changing it is a breaking change.

## Example

`getReferences()` from [#148](https://github.com/nf-core/nf-core-utils/pull/148)
ships as beta. The extension method adds one line:

```groovy title="NfUtilsExtension.groovy" hl_lines="3"
@Function
Map getReferences(Map args) {
    log.warn1("getReferences() is beta: its arguments and output may change in a minor release")
    return ReferenceDatasheet.getReferences(args)
}
```

Its section in `docs/ReferencesExtension.md` opens with the admonition:

```markdown title="docs/ReferencesExtension.md"
### `getReferences()`

!!! warning "Beta"

    This function is beta. Its arguments and output may change in a minor
    release. See [ADR-0003](adr/0003-beta-functions.md).
```

Its changelog entry starts with `Beta:`:

```markdown title="CHANGELOG.md"
### Added

- Beta: `getReferences(schema:, rows:, params:, genome:, genomes:, ignore:)` resolves
  reference params from `params`, datasheet rows or the genome config.
```

A pipeline that calls it sees one warning per run, however many times it calls it:

```console title="Output"
WARN: getReferences() is beta: its arguments and output may change in a minor release
```

When the arguments settle, a later minor release promotes it. That release
deletes the `log.warn1` line and the admonition, and adds:

```markdown title="CHANGELOG.md"
### Changed

- `getReferences()` is stable and covered by Semantic Versioning.
```

Pipelines that call `getReferences()` keep working unchanged and stop seeing the
warning.

## Rationale

- Pipelines get the function now, and the warning tells them what they accepted.
- `log.warn1` already exists in the plugin, so the runtime mark costs one line per
  function and no new mechanism.
- Promotion needs no rename, so no pipeline breaks when a function becomes stable.

## Consequences

- A minor release may break a pipeline that calls a beta function. The changelog
  entry for that release must say so.
- Each beta function warns once per run until it is promoted. That is the intended
  pressure to promote or drop it.
- Reviewers check that new functions are either stable on purpose or carry all three
  marks.

## Alternatives considered

- **Opt-in flag, like `nextflow.preview.*`.** Rejected for now: the plugin has no
  config scope, and building one to guard a few functions costs more than it saves.
  Every pipeline would also have to set the flag.
- **Beta names, such as `getReferencesBeta()`.** Rejected: promotion would rename
  the function and break every caller.
- **Hold unsettled functions for a later minor release.** Still available for any
  function, but it keeps useful code from pipelines.
