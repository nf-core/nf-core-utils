# Release access control

Release approval comes from the required reviewers of the GitHub environment
`plugin-release`. The repository does not check team membership and holds no
organization credential.

**Decision (2026-11-16, maintainer):** "just rely on the GitHub environment
settings of who is the actual release reviewer. I don't know why we need to read
the teams." The reviewer list stays where it is for now: the environment's
current required reviewers are the `infrastructure` team and `maxulysse`. This
replaces the earlier design (verified core/infrastructure team IDs, direct-member
checks on the dispatching actors, an `approved` activation flag and a separate
`RELEASE_MEMBERS_READ_TOKEN`), which never ran because that token was never created.

## Who can publish

- **Dispatch:** anyone who can run workflows in this repository, as before. The
  workflow runs only when dispatched on `main`.
- **Approve:** a required reviewer of `plugin-release`, who must not be the person
  who started the run (self-review is prevented).

Change who may approve in repository Settings, Environments, `plugin-release`.
The workflow never names the reviewers.

## What the workflow enforces

`release.yml` runs `scripts/release/check_release_access.py` in the `authorize` job
before any write-capable job. It uses only the workflow's own token (`contents: read`,
`actions: read`) and fails closed unless:

1. The run is `release.yml` dispatched on `main` of `nf-core/nf-core-utils`.
2. The optional `expected_sha` is the full commit being released.
3. The `plugin-release` environment exists and has a required-reviewers rule with
   at least one reviewer, `prevent_self_review: true` and `can_admins_bypass: false`.
4. The environment deploys only from the selected branch `main`, not tags.

Checks 3 and 4 exist because GitHub silently creates an unprotected environment when
a job names one that does not exist; removing the reviewers must not turn publishing
into an unreviewed action. Any API error or missing field denies.

The `release` and `dry_run` jobs run in `plugin-release`, so GitHub holds them for
approval. They run only if `authorize` succeeded in the **current run attempt**:
rerunning only a failed job cannot reuse an earlier attempt's authorization, so use
**Re-run all jobs**. A rerun still follows the partial-release recovery checks in the
README; a registry version is never published twice. `dry_run: true` waits for the
same approval and publishes nothing.

Store `NPR_API_KEY` only as a `plugin-release` environment secret, so it is released
to a job only after approval.

## Protections removed

- Dispatching and rerunning actors no longer need to be direct members of the core
  or infrastructure teams; bot actors are no longer rejected.
- The environment may list any reviewers (users or teams, one or many); it no longer
  has to be exactly the two teams, and child teams are not inspected.
- No activation flag: the release path is live as soon as the environment is
  protected.

## Remaining limits

- Environment approval is job-level, not tied to one workflow file. A workflow
  change on `main` could request the same environment, so reviewers should read the
  selected commit before approving.
- Repository administrators can edit the environment, its secrets and the workflow.
  Disabling runtime admin bypass does not stop reconfiguration.
- GitHub requires one approval among the reviewers, not one from each.
- Anyone with write access can still create GitHub releases and tags by other means
  or hold a registry key outside GitHub; use rulesets and key hygiene for those.
