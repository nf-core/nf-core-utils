# validation Agent Guide

nf-test workflows that exercise the installed plugin through real Nextflow runs.
This is the end-to-end contract for user-visible behavior.

## Use this layer for

- Public plugin behavior that depends on workflow/session state.
- Validation workflow stdout/stderr and selected output snapshots.

## Snapshot rules

Snapshots are review artifacts, not disposable generated files.

- Do not delete `*.nf.test.snap` / `*.snap` to make tests pass.
- Regenerate only for expected behavior changes.
- Review snapshot diffs before committing.
- The prek hook blocks staged snapshot deletions. Intentional deletion requires:

```bash
ALLOW_VALIDATION_SNAPSHOT_DELETE=1 git commit ...
```

## Stabilize before snapshotting

Normalize nondeterministic values in `.nf.test` files, not `.snap` files:

- Use `filterNextflowOutput(...)` for Nextflow versions, run names, paths, and
  hashes.
- Replace Seqera watch/trace IDs with placeholders.
- Snapshot only stable output files/content.

## Checks

```bash
make validate
make update-snapshots   # only after expected behavior changes
make validate           # confirm regenerated snapshots are stable
prek -c prek.toml run validation-nf-test-snapshot-guard
```
