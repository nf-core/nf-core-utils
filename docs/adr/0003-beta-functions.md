# ADR-0003: Ship beta functions outside the SemVer promise

## Status

Proposed (2026-11-23)

## Context

1.0 locks the public API: after it, a breaking change to an exported function needs
a major release. Some new functions are useful now but their shape is not settled.
`genomeExistsError()`, and `getReferences()` and `getReferenceNames()` in
[#148](https://github.com/nf-core/nf-core-utils/pull/148), are such functions.

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

`genomeExistsError()` is the first beta function. It is not in 0.7.0, and
[#148](https://github.com/nf-core/nf-core-utils/pull/148) changes its arguments, so
its API is not settled. It carries all three marks:

- **Runtime.** `NfUtilsExtension.genomeExistsError()` calls `log.warn1` before it
  checks the genome:

  ```groovy title="src/main/groovy/nfcore/plugin/NfUtilsExtension.groovy"
  @Function
  void genomeExistsError(Map args) {
      log.warn1("genomeExistsError() is beta: its arguments and output may change in a minor release")
      ReferenceDatasheet.genomeExistsError(args)
  }
  ```

- **Docs.** Section 1.3 of `docs/ReferencesExtension.md` opens with the beta
  admonition.
- **Changelog.** Its `Added` entry starts with `Beta:`.

A pipeline that calls it logs the warning once per run:

```console title="Output"
WARN: genomeExistsError() is beta: its arguments and output may change in a minor release
```

`NfUtilsExtensionBetaTest` checks that two calls log the warning exactly once.

When its arguments settle, a minor release promotes it: delete the `log.warn1`
line, the admonition and `NfUtilsExtensionBetaTest`, and add a `Changed` entry:

```markdown title="CHANGELOG.md"
- `genomeExistsError()` is stable and covered by Semantic Versioning.
```

Pipelines that call it keep working unchanged and stop seeing the warning.

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
