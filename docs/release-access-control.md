# Release access control

Release approval comes from the required reviewers of the GitHub environment
`plugin-release`. The repository does not check team membership and holds no
organization credential. [ADR-0002](adr/0002-release-approval-through-environment-reviewers.md)
records why.

## Who can publish

- **Dispatch:** anyone who can run workflows in this repository, as before. The
  workflow runs only when dispatched on `main`.
- **Approve:** a required reviewer of `plugin-release`, who must not be the person
  who started the run (self-review is prevented).

Change who may approve in repository Settings, Environments, `plugin-release`.
The workflow never names the reviewers.

## What the workflow enforces

The `authorize` job in `release.yml` runs before any write-capable job, with no
token permissions. It runs only when the workflow is dispatched on `main` of
`nf-core/nf-core-utils`, and fails if the optional `expected_sha` is not the commit
being released.

The workflow does not read the environment's settings: GitHub enforces them. Keep
`plugin-release` configured with at least one required reviewer, self-review
prevention, no admin bypass and deployments from `main` only.

The `release` and `dry_run` jobs run in `plugin-release`, so GitHub holds them for
approval. They run only if `authorize` succeeded in the **current run attempt**:
rerunning only a failed job cannot reuse an earlier attempt's authorization, so use
**Re-run all jobs**. A rerun still follows the partial-release recovery checks in the
README; a registry version is never published twice. `dry_run: true` waits for the
same approval and publishes nothing.

Store `NPR_API_KEY` only as a `plugin-release` environment secret, so it is released
to a job only after approval.

## Remaining limits

- The workflow trusts the environment's settings. If an administrator removes the
  required reviewers, or deletes the environment so that GitHub recreates it
  unprotected, the next release runs without approval. A recreated environment has
  no `NPR_API_KEY`, so that case stops at the prerequisite check.
- Environment approval is job-level, not tied to one workflow file. A workflow
  change on `main` could request the same environment, so reviewers should read the
  selected commit before approving.
- Repository administrators can also edit the environment's secrets and the workflow.
  Disabling runtime admin bypass does not stop reconfiguration.
- GitHub requires one approval among the reviewers, not one from each.
- Anyone with write access can still create GitHub releases and tags by other means
  or hold a registry key outside GitHub; use rulesets and key hygiene for those.
