# Software versions

`collectVersions()` turns everything a pipeline emitted on its `versions` topic into one `versions.yml`: records from typed processes, `[process, tool, version]` tuples from untyped processes, and legacy `versions.yml` files, plus the workflow and Nextflow versions.

## Sub-features

- `versions-records` includes tools emitted as `record(process, tool, version)`.
- `versions-tuples` includes tools emitted as `tuple val(task.process), val(tool), eval(...)`.
- `versions-legacy` includes tools from a process's `versions.yml`.
- `versions-topic-channels` covers the older `processVersionsFromTopic`/`processVersionsFromFile`/`softwareVersionsToYAML` helpers.

## How to get to it (user POV)

- Collect `channel.topic('versions')` in a pipeline and pass it to `collectVersions`.
- Use the older per-source helpers in a pipeline that has not migrated.

## Driving it with verify

Preconditions:

- Baseline preconditions from the index hold.
- Nextflow is 26.04 or later (typed processes); `doctor` prints the version.

- **Mixed sources.** Run `verify drive versions-records validation/version-topic-records --outdir results`. `exit-code.txt` is `0`. `results/versions.yml` lists `preseq: 3.2.0`, `fastqc: 0.12.1` and `legacy: '2.0'` under their process names, plus a `Workflow` block with `nf-core-utils-validation: v1.0.0`. `results/citations.txt` lists the tools that have a `meta/*.yml`.
- **Older helpers.** Run `verify drive versions-topic-channels validation/version-topic-channels --outdir results`. `exit-code.txt` is `0` and `results/` contains `topic_versions.yml`, `single_versions.yml`, `deprecated_versions.yml`, `empty_versions.yml`, `file_versions.yml` and `mixed_versions.yml`.

## Gotchas

- Without `--outdir results`, files go to the config's default; check the journey's `nextflow.config` before trusting an empty `results/`.
- `subscribe` writes the files asynchronously. A run with exit `0` but no `versions.yml` is a failure, not a timing issue to retry.
- Compare against `validation/version-topic-records/main.nf.test.snap` for the expected content; the Nextflow version line differs per toolchain.
