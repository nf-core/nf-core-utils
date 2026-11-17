# ADR-0002: Approve releases through the environment's reviewers

## Status

Accepted (2026-11-16)

## Context

The release workflow publishes to the Nextflow Plugin Registry. Its first access
design checked the dispatching actor against verified core and infrastructure team
IDs. That needed an `approved` activation flag in `.github/release-access.json` and
a separate `RELEASE_MEMBERS_READ_TOKEN` to read organization membership. The token
was never created, so the design never ran.

GitHub already holds a job that names the `plugin-release` environment until one of
that environment's required reviewers approves it.

## Decision

Release approval comes only from the required reviewers of the `plugin-release`
environment. The repository does not check team membership, holds no organization
credential, and does not read the environment's settings back.

The maintainer's words: "just rely on the GitHub environment settings of who is the
actual release reviewer."

The reviewer list stays as it is for now: the `infrastructure` team and `maxulysse`.

## Rationale

- One source of truth. Who may approve is set in one place, the environment.
- No extra credential to create, scope, rotate or leak.
- A script that reads the environment's protection and fails when it is missing
  restates what GitHub enforces. It was removed for that reason.

## Consequences

- Dispatching and rerunning actors no longer need to be direct members of the core
  or infrastructure teams, and bot actors are no longer rejected.
- The environment may list any reviewers, users or teams.
- There is no activation flag: the release path is live once the environment is
  protected.
- The workflow trusts the environment. An administrator who removes its reviewers
  removes the approval step. See the limits in
  [release access control](../release-access-control.md).

## Alternatives considered

- **Team-membership check with a Members-read token.** Rejected: a second source of
  truth and a credential nobody wanted to create.
- **Read-only check that the environment still requires review.** Rejected as
  tautological.
