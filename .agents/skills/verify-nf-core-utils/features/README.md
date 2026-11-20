# nf-core-utils verification map

This directory is the maintained source for verifying what pipeline authors get from nf-core-utils. Read this index, then use the matching feature file as the recipe.

## Baseline preconditions

- Run from the repository root.
- `.agents/skills/verify-nf-core-utils/scripts/verify launch` succeeded in this checkout.
- `.agents/skills/verify-nf-core-utils/scripts/verify doctor` prints `DOCTOR OK`.
- The genome journeys pass with `NXF_OFFLINE=true`. The others have not been run offline; prefix a drive with `NXF_OFFLINE=true` to check before relying on that.

## Driving conventions

- `verify` below means `.agents/skills/verify-nf-core-utils/scripts/verify`.
- Every journey is `verify drive <label> <pipeline> [nextflow args]`. Use a distinct label per journey; reusing a label overwrites its evidence.
- Pass `--outdir results` to journeys that write files.
- Never edit files under `validation/` to make a journey pass. Write a scratch pipeline instead.

## Proof and skip reporting

- A proof is the evidence dir: `command.txt`, `stdout.txt`, `stderr.txt`, `exit-code.txt`, `nextflow.log`, `plugin-loaded.txt`, and `results/` when written.
- Most messages are in `stderr.txt` (Nextflow's console log); only `println` output is in `stdout.txt`.
- `plugin-loaded.txt` must contain two lines. Otherwise the run did not use this checkout's build.
- Report the feature ID and journey label with each proof.
- A path the map lists but you did not drive is reported as not verified, not as covered by another path.

## Known gaps

Behavior this map cannot prove yet. Closing one means adding or extending a journey, then deleting its line here. Until then, report these as not verified.

- **Offline runs.** Only the genome journeys have passed with `NXF_OFFLINE=true`. Drive each other journey offline once and record the result in its feature file.
- **Reference getters.** `getGenomeAttribute`, `getReferencePath`, `getReferenceMetadata` and `getReferenceAttribute` have no journey. Add a validation pipeline with a `params.genomes` config and map it in [genome-checks.md](./genome-checks.md).
- **Notification delivery.** `completionEmail` and `imNotification` run only with delivery disabled. Proving delivery needs a local SMTP sink and a local webhook receiver, so no real address or Slack/Teams hook is used. See [completion-utilities.md](./completion-utilities.md).
- **`dumpParametersToJSON` journey.** `validation/pipeline-utilities` checks a `params.json` that is never written, so its content asserts never run. Point it at `pipeline_info/params_*.json` and update its snapshot.

## Feature entry contract

Each feature file has an H1, one paragraph of user-visible behavior, then four H2s in order: `Sub-features`, `How to get to it (user POV)`, `Driving it with verify`, `Gotchas`.

## Features

- [Genome checks](./genome-checks.md): `genomeExistsError` aborting on an unknown `--genome` and passing on a known one.
- [Startup checks](./startup-checks.md): `checkConfigProvided`, `checkProfileProvided`, `checkCondaChannels` warnings and errors at launch.
- [Software versions](./software-versions.md): `collectVersions` over a `versions` topic of records, tuples and legacy `versions.yml`.
- [Citations on the fly](./citations.md): `citationsOnTheFly` and `citationOutputs` citing only the tools that ran.
- [Completion and pipeline utilities](./completion-utilities.md): `completionSummary`, `completionEmail`, `imNotification`, `getWorkflowVersion`, `dumpParametersToJSON`.
