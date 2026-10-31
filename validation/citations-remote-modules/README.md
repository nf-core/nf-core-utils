# Registry-module citation reproduction

**Outcome: partial success, with a reproducible missing MultiQC citation.**
Tested on 2026-10-30 against PR #51's remote head
[`bc5e40b`](https://github.com/nf-core/nf-core-utils/commit/bc5e40bfd9ffc85220ac4f082c729ee3fa1db72f),
not main. Fetching before testing and checking the remote again afterwards found
no newer head. The checkout was detached; no PR branch, plugin implementation,
or installed module was modified. Nothing was committed or pushed.

This is an opt-in manual integration case, not part of the default nf-test
suite. It downloads registry modules and creates real Conda environments.
`check.sh` runs the Spock `RegistryModuleCitationsTest` against the generated
artifacts. Only the missing MultiQC citation and bibliography entry are marked
`@PendingFeature`, tracked in [#89](https://github.com/nf-core/nf-core-utils/issues/89).
FastQC and SAMtools assertions remain active. The check exits 0 with four
passed and two skipped tests; the pending tests **execute and fail**, so this
does **not** mean MultiQC is cited correctly. Each is a separate feature: if
either starts passing, Spock fails the build until its annotation is removed.
Missing artifacts fail in setup, outside the pending-feature handling.
No citation fix or accepting snapshot is included.

Without `NF_CORE_REMOTE_CITATIONS_OUTDIR`, this spec is skipped by `@Requires`
in the ordinary unit suite. `check.sh` sets that environment variable to the
supplied absolute output path and forces Gradle to rerun the checks. No Spock
dependency upgrade or custom expected-failure implementation is needed.

## Sources and runtime

Starting references:

- <https://docs.seqera.io/nextflow/reference/cli/module>
- <https://docs.seqera.io/nextflow/modules/using-modules>

Runtime: Nextflow **26.10.2 build 14160**, Java Temurin **21.0.12.1+1**, Conda
**26.9.4**, Linux x86_64. Plugin **nf-core-utils@0.7.0-dev**, built and installed
locally from the PR head above. This is a supported stable runtime, not an edge
release. No containers, stubs, mocked versions, or hand-written replacement
modules were used.

| Registry module         | Pinned registry version | Tool executed |
| ----------------------- | ----------------------- | ------------- |
| `nf-core/fastqc`        | `0.0.0-6d1763b`         | FastQC 0.13.0 |
| `nf-core/multiqc`       | `0.0.0-6d1763b`         | MultiQC 1.35  |
| `nf-core/samtools/view` | `0.0.0-df69bf6`         | SAMtools 1.25 |

Registry: `https://registry.nextflow.io/api`. The version suffixes resolve to
nf-core/modules commits
[`6d1763b`](https://github.com/nf-core/modules/commit/6d1763b6d25bc0fd7c8f8c8bab81e4be7c733441)
and [`df69bf6`](https://github.com/nf-core/modules/commit/df69bf63a03a506f513f76b18d04ad4985f002eb).
The evidence archive includes the exact downloaded modules, `.module-info`
files, and SHA-256 hashes of each `main.nf` and `meta.yml`. `nextflow module list`
reported all three modules as `OK` both before and after execution.

## Reproduce

Use a checkout of the tested PR commit, with this fixture added. From the
repository root:

```bash
mise install --locked
mise run install
mkdir -p .amp/in/artifacts/remote-citations
```

Run these commands from `validation/citations-remote-modules/`:

```bash
mise exec -- nextflow module install nf-core/fastqc -version 0.0.0-6d1763b
mise exec -- nextflow module install nf-core/multiqc -version 0.0.0-6d1763b
mise exec -- nextflow module install nf-core/samtools/view -version 0.0.0-df69bf6
mise exec -- nextflow module list

mise exec -- nextflow \
  -log ../../.amp/in/artifacts/remote-citations/nextflow-with-multiqc.log \
  run main.nf -ansi-log false \
  -with-trace ../../.amp/in/artifacts/remote-citations/trace-with-multiqc.tsv \
  --outdir ../../.amp/in/artifacts/remote-citations/with-multiqc

bash check.sh ../../.amp/in/artifacts/remote-citations/with-multiqc
# Spock: 4 passed, 2 skipped (@PendingFeature, #89), exit 0.
# MultiQC remains missing from both files.

mise exec -- nextflow \
  -log ../../.amp/in/artifacts/remote-citations/nextflow-without-multiqc.log \
  run main.nf -ansi-log false -params-file without-multiqc.json \
  -with-trace ../../.amp/in/artifacts/remote-citations/trace-without-multiqc.tsv \
  --outdir ../../.amp/in/artifacts/remote-citations/without-multiqc
```

Both Nextflow runs exit 0. The first executes all three processes. The second
executes only FastQC and SAMtools, while MultiQC remains installed and its
metadata is still discovered. There is no `-resume` or `-stub-run` in either run.

## Metadata discovery and observed results

`nextflow module install` writes real files into `modules/nf-core/<name>/`.
`meta.yml` includes module version and `tools:` metadata; `.module-info` includes
the registry URL and integrity checksum. The workflow uses registry-name
includes, for example `from 'nf-core/fastqc'`. The debug log records
`DefaultRemoteModuleResolver` resolving each name to its installed `main.nf`.
This exercises registry resolution, not merely remote-looking helper arguments.

The existing `files("${projectDir}/modules/**/meta.yml")` pattern finds all three
metadata files, including the deeper `samtools/view` directory. Their real
paths are passed to `citationsOnTheFly()` with collected `versions` topic tuples.
The plugin does not itself scan the registry or infer execution from the
installed module inventory.

| Executed tool | Expected citation/bibliography reference                     | Observed in both HTML files        |
| ------------- | ------------------------------------------------------------ | ---------------------------------- |
| FastQC 0.13.0 | `https://www.bioinformatics.babraham.ac.uk/projects/fastqc/` | Present, homepage fallback         |
| SAMtools 1.25 | `https://doi.org/10.1093/bioinformatics/btp352`              | Present, DOI link                  |
| MultiQC 1.35  | `https://multiqc.info/`                                      | **Pending feature (#89): missing** |

All three trace rows are `COMPLETED`, exit `0`. Actual tool results were also
checked: FastQC read 2 sequences / 80 bases; SAMtools produced a BAM containing
1 alignment; MultiQC's FastQC summary contains the same 2 sequences / 80 bases.

The collected topic contains only:

```text
FASTQC         fastqc     0.13.0
SAMTOOLS_VIEW  samtools   1.25
```

The separately observed MultiQC output is
`NON_TOPIC_VERSION: [MULTIQC, multiqc, 1.35]`. The upstream module deliberately
uses `emit: versions` **without** `topic: versions`; its comment explains that
publishing to the topic can deadlock pipelines where MultiQC consumes versions.
The local-module example does publish MultiQC to that topic, so it does not
exercise this distinction.

Thus the failure is **incomplete execution input to the topic-driven citation
flow**, not unavailable installed metadata. No warning is produced for MultiQC:
it is absent from the tools selected for citation. No topic forwarding,
`extraTools` workaround, metadata enrichment, or plugin fix was added.

In the control run, MultiQC correctly has no trace row and no citation despite
its discovered metadata. Both runs produce the same FastQC/SAMtools citation
and bibliography content. The installed metadata has no `publication` author
or year for these tools, so this case does **not** independently test Harvard
author parsing. MultiQC's installed metadata has no DOI; a paper reference
cannot be expected without additional metadata.

## Evidence and other checks

Original-run evidence is retained in the test orb, not checked into this
repository: run/debug logs, traces, module provenance, task scripts/logs/exit
codes, observed tool outputs, HTML citations/bibliographies, collected versions,
discovered paths, and the original check's two failures. The original
`remote-citations-evidence.tar.gz` archive contains that evidence and the
fixture before expected-failure marking. Reproduction commands above write
fresh evidence under `.amp/in/artifacts/remote-citations/`.

Two setup attempts are retained separately, not counted as successful controls:
`-b` was insufficient for the upstream SAMtools module's filename/index logic;
the fixture now uses the supported `--output-fmt bam`. Also, CLI
`--run_multiqc false` still ran MultiQC in this runtime; the verified negative
control uses a JSON boolean via `-params-file` instead.

Additional checks on the unchanged PR code:

- `mise run test`: 313 total, 309 passed, 0 failed, 4 skipped.
- From `validation/`, `mise exec -- nf-test test citations-on-the-fly/main.nf.test --verbose`:
  1 test passed, existing snapshot unchanged.
- `bash -n check.sh`: passed.

Scope is limited to these pinned process modules on Nextflow 26.10.2; this does
not establish support for every registry module, nested workflow module, or
older supported runtime.
