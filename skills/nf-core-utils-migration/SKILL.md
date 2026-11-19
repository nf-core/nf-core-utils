---
name: nf-core-utils-migration
description: Migrate an nf-core pipeline from the vendored utils subworkflows (utils_nextflow_pipeline, utils_nfcore_pipeline, utils_nfschema_plugin) and local helper copies to the nf-core-utils Nextflow plugin, or bump nf-core-utils / nf-schema to a newer version. Use this whenever someone wants to "use nf-core-utils", drop or replace vendored nf-core utils subworkflows, move helpers like getGenomeAttribute, softwareVersionsToYAML, paramsSummaryMultiqc, completionEmail or methodsDescriptionText to `plugin/nf-core-utils`, update the nf-core-utils or nf-schema pin, or build MultiQC tool citations from module meta.yml with citationsOnTheFly (publication blocks), or debug a run failing with "Missing process or function X ... Did you mean 'X'" after a plugin change (including AWS megatests / Seqera Platform runs).
---

# nf-core-utils migration

Move an nf-core pipeline off vendored utility code and onto the `nf-core-utils` plugin (plus direct `nf-schema` calls), one plugin version at a time, without breaking runs that launch the pipeline with extra configs.

Tool citations built from module `meta.yml` (`citationsOnTheFly`, `publication` blocks) are covered in the Citations section below, as a separate PR after the migration. nf-test infrastructure (`tests/lib`, snapshot helpers, nft-utils) is out of scope: mention it as a follow-up if you see it, do not fold it into the migration PR.

Reference migrations worth reading before starting: nf-core/rnavar#233 (first removal of vendored subworkflows), nf-core/rnavar#252 (genome and versions helpers), nf-core/seqinspector#241 and nf-core/rnaseq#1977 (full replacement). Fetch them with `gh pr diff <n> -R nf-core/<repo>`.

## 1. Inventory what is already migrated

Migration happens in stages: each plugin release absorbs more helpers, so a pipeline that "already uses nf-core-utils" often still carries local copies that shadow plugin functions. Do not assume the user's description of the starting state is current. Check:

```bash
ls subworkflows/nf-core/ | grep '^utils_'                      # vendored subworkflows still present
grep -n '"utils_' modules.json                                  # their modules.json entries
grep -rn "from 'plugin/" main.nf workflows subworkflows/local   # what is already imported from plugins
grep -n "^def " main.nf workflows/*.nf subworkflows/local/utils_*/main.nf   # local helper defs
grep -n "id '" nextflow.config                                  # current plugin pins
```

Compare every local `def` against the functions the target plugin version exports (step 2). A local def with the same name as a plugin export is a candidate for deletion. Keep defs that hold pipeline-specific logic (custom validation, tool lists, pipeline-specific summary text).

## 2. Read signatures from the plugin version you will pin

Plugin APIs change between versions, and other pipelines' diffs may target a different version. Read the real signatures from the cached plugin rather than from memory:

```bash
nextflow plugin install nf-core-utils@<ver>      # or run the pipeline once; plugins land in ~/.nextflow/plugins/
P=~/.nextflow/plugins/nf-core-utils-<ver>
grep Plugin-Requires $P/classes/META-INF/MANIFEST.MF            # minimum Nextflow version
python3 -c "import json;[print(x['spec']['name'],[p['type'] for p in x['spec']['parameters']]) for x in json.load(open('$P/classes/META-INF/spec.json'))['definitions'] if x['type']=='Function']"
javap -cp $P/classes nfcore.plugin.NfUtilsExtension | grep <function>
javap -cp ~/.nextflow/plugins/nf-schema-<ver>/classes nextflow.validation.ValidationExtension | grep <function>
```

If `Plugin-Requires` is above `manifest.nextflowVersion`, bump `manifest.nextflowVersion`, the minimum `NXF_VER` in `.github/workflows/nf-test.yml`, and the README badge in the same PR.

When you keep an older pin on purpose (for example nf-schema 2.x because 3.x drops `paramsSummaryLog(Map, WorkflowMetadata)`), list the call sites that would break on the next major in the PR description so the follow-up is visible.

## 3. Replace the helpers

Use `references/function-map.md` for the call-by-call replacement of each vendored subworkflow and local helper. Pin the plugin in `nextflow.config`:

```groovy
plugins {
    id 'nf-core-utils@<ver>' // nf-core-utils is a collection of utilities for Nextflow pipelines
    id 'nf-schema@<ver>'     // Validation of pipeline parameters and creation of an input channel from a sample sheet
}
```

Then delete `subworkflows/nf-core/utils_nextflow_pipeline`, `utils_nfcore_pipeline` and `utils_nfschema_plugin` (including their `tests/`) and their `modules.json` entries. Prefer `nf-core subworkflows remove <name>` so `modules.json` stays consistent; if you must edit `modules.json` by hand, run `npx -y prettier@3 --write modules.json` afterwards, because a JSON dump explodes the `installed_by` arrays.

## 4. Pin plugins in every config that is not nextflow.config

A `plugins {}` block in a launch-time config (the Seqera Platform `nextflow_config` in `.github/workflows/awsfulltest.yml` / `awstest.yml`, Tower configs, institutional `-c` configs) replaces the pipeline's own `plugins {}` block instead of merging with it. Any plugin the pipeline imports with `include { ... } from 'plugin/<name>'` but that is not pinned in that block then resolves to the latest release, which breaks as soon as a new major ships.

Repeat exactly the versions from `nextflow.config` in every such block, plus whatever plugin the block adds for itself:

```yaml
nextflow_config: |
  plugins {
    id 'nf-core-utils@<same as nextflow.config>'
    id 'nf-schema@<same as nextflow.config>'
    id 'nf-slack@0.5.0'
  }
```

Check that the pins agree:

```bash
grep -rhoE "id '(nf-core-utils|nf-schema)@[^']+'" nextflow.config .github/workflows/ | sort | uniq -c
```

Every plugin should appear with a single version.

## 5. Diagnose plugin failures from evidence

`Missing process or function X(...) -- Did you mean 'X' instead?` means a function called `X` exists but no overload matches the arguments. Read it as a plugin version mismatch, not a typo: find which version actually loaded, then compare its signatures (step 2) with the call site.

Confirm which version loaded before changing anything:

- Megatest / Platform run: `gh run list -R nf-core/<pipeline> -w "nf-core AWS full size tests"` and `gh run view <id> --log`, or the Slack message posted by nf-slack, or the Platform run log. Look for `Downloading plugin <name>@<ver>` / `Starting plugin <name>@<ver>`.
- Local reproduction of a config override: run with a config that only holds the external `plugins {}` block and check the log.

```bash
printf "plugins {\n  id 'nf-slack@0.5.0'\n}\n" > /tmp/override.config
nextflow run . -profile test,docker --outdir /tmp/out -c /tmp/override.config 2>&1 | head -40
grep -E "plugin (nf-schema|nf-core-utils)@" .nextflow.log
```

Ask the user for logs only after these sources come up empty.

## 6. Verify

Separate failures you caused from failures already on `dev` before reporting either: rerun the failing test or lint on a clean worktree.

```bash
git worktree add /tmp/dev-baseline origin/dev
(cd /tmp/dev-baseline && <same test or lint command>)
git worktree remove --force /tmp/dev-baseline
```

Run at least:

- `nextflow lint main.nf workflows subworkflows/local` (strict syntax)
- `nf-core pipelines lint`
- the pipeline nf-tests, with `NXF_VER` set to the pipeline's minimum Nextflow version
- `nextflow run . --help`, `--help_full`, and `--version`, since these paths exit early and are easy to break when inlining `UTILS_NFSCHEMA_PLUGIN` / `UTILS_NEXTFLOW_PIPELINE`

## Citations (separate PR)

Once the helpers come from the plugin, the MultiQC methods description can cite the tools that actually ran, using `citationsOnTheFly` (nf-core-utils 0.7.0 or later) and the `tools:` entries of each module `meta.yml`. Keep this out of the migration PR: it touches many module files, changes snapshots pipeline-wide, and involves attribution decisions the user should make.

Read `references/citations.md` before starting. In short:

1. Wire `channel.topic("versions").collect(flat: false)` into `citationsOnTheFly`, passing tools that run after the collect (MultiQC) as extras.
2. Predict which tools will log `No citation metadata found` and break snapshots; cite real tools, ignore generic utilities one exact entry at a time.
3. Agree with the user where citation metadata lives: upstream nf-core/modules, module patches, or a local overlay file.
4. Take every DOI, author list and title from Crossref / PubMed, never from memory, and ask before replacing a module's cited paper.
5. Keep module patches limited to `meta.yml`, and read the rendered `Tools used` text from a test run.

## Gotchas

- Snapshots: `softwareVersionsToYAML` from the plugin keeps tuple versions as quoted strings, so snapshots that held numeric versions (`1.21` instead of `"1.21"`) change across many files. That is expected; check that only quoting changed.
- Collect versions so tuples and empty streams survive: rnaseq#1977 collects with `.toList()` and reads legacy `versions.yml` emits via `entry instanceof Path ? entry.text : entry` so remote work directories keep working.
- Keep `validateParameters(cast_cli_params: false)` if the vendored `UTILS_NFSCHEMA_PLUGIN` call relied on it; check the old call before inlining.
- `getWorkflowVersion()` reads the manifest; pass `workflow.manifest.version, workflow.commitId` if the pipeline printed the git suffix before.
- `methodsDescriptionText` from the plugin takes a `String` path, so pass `file.toString()`, not the `Path`.
- nf-core YAML files sometimes quote keys (`- "gatk4":`). Bulk edits with regexes must match quoted and unquoted keys, then count the edited files against the expected list.
- If a module must be patched, `nf-core modules patch` diffs gitignored files under `modules/nf-core/<m>/tests/` too, so reset them to the pinned SHA first (`git -C ~/.config/nfcore/nf-core/modules show <git_sha>:modules/nf-core/<m>/<file>`) and check that each `.diff` only names the files you meant to change.

## PR

- Open the PR first, then write the CHANGELOG entries with the real PR number. Guessing the next number races other contributors.
- CHANGELOG: an entry under `Changed`, a `Fixed` entry if the PR fixes a plugin pin, and a `Dependencies - Nextflow plugins` table with old and new versions.
- PR description: plugin versions before/after, every external `plugins {}` block you pinned, which helpers moved to the plugin and which stayed local (and why), follow-ups (next-major call sites, citations, test infrastructure).
