---
name: verify
description: Build the nf-core-utils plugin and run it in a real Nextflow pipeline to observe a change. Use when asked to verify a change to the plugin at runtime.
---

# Verify nf-core-utils at runtime

The surface is a Nextflow pipeline that includes a function from `plugin/nf-core-utils`.
Do not use nf-test or Gradle tests as evidence; CI runs those.

## 1. Build and install into an isolated plugins directory

Run this from the checkout that holds the change:

```bash
NXF_PLUGINS_DIR=<scratch>/plugins-pr mise exec -- ./gradlew install -q
```

The install task prints the location and says it came from `NXF_PLUGINS_DIR`. This keeps
`~/.nextflow/plugins` untouched.

## 2. Run a scratch pipeline

Create `<scratch>/p/main.nf` and `<scratch>/p/nextflow.config`. Pin the version from
`build.gradle`:

```groovy
plugins {
    id 'nf-core-utils@0.7.0'
}
```

Run it from the pipeline directory:

```bash
env -u AGENT -u CLAUDECODE NXF_PLUGINS_DIR=<scratch>/plugins-pr NXF_OFFLINE=true NXF_ANSI_LOG=false \
    nextflow -C nextflow.config run main.nf
```

- `-C` uses only this config file, so the user's global Nextflow config is not loaded.
- `AGENT` and `CLAUDECODE` turn on Nextflow agent mode, which changes the output.
- Outside the repo, `mise exec` has no tools: use `mise which nextflow` and
  `mise where java` (as `JAVA_HOME`) from the checkout.

## 3. Baseline

A branch usually has the same version as the last published plugin. For a "before" run,
point `NXF_PLUGINS_DIR` at a second, empty directory and set `NXF_OFFLINE=false`:
Nextflow downloads the published version from the registry.

## Flows worth driving

- `completionEmail(summary, null, null, false, './results', true, [])` inside
  `workflow.onComplete`: with no recipient nothing is sent, and the rendered e-mail is
  written to `results/pipeline_info/pipeline_report.{txt,html}`.
- A failed run: add a process that exits non-zero behind a `params.fail` switch.
- Pipeline assets: files under `<pipeline>/assets/` are read relative to `projectDir`.
