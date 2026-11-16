# Docs handoff status (#61)

This records the three stages of getting plugin docs onto the website as separate
facts: the dispatch from this repo, the downstream website pull request, and live
publication. A green dispatch says nothing about the other two. Status is as observed
on 2026-11-16; the website facts come from `gh` reads and a live page fetch, and may
have changed since.

## 1. Status by stage

| Stage                                                                                | 0.6.0                                                 | 0.7.0                                                                                                | Evidence                                                                                                                                                                      |
| ------------------------------------------------------------------------------------ | ----------------------------------------------------- | ---------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Dispatch (`website-docs.yml` starts `add-nf-core-utils-docs.yml` in nf-core/website) | OK. Release run `39005609093` succeeded (2026-10-17). | OK. `workflow_dispatch` run `41164898059` succeeded (2026-10-31, 11 s).                              | `gh run list -w website-docs.yml`.                                                                                                                                            |
| Downstream website PR                                                                | Merged. nf-core/website#4484, 2026-10-21.             | Open. nf-core/website#4529, created 2026-10-31, review required, checks green, mergeability unknown. | `gh pr view`. Review reason not determined.                                                                                                                                   |
| Live publication                                                                     | Live.                                                 | Not live.                                                                                            | `https://nf-co.re/docs/nf-core-tools/nf-core-utils/nfcoreutilities` returns HTTP 200 with `citationsOnTheFly` (0.6.0 content) but no `getReferencePath` or `citationOutputs`. |

The 1.0 dispatch cannot happen today: `.github/release-access.json` has `approved=false`.
Dispatch in `release.yml` is at line 183, because releases created with `GITHUB_TOKEN`
do not fire release-event workflows.

## 2. Other findings

1. nf-core/website#4490 switches the website workflow to `mise run website-docs`, adds
   `md_github_url` edit links and a breadcrumb fix. It is open, last updated 2026-11-10.
2. The header comment in `.github/workflows/website-docs.yml` and the 0.7.0 CHANGELOG say
   the website runs `mise run website-docs`. The website's current `main` workflow uses an
   inline shell loop, so the comment is wrong and "Edit this page" links are not live until
   #4490 merges.
3. The inline loop copies only `NfCoreUtilities.md`, `NextflowPipelineExtension.md`,
   `ReferencesExtension.md` and `utilities/*.md`. A new `docs/migration.md` would not
   publish until #4490 lands, and must also be added to `USER_DOCS` in
   `mise-tasks/website-docs.py:24`.
4. Whether the `nf_core_bot_auth_token` secret is valid was not verified. The 0.7.0
   dispatch succeeded, which suggests it is.

## 3. Acceptance for #61 not yet met

1. Dispatch run URL, downstream PR URL with merged status, and a live-page check
   recorded as separate facts for the 1.0 tag.
2. The live page contains `getReferencePath`, `citationOutputs` and a link to the 1.0
   migration page after the website PR merges.
3. `mise run website-docs --release <1.0 tag>` produces pages for every file in
   `USER_DOCS`, with frontmatter and `md_github_url`. Not run here.
4. The workflow comment matches the website's behaviour, or website#4490 is merged.

## 4. Open questions

The release approval model changed after this status was recorded: approval comes from
the `plugin-release` environment's required reviewers (#124), and RCs publish as
`1.0.0-rc.N` (#125). Neither changes the three-stage status above.

1. Should 1.0 be held until the website PR merges and the live page is verified, or
   tagged first with live verification as a post-release gate? The audit suggested
   tagging first and merging website#4490 before the 1.0 dispatch. This is not decided.
2. Are website#4529 and #4490 waiting on a particular reviewer?
3. Does the 1.0 website PR open as a new PR, or update #4529 if that is still unmerged?
   The workflow's `create-pull-request` branch configuration was not read in full.
