# DRAFT: not approved. Release gates and rollback policy for 1.0.0 (#56)

This is a proposed gate checklist and rollback policy for tagging 1.0.0. Nothing here
is approved or in force. It needs maintainer approval of the gates and rollback rules,
confirmed approvers and named release-candidate (RC) consumers (#60) before it becomes
policy. It also lists the steps only a human with release authority can perform, and one
ordering question that is still open.

Maintainer decisions of 2026-11-16 are folded in below. Release approval comes from the
required reviewers of the `plugin-release` environment, and the team-membership check and
members token are removed (#124, merged). There is no release-candidate version: #125,
which added `1.0.0-rc.N`, was closed. Candidates are small stable releases such as 0.8.0,
which the release workflow already supports. Candidate evidence comes
from the generated template, nf-core/rnaseq and nf-core/createpanelrefs. Template
migration is carried by nf-core/tools#3870; nf-core/tools#3790 was closed. Proposed
approvers are Edmund Miller, the infrastructure team, Matthias, and probably Maxime;
that list is not confirmed. Still open: a verified before/after run against an installed
candidate.

Evidence for the current state was gathered on 2026-11-16 against `main` at `f58032f`.
The `release.yml` and settings facts come from the audit and were not all re-checked
here. `build.gradle` and every validation pin are at 0.7.0.

## 1. Current state

1. #56 is blocked by #63, #61, #58, #59, #64, #60 and #57. Only #63 is a recorded decision.
2. No gate checklist or rollback policy exists in the repo. The README section "Recover a
   partial release" and the skill `SKILL.md` cover partial-release recovery for the
   publisher only. Nothing covers caller pin-back or a known-good plugin and template
   combination.
3. `release.yml` enforces version, pin and changelog agreement only at dispatch time.
4. The guarded path has never run to completion. Until #124 merged on 2026-11-16,
   `.github/release-access.json` had `approved=false` and every dispatch failed in
   `authorize`. Approval now comes from the environment's required reviewers.
5. The release path publishes only stable `X.Y.Z` versions, always as `--latest` and
   always with a website docs dispatch. That is accepted: candidates are small stable
   releases (#125, the `1.0.0-rc.N` path, was closed).

## 2. Gate checklist (DRAFT)

Every gate needs a linked artifact (run URL, command output or PR) at the final SHA.
A skipped or no-op check does not count.

| Gate                | Requirement                                                                                                                                                                                                                                                                                                                           |
| ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| G1 Scope            | The extraction inventory (#55) shows every generic template utility mapped to an existing export. The generated template runs success and failure paths against the candidate zip (#119, #60).                                                                                                                                        |
| G2 Build            | Fresh `mise run assemble`, `mise run test` and `mise run validate` on the final SHA, with test counts shown and no skipped suite. CI Plugin Validation is green on all four pins (25.10.4, 25.10.8, 26.04.7, 26.10.2) at that SHA. Patch pins are refreshed deliberately before release.                                              |
| G3 Reports          | The completion report persists without recipients and on delivery failure. Failure-only mail works. Attachments and remote paths work. Platform suppression works. MultiQC integration does not write MultiQC into the versions topic (#89 documented as a known limit). No live external sends.                                      |
| G4 Docs             | Every public example calls a real export with real values, is executed and not only linted, and states its runtime and parser requirements (#61).                                                                                                                                                                                     |
| G5 Candidate        | The exact RC consumers, reviewers and promotion approver are named in #60, with the sha512 of the candidate zip they ran. Evidence comes from the generated template, nf-core/rnaseq and nf-core/createpanelrefs.                                                                                                                     |
| G6 Release controls | Read-back of the `plugin-release` environment: core and infrastructure reviewers, no self-review, no admin bypass, `main` only. `NPR_API_KEY` is environment-scoped. The native dry-run matrix is recorded. Approval comes from the environment's required reviewers, with no team-membership check or members token (#124, #59).     |
| G7 Consistency      | `build.gradle` version, the five validation pins and a dated CHANGELOG section with 1.0 migration notes agree, and the `release.yml` prerequisites pass. The selected SHA has green CI.                                                                                                                                               |
| G8 Post-publish     | Registry status is `PUBLISHED` and sha512 equals the built zip. A fresh-cache nf-test passes. The GitHub release and tag point at the tested SHA. The release-evidence artifact is kept. The website-docs run is green and the website PR is opened and merged (#61).                                                                 |
| G9 Pin-back         | A known-good combination is documented and tested: plugin 0.7.0 with the template pinned to it, and the template commit that uses 1.0.0.                                                                                                                                                                                              |
| G10 Review bypass   | The repository admin role is off the bypass list of the `main: required checks` ruleset, and the tag ruleset that blocks creating `1.0.0` (id 27414195) is deleted, in that order, before `release.yml` is dispatched with `version=1.0.0` (#135). The bypass was turned on on 2026-11-16 to merge the 1.0 preparation pull requests. |

Review notes from the audit, not yet adopted: make G6 (live dry-run matrix) a hard
blocker, and require a second human as co-approver on the `plugin-release` environment,
since `prevent_self_review` is on.

## 3. Rollback policy (DRAFT)

| Rule | Policy                                                                                                                                                                                                                                                                                                                                                                   |
| ---- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| R1   | Published versions, tags and GitHub releases are immutable. Never delete, overwrite, retag or re-upload. Fix defects with a new patch version (1.0.1).                                                                                                                                                                                                                   |
| R2   | Before registry upload (authorize, prerequisites, build or test failure): fix the cause, confirm the registry GET returns 404 and no tag or release exists, then re-dispatch. Rerun all jobs for a fresh authorization.                                                                                                                                                  |
| R3   | Ambiguous or failed upload: stop and query the exact registry version. If 404, retry is allowed. If `PUBLISHED`, treat as published (R4). If present but not `PUBLISHED`, stop and ask the registry maintainers.                                                                                                                                                         |
| R4   | Published but a later step failed: verify sha512 against the failed run's `build/distributions` zip and release-evidence, finish fresh-download validation at the same commit, then create the missing GitHub release at that commit. If evidence is missing or mismatched, stop and report. Do not tag current `main`.                                                  |
| R5   | Docs dispatch failed: rerun `website-docs.yml` with `ref_name=<tag>`.                                                                                                                                                                                                                                                                                                    |
| R6   | Defective 1.0.0 after publish: callers pin back to the known-good version (`nf-core-utils@0.7.0` or the last good 1.0.x) in `nextflow.config`. The template owners revert the template pin PR. Publish 1.0.1 through the same guarded path. The CHANGELOG notes the withdrawn version. Mark the GitHub release "do not use" only if release-record edits are authorised. |
| R7   | No data migration exists. If a future version writes state, amend this policy before that release.                                                                                                                                                                                                                                                                       |
| R8   | Never loosen release controls (broad credentials, bypass) to speed recovery. Pause and repair the policy.                                                                                                                                                                                                                                                                |

## 4. Steps only a human with release authority can perform

In order:

1. Merge the release-prep PR. The `main` ruleset needs one independent approving review
   and passing CI, build and prek checks. Since 2026-11-16 a repository administrator
   can bypass it; G10 turns that off before 1.0.0.
2. Record the exact RC consumers, reviewers and the promotion approver in #60.
3. Grant or verify the `plugin-release` environment's required reviewers (add the `core`
   team, which is missing today) and move `NPR_API_KEY` into the environment. The
   members token (`RELEASE_MEMBERS_READ_TOKEN`) is no longer needed (#124).
4. Publish a small stable release (for example 0.8.0) through the guarded workflow as
   the candidate, after native test evidence.
5. A core or infrastructure member dispatches `release.yml` with `version=1.0.0` and
   `expected_sha`.
6. A different eligible core or infrastructure member approves the `plugin-release`
   environment.
7. Monitor the run and, if needed, perform recovery under section 3.
8. nf-core/website maintainers merge the docs PR created by `add-nf-core-utils-docs.yml`
   (#61), and the nf-core/tools template PR that consumes 1.0.0 (#119, #64).
9. Optional: an org owner enables immutable releases and a lasting tag ruleset. Immutable
   releases are off (`enabled=false`). The only tag rule is the temporary `1.0.0` block
   in G10.

## 5. Unresolved: validation pin ordering

`release.yml` requires the five validation configs to pin `nf-core-utils@<new version>`
before dispatch (the prerequisite loop at `.github/workflows/release.yml:98-102`). The
new version does not exist in the registry until that same run publishes it. The audit
did not establish how the pre-release pin update and the CI Plugin Validation jobs (which
pin the same version) behave while the version is unpublished. Open question for the
maintainers: do the pins move in the release-prep PR, and if so what makes validation
pass before publication, or does the loop need a different ordering?

Separately, `validation/citations-remote-modules/nextflow.config` pins `0.7.0-dev`, which
the pin loop does not cover.

## 6. Open questions for the maintainers

1. Confirm the proposed approvers (Edmund Miller, the infrastructure team, Matthias and
   probably Maxime): who is the 1.0.0 promotion approver and the second environment
   approver?
2. Will release-record edits (marking a defective release) be allowed while immutable
   releases is disabled and none is planned?
3. Is a tag ruleset in scope for 1.0, or deferred to the access-control follow-up?
4. Do you approve G1-G9 and R1-R8 as written?
