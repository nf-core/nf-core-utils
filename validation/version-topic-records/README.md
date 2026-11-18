# collectVersions() and citationsOnTheFly() with versions records

Typed processes (`nextflow.enable.types = true`) emit their versions to the `versions` topic as
`record(process: ..., tool: ..., version: ...)`. This test feeds the topic from a typed process,
an untyped process emitting `[process, tool, version]` tuples, and a legacy `versions.yml`, and
checks that `collectVersions()` reports all three the same way. `citationsOnTheFly()` must cite
the tools from both the record and the tuple, with their versions.

Typed processes need Nextflow 26.04 or later, so the test is skipped when `NXF_VER` selects an
older release.

```bash
mise run validate version-topic-records/main.nf.test
```
