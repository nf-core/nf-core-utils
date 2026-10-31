---
name: releasing-nf-core-utils
description: Prepares, publishes, recovers, and verifies nf-core/nf-core-utils releases using the guarded release workflow. Use when asked to cut a plugin release, investigate a partial release, or check GitHub against the Nextflow Plugin Registry.
---

# Releasing nf-core-utils

Treat [release.yml](../../../.github/workflows/release.yml) and the
[README release and recovery instructions](../../../README.md#publish-a-release)
as the source of truth. Read both before acting; do not replace their checks with
a local upload or recreate their release-note extraction.

## Authority and credentials

- A readiness check or registry comparison is read-only. It does not authorize
  merging, pushing, publishing, workflow dispatch, or changing credentials.
- Obtain explicit authorization for the specific publication or recovery action.
  Respect any pause on releases even when CI is green. A request to ship a PR is
  not permission to publish the plugin.
- Never read or print secret values to diagnose authentication. Use GitHub's
  private secret input for the repository's `NPR_API_KEY`; the docs workflow also
  needs `nf_core_bot_auth_token`. Report only availability, not values.
- Do not pass tokens as command arguments, write them into the repository, embed
  personal secret-manager references, or modify global Gradle configuration.
  The Gradle publisher accepts `NPR_API_KEY` or `npr.apiKey`, not the obsolete
  `pluginRegistry.accessToken` key. Follow the README only if a local release is
  separately authorized because CI is unavailable.

## Prepare and check readiness

1. Confirm the repository is `nf-core/nf-core-utils`. Fetch `origin/main` and
   tags; inspect the remote state rather than assuming a feature branch is ready.
   Preserve author history and unrelated local changes; never delete an existing
   worktree or reset it to prepare a release.
2. Check the intended release-preparation PR and its exact head checks:

   ```bash
   gh pr view <pr> --repo nf-core/nf-core-utils --json state,headRefOid,mergeStateStatus,statusCheckRollup,mergedAt,mergeCommit,url
   ```

   Release preparation must land on main through review before publication.
   Do not merge it without authorization. Check CI for the selected main commit,
   not only an older PR head.

3. Compare main with the latest published tag to choose a minor version for new
   public functions or a patch for compatible fixes. Confirm the prepared stable
   `X.Y.Z` version in `build.gradle`, every explicit validation pin, and a dated
   changelog section with release notes. Keep earlier release entries intact.
4. Inspect existing remote tags, GitHub releases, and the exact registry version.
   A transport/authentication error is not evidence that a version is absent.
   If publication may have started, use the recovery path below instead.
5. Report the intended version, selected commit, CI/review state, and outstanding
   prerequisites. Do not dispatch while publication is paused or unauthorized.

## Publish through GitHub Actions

Only after explicit publication authorization, use **Publish plugin release** on
**main**, passing the prepared version without a `v` prefix. Use the command in
the README with that version; do not run `make release` or bypass the workflow.

The workflow pins the selected commit and rejects existing tags, GitHub releases,
and registry versions. It builds, unit-tests, and validates before publishing;
checks the registry SHA-512 checksum; downloads into a fresh cache and validates
that download; then creates the GitHub release/tag at the tested commit and
dispatches documentation. A successful upload alone is not a completed release.

Track the exact release run, its selected commit, and its `release-evidence`
artifact. Check the separate `website-docs.yml` run too. Docs dispatch requests a
website update; it does not prove the website pull request has merged.

## Recover a partial release

1. Read the failed run, its selected commit, logs, and `release-evidence`. Query
   the exact registry version as shown in the README before deciding to retry.
2. If the failure was before publication and the version is confirmed unpublished,
   fix the cause and rerun only with authorization. For a 401, have the authorized
   maintainer configure `NPR_API_KEY` through the private secret input; never
   copy credentials into arguments or retry a potentially published version.
3. Once published, do not rerun publication, overwrite a tag, or replace a package.
   Verify the registry status and SHA-512 against the failed run's package, then
   finish fresh-download validation at that same commit before creating any
   missing GitHub release. If evidence is unavailable or mismatched, stop and
   report the blocker rather than tagging current main or guessing provenance.
4. If only docs dispatch failed, use the existing release tag as `ref_name` for an
   authorized `website-docs.yml` rerun. A defective published plugin requires a
   new version, not replacement of the old one.

## Read-only release comparison

Run [scripts/compare_release_surfaces.py](scripts/compare_release_surfaces.py)
with `uv run <path-to-skill>/scripts/compare_release_surfaces.py`.
It explicitly queries this repository, paginates GitHub releases, excludes drafts,
and compares tags with registry versions whose status is `PUBLISHED`. It reports
unpublished registry entries separately and exits nonzero on API/schema failures.
It never publishes or repairs discrepancies.

Set equality is not proof of package integrity or tag provenance. For the target
release, also check the workflow evidence, exact tag commit, package checksum,
fresh-download validation, and documentation status. Report only mismatches
confirmed by current queries; do not assume historical exceptions still apply.

## Report

Distinguish readiness from publication. Include version, selected commit, exact
CI/workflow results, registry/GitHub status and URL when available, checksum and
fresh-download evidence, documentation status, and remaining blockers. Never
claim cross-system completion from an upload message or a successful dispatch.

[evals/evals.json](evals/evals.json) contains dry-run scenarios for checking these
authority and recovery boundaries without performing release actions.
Run the comparison's regression tests with `python3 -m unittest discover -s
<path-to-skill>/scripts -v`.
