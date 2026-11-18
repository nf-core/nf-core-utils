# Startup checks

At launch, nf-core pipelines warn when the user supplied no custom configuration or passed a positional argument, abort when `-profile` ends with a trailing comma, and report whether Conda channels are set up. The plugin provides these as `checkConfigProvided()`, `checkProfileProvided(args, monochrome)` and `checkCondaChannels()`.

## Sub-features

- `startup-no-config` warns `You are attempting to run the pipeline without any custom configuration!` when only the pipeline config is loaded.
- `startup-positional` warns `nf-core pipelines do not accept positional arguments.` for an extra positional argument.
- `startup-trailing-comma` aborts with ``The `-profile` option cannot end with a trailing comma``.
- `startup-conda` returns whether `conda.channels` matches the expected order.

## How to get to it (user POV)

- Run any pipeline that calls the checks, with no `-c`/`-profile`, with an extra positional argument, or with `-profile docker,`.
- Call the functions with different argument lists from inside a pipeline.

## Driving it with verify

Preconditions:

- Baseline preconditions from the index hold.

- **CLI matrix.** Run the repo's own 15-case harness against this run's plugins dir: `run=$(cat .nextflow-verify-run); NXF_PLUGINS_DIR=$run/state/plugins mise exec -- bash validation/config-validation/startup-checks.sh > $run/evidence/startup-matrix.txt 2>&1; echo $? >> $run/evidence/startup-matrix.txt`. The file ends with `PASS: 15 startup validation cases` and `0`. Each case line shows `exit=`, `config=`, `positional=` and `ERROR=` counts.
- **In-pipeline calls.** Run `verify drive startup-functions validation/config-validation`. `exit-code.txt` is `0` and `stderr.txt` contains `Configuration check result: false` (only the pipeline config is loaded) and a `Conda channels check result:` line.
- **Trailing comma, real launch.** Run `verify drive startup-comma validation/config-validation -profile docker,`. `exit-code.txt` is `1` and `stderr.txt` contains ``ERROR ~ The `-profile` option cannot end with a trailing comma, please remove it and re-run the pipeline!``.

## Gotchas

- `startup-checks.sh` pins `nf-core-utils@0.7.0` in its generated config. After a version bump in `build.gradle` it fails to find the plugin until that line is updated; report that, do not edit the version to match silently.
- `checkCondaChannels()` reads the machine's own Conda setup, so `true`/`false` differs between machines (the repo's nf-test filters that line out). Only assert the value when the task is about Conda channels, and record the machine's `conda config --show channels` with it.
- nf-test adds its own config files, which hides the no-config warning. That is why the CLI matrix exists; do not prove `startup-no-config` through `mise run validate`.
- The no-config warning also disappears when `~/.nextflow/config` exists. The matrix isolates `NXF_HOME`; a hand-run scratch pipeline must too (`drive` already does).
