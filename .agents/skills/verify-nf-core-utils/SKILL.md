---
name: verify-nf-core-utils
description: Builds this checkout's nf-core-utils Nextflow plugin into an isolated plugins dir and proves its user-facing functions by running real `nextflow run` pipelines that `include { ... } from 'plugin/nf-core-utils'`, capturing stdout, stderr, exit code, .nextflow.log and output files. Use to verify a plugin change end to end ("does it work in a pipeline", "prove it", smoke test, before a release), beyond unit tests and nf-test snapshots.
---

# Verify nf-core-utils

The plugin is a library. A user touches it by adding `include { fn } from 'plugin/nf-core-utils'` to a pipeline and running `nextflow run`. Verification means doing exactly that against the plugin built from this checkout, never a registry copy or another worktree's install.

Everything goes through one helper, run from the repository root:

```bash
.agents/skills/verify-nf-core-utils/scripts/verify launch | doctor | drive <label> <pipeline> [args...] | cleanup
```

The helper loads the repo's locked toolchain with `mise env`, which also unsets `AGENT` and `CLAUDECODE`. Those variables turn on Nextflow agent mode and change output. Do not call bare `nextflow` from an agent shell to verify; go through `drive`.

## Launch

Prerequisite: `mise` is on `PATH`, and `mise trust mise.toml && mise install` has run once in this checkout. A new checkout or worktree is untrusted, so the helper exits with `mise env failed` until then. Trust only this file; never `mise trust --all`. On a cold machine, the install takes about 10 s and the first `launch` about 40 s while Gradle downloads its dependencies.

```bash
.agents/skills/verify-nf-core-utils/scripts/verify launch
```

This runs `./gradlew install` with `NXF_PLUGINS_DIR` pointing at a fresh `$VERIFY_RUN/state/plugins`. It prints `VERIFY_RUN=<path>` and records that path in `.nextflow-verify-run` at the repo root (gitignored), so later `doctor`, `drive` and `cleanup` calls find the run without any shell state. Setting `VERIFY_RUN` explicitly overrides the pointer. Ready means `Plugin nf-core-utils installed successfully!` appeared on stderr and `VERIFY_RUN=` was printed. There is no server; each `drive` is a separate short-lived Nextflow run.

Isolation: every run has its own plugins dir, `NXF_HOME` and launch dirs, and `~/.nextflow/plugins` and `~/.nextflow/config` are never read or written. Two checkouts can verify side by side. Within one checkout, `build/` and the pointer file are shared: do not run two launches from one checkout at the same time.

The version comes from `build.gradle` (`version = '...'`). `drive` passes `-plugins nf-core-utils@<version>` for you. Ignore the `@0.4.0` examples in `validation/README.md`; they are stale.

## Doctor

```bash
.agents/skills/verify-nf-core-utils/scripts/verify doctor
```

Read-only. It prints `DOCTOR OK` or `DOCTOR FAILED` with a reason and exits non-zero on failure. It fails when:

- No launched run is found (neither `VERIFY_RUN` nor `.nextflow-verify-run`).
- The installed plugin dir is missing.
- The installed classes differ from `build/classes/groovy/main` (another build overwrote `build/`, or the install is stale).
- `src/` or `build.gradle` changed since launch, including new untracked files (compares `HEAD` plus a hash of the diff and status).

On any failure, run `cleanup` and `launch` again. Run doctor first whenever a result looks wrong.

## Drive

```bash
.agents/skills/verify-nf-core-utils/scripts/verify drive <label> <pipeline .nf or dir> [nextflow args...]
```

`drive` creates `$VERIFY_RUN/state/launch/<label>`, runs `nextflow run <pipeline> -plugins nf-core-utils@<version> [args]` from there, and copies the evidence out. The ready-made journeys are the directories under `validation/`; each is a self-contained pipeline with its own `nextflow.config`. Pass `--outdir results` when a journey writes files, so they land in the launch dir and get copied. `drive` prints `exit=<code> evidence=<dir>`; its own exit status is 0 even when Nextflow fails, so read `exit-code.txt`.

For a behavior no journey covers, write a scratch pipeline outside the repo (for example under `$VERIFY_RUN/state/`) with an `include { ... } from 'plugin/nf-core-utils'` line and a `nextflow.config` containing `plugins { id 'nf-core-utils' }`, then `drive` it.

The feature map lists the journeys: [features/README.md](features/README.md).

## Evidence

`$VERIFY_RUN/evidence/<label>/` holds, per drive:

| File                | Content                                                                                                                                                  |
| ------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `command.txt`       | The exact `nextflow run` command                                                                                                                         |
| `stdout.txt`        | Only `println` output                                                                                                                                    |
| `stderr.txt`        | Nextflow's console log: every `log.info`/`log.warn` line, completion summaries, and `ERROR ~` blocks. Assert on this file for most messages.             |
| `exit-code.txt`     | Nextflow's exit code                                                                                                                                     |
| `nextflow.log`      | The full run log                                                                                                                                         |
| `plugin-loaded.txt` | Two log lines: `plugins-dir=<this run's dir>` and `Plugin started nf-core-utils@<version>`. A warning on stderr means the proof is not about this build. |
| `results/`          | Copied when the run wrote `./results`                                                                                                                    |

`$VERIFY_RUN` lives under `${VERIFY_ROOT:-$TMPDIR}/verify-nf-core-utils/`, outside the repo, so `mise run clean` cannot delete it. Set `VERIFY_ROOT` before `launch` to keep evidence somewhere durable.

Proof standards:

- Drive the real user path: a pipeline that includes the function from `plugin/nf-core-utils`. Unit tests and calling Groovy classes directly do not count.
- Capture the action and the resulting state: the command, the console output, and the files the function wrote.
- Check side effects alongside the console: files under `results/`, and the work dir for functions that write there (copy those out before `cleanup`).
- A failing path is proven by a non-zero `exit-code.txt` and the expected message, plus the absence of the success marker.
- Do not trust a journey's own `✅ ... tested successfully` lines. Some only log, and at least one guards its asserts behind a file check that never passes. Assert on the function's output yourself.
- Email and IM notifications are only safe on their disabled paths (no `--email`, no `--hook_url`). Do not point them at a real address or webhook without the user's say-so; the disabled path proves nothing about delivery.

## Cleanup

```bash
.agents/skills/verify-nf-core-utils/scripts/verify cleanup
```

Removes `$VERIFY_RUN/state` (plugins, `NXF_HOME`, launch and work dirs) and keeps `$VERIFY_RUN/evidence`. No process outlives a `drive`, so nothing is killed. The Gradle daemon started by `launch` is shared with normal development; leave it. Run cleanup after every failed attempt as well. Then run `git status` to confirm nothing landed in the checkout besides `build/`.

## Helpers

- `scripts/verify` is the only helper. Its usage line prints when it is called without arguments.
- The repo also ships `validation/config-validation/startup-checks.sh` (15 CLI startup-check cases). It honors `NXF_PLUGINS_DIR`; see [features/startup-checks.md](features/startup-checks.md).
