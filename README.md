# nf-core-utils plugin

The nf-core-utils plugin provides utility functions used by nf-core pipelines.

## Features

- [NextflowPipelineExtension](docs/NextflowPipelineExtension.md)
- [ReferencesExtension](docs/ReferencesExtension.md)
- [NfCore Utilities](docs/NfCoreUtilities.md)

## Development setup

Install [mise](https://mise.jdx.dev/getting-started.html), then from the repository root:

```bash
mise install --locked
mise tasks ls
```

`mise.toml` defines the development commands and tools: Temurin Java 21 (required
by the Gradle plugin), Nextflow 26.10.2, and nf-test 0.9.3. `mise.lock` records exact
versions and checksums for Linux and macOS on x64 and ARM64. CI uses the same
configuration via `jdx/mise-action`.

The repo is a mise [monorepo root](https://mise.jdx.dev/tasks/workspace-graph.html)
with a single Gradle workspace project (`gradle::` at `.`). There are no
`include` directives in `settings.gradle`, so the project is declared under
`[monorepo.projects]` rather than inferred. Inspect it with
`mise tasks graph` (add `--explain` or `--json` as needed). Run tasks only in
projects touched by a change with `mise run --affected <task>` (for example
`mise run --affected test`).

Make is no longer used: replace `make <target>` with `mise run <target>`.
Available tasks are `assemble`, `test`, `clean`, `install`, `validate`,
`update-snapshots`, `website-docs`, and `release` (also as `//:assemble`, and so on). The Gradle
wrapper still manages Gradle itself.

`mise run website-docs --release <tag> [--output <dir>]` writes the user docs
as nf-core website pages. This file task requires [uv](https://docs.astral.sh/uv/),
which supplies Python and the script's inline dependencies. By default it writes
to `../sites/docs/src/content/docs/nf-core-tools/nf-core-utils`, for use when this
repository is checked out inside the website repository.

Use `mise exec -- <command>` for tools outside a task (for example,
`mise exec -- nextflow -version`), or optionally
[activate mise in your shell](https://mise.jdx.dev/getting-started.html#activate-mise).
Put personal overrides in the ignored `mise.local.toml`. After changing tool
versions, run `mise lock --platform linux-x64,linux-arm64,macos-x64,macos-arm64`
and commit both configuration and lockfile.

`gradle.properties` sets `org.gradle.java.installations.fromEnv=JAVA_HOME` so
Gradle's [toolchain detection](https://mise.jdx.dev/lang/java.html#gradle-toolchains-detection)
offers mise's selected JDK as a candidate. Confirm with
`mise exec -- ./gradlew -q javaToolchains` (run `mise exec -- ./gradlew --stop`
first if a daemon was started before that property was set). When Gradle itself
runs on that JDK, the listing shows `Detected by: Current JVM` for the same
home; `fromEnv` still registers it for toolchain resolution when the daemon JVM
differs.

## Building

To build the plugin:

```bash
mise run assemble
```

Run unit tests with `mise run test` and end-to-end tests with `mise run validate`.
Validation installs the plugin first. Regenerate snapshots only for intended
behavior changes with `mise run update-snapshots`.

### Nextflow compatibility

The plugin requires Nextflow 25.10.4 or newer, as declared in `build.gradle`.
CI runs the full end-to-end suite, including startup warning counts and exit
statuses, on every supported stable release line: 25.10, 26.04, and 26.10.
The [validation matrix](.github/workflows/validation.yml) pins the latest patch of
each line (25.10.8, 26.04.7, and 26.10.2), plus the minimum 25.10.4 for the
startup-validation regression. Keep this matrix current when adopting a new stable release line or
changing the minimum supported version; edge releases are not part of it.

To run the same suite against a matrix version locally, override the Nextflow
runtime without changing the locked development toolchain:

```bash
NXF_VER=25.10.4 mise run validate
```

### Build caching

Gradle's local build cache is enabled by default for builds and unit tests. Unchanged,
cacheable tasks can restore their outputs even after `./gradlew clean`; Gradle
invalidates entries when their declared inputs change. Use `--no-build-cache` to
disable reuse for a run, or `--rerun-tasks` to force task execution.

The build and validation workflows use `gradle/actions/setup-gradle` to cache
Gradle distributions, dependencies, and build outputs between GitHub Actions runs.
Only runs on the default branch write caches; pull requests restore them read-only.
The action's job summary reports cache usage. Configuration caching is not enabled.

For Amp orbs, `.agents/setup` installs mise and runs `mise install --locked` for
Java 21, Nextflow, and nf-test. It also installs checksum-verified Miniforge
26.9.4-1 with the `conda-forge` and `bioconda` channels required by validation.
The tools remain on `PATH` in subsequent login shells without interactive shell
activation. Setup builds and unit-tests the plugin through mise to warm the
Gradle cache; run `mise run validate` for end-to-end validation.

## Testing with Nextflow

The plugin can be tested without a local Nextflow installation:

1. Build and install the plugin to your local Nextflow installation: `mise run install`
2. Run a pipeline with the plugin: `mise exec -- nextflow run hello -plugins nf-core-utils@0.1.0`

## Publish a release

Use the manually triggered [Publish plugin release workflow](.github/workflows/release.yml).
It publishes the commit selected when the workflow starts, not a later main-branch commit.

The [release access-control proposal](docs/release-access-control.md) documents
the proposed team, environment, credential, and tag boundaries and their limits.
It is not enabled: team IDs need verification; credentials and live settings require approval.
The local proposal's `expected_sha` input optionally binds a manual dispatch to a
full commit SHA; automation must supply the main commit it checked. It is not an
authorization override. Existing publication and partial-release checks still apply.

### One-time setup

Add an Actions secret named `NPR_API_KEY` to this repository, or grant it access to an organization secret with that name.
The token must authorize publishing `nf-core-utils` for the `nf-core` provider in the Nextflow Plugin Registry.
Enter the token through GitHub's secret input, not in source code, command arguments, or chat.
The existing `nf_core_bot_auth_token` must also be available to the documentation workflow for its dispatch to `nf-core/website`.

### Release a prepared version

1. Compare main with the latest published tag. Use a minor version for new public functions and a patch version for compatible fixes.
2. Set `version` in `build.gradle` and update the explicit `nf-core-utils@<version>` pins in `validation/`.
3. Add a dated section to the [changelog](./CHANGELOG.md). Keep previously released entries under their original version.
4. Commit the release preparation and land it on main through the repository's review process.
5. In GitHub Actions, select **Publish plugin release**, choose **main**, and enter the prepared version without a `v` prefix.
   Alternatively, run this command, replacing `0.7.0` with the prepared version:

   ```bash
   gh workflow run release.yml --repo nf-core/nf-core-utils --ref main -f version=0.7.0
   ```

The workflow rejects existing tags, GitHub releases, and registry versions. It then:

- Builds the plugin and runs unit tests and end-to-end validation.
- Publishes to the Nextflow Plugin Registry and checks the package's SHA-512 checksum.
- Downloads the published plugin into a fresh cache and reruns end-to-end validation against that download.
- Creates the GitHub release and tag at the tested commit, using the changelog as release notes.
- Dispatches `website-docs.yml` to request the documentation update on nf-co.re.

Check both the release run and the separate documentation workflow before reporting their status.
A successful docs dispatch requests a website update; it does not prove that the resulting website pull request has merged.
The release run retains its package, test evidence, registry metadata, and release notes as the `release-evidence` artifact.

### Recover a partial release

If the run fails before registry publication, fix the cause and rerun after confirming that the version is still unpublished.
If publication may have started, inspect the exact registry version before retrying:

```bash
curl --fail --silent --show-error \
  'https://registry.nextflow.io/api/v1/plugins/nf-core-utils/<version>'
```

Once a version is published, the workflow intentionally refuses to republish it.
Use the failed run's commit and `release-evidence` artifact to verify the registry checksum and finish fresh-download validation before creating any missing GitHub release.
If only the documentation dispatch failed, rerun `website-docs.yml` with the existing release tag as `ref_name`.
Preserve published versions and tags. Fix a defective published package in a new version rather than replacing it.

### Publish locally when CI is unavailable

`releasePlugin` uploads the plugin to `https://registry.nextflow.io/api`. It does not run tests, create Git tags, or create GitHub releases.
For an explicitly authorized local release, follow the same checks and ordering as the workflow.
Set `npr.apiKey` in `$HOME/.gradle/gradle.properties`, or use `NPR_API_KEY`, then run `mise run release` only after testing.
Check the actual Nextflow and nf-test versions: personal tools earlier in `PATH` can override mise's tools.
For isolated local validation, use the executables from `mise which`, an empty `NXF_HOME`, and a temporary `CONDARC` containing the `conda-forge` and `bioconda` channels.
Keep nf-test's default work directory so snapshot path normalization stays stable. Change neither personal configuration nor snapshots to make a release check pass.
No pull request to the legacy plugin index is needed.
