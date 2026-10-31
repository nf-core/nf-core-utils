# Release automation proposal — disabled, not approved

This is a review-only proposal against main commit
`56cf21131cfb283beb05d6bfe326df4becaab4e2`, not a release or an enablement request.
The template is outside `.github/workflows`, has a `.disabled` suffix, and has
an unconditional `if: ${{ false }}` job gate. The helper never runs on import.
No secrets, environments, permissions, team access, rulesets, or releases were
changed by this proposal. [0.7.0 is already released](https://github.com/nf-core/nf-core-utils/releases/tag/0.7.0)
and must not be republished to test automation.

## Policy and activation blockers

[nf-core/ops#239](https://github.com/nf-core/ops/issues/239) tracks the policy.
The intended teams are [nf-core/core](https://github.com/orgs/nf-core/teams/core)
and [nf-core/infrastructure](https://github.com/orgs/nf-core/teams/infrastructure).
Their verified numeric IDs are 2649377 and 8796434. Publication requires approval
by a different eligible member of either team, with self-approval prevented.
Existing initiation permissions stay unchanged. The guard uses verified active
direct membership; inherited-member expansion requires a separate policy decision.

The dispatch principal and machine-identity policy remain unapproved, and runtime
credential/native approval enforcement still need verification. No live permission
or credential configuration is established by this draft. Do not substitute
admin, write, org membership, tag push or bot status for release authorization.

GitHub's [environment documentation](https://docs.github.com/en/actions/reference/workflows-and-actions/deployments-and-environments)
supports required team reviewers for public repositories on current plans;
there is no demonstrated need for a paid-tier upgrade for this public repo.
Legacy plans and any future private/internal visibility require reassessment.
GitHub requires only **one** approval across the configured reviewers, not one
from each team. If approval from both teams is intended, a single reviewer list
cannot express that policy. Disabling runtime admin bypass does not stop an
administrator from reconfiguring the environment or workflow.

## Proposed entry point

After successful main-push `Plugin Validation`, the proposed workflow waits for
an actual-team approval through a separately configured protected environment.
Only then does it access a dedicated dispatch credential and:

1. Reject forks, PR events, other branches, failed/skipped validation, and stale
   main commits. It never executes candidate code or consumes candidate artifacts.
   Automation code is checked out at the workflow's trusted default-branch SHA.
2. Query the latest main-push runs of both `build.yml` and `validation.yml` at the
   exact candidate SHA. Both must be completed and successful. Workflow identities
   are paths, not caller-provided names. API/auth errors fail closed.
3. Read the stable version from `build.gradle` at that SHA as data, not shell code.
4. Refuse any prior `release.yml` attempt at the same commit, including a failed
   or pending attempt. Recheck main immediately before dispatch.
5. Dispatch **only** existing `release.yml` on `main`, with `version` and full
   `expected_sha`. No authorization/bypass flags, direct upload, tag creation,
   publisher credential, website token, or automatic write retry exists here.

The receiver's optional `expected_sha` input must be compared to `github.sha`
before publisher credentials/write permissions.
Automation always supplies it; manual version-only use remains compatible.
The second main check is helpful but cannot close the check/dispatch race.
The receiver's exact-SHA rejection is mandatory, not optional activation polish.

`release.yml` remains the sole owner of its version/pin/changelog checks,
tag/GitHub-release/registry uniqueness checks, build/unit/E2E gates, SHA-512
checksum, fresh-cache download validation, exact-commit tag, evidence artifact,
and docs dispatch. Neither CI completion nor a dispatch HTTP success means that
publication or documentation succeeded. Follow [the existing release and recovery
instructions](../../README.md#publish-a-release); inspect the release run, separate
docs run, and resulting website PR status before claiming completion.

Concurrency serializes candidates, and existing-run inspection reduces duplicate
requests, but GitHub dispatch has no idempotency key and run indexing can lag.
The downstream release concurrency/uniqueness guards remain necessary. Never
automatically retry an ambiguous dispatch response or a partial publication.
If build CI finishes later than validation, the proposal refuses dispatch; an
operator must investigate and explicitly rerun the candidate workflow after CI.
It does not poll, schedule retries, or bypass the protected approval on purpose.

## Automation policy choices

Choose one before activation:

- **Team-member initiation (current authorization design):** retain
  direct manual dispatch by a verified actual-team human under the receiver's
  fail-closed membership check. Keep this automation dormant. If an unattended
  dispatch credential is proposed for a human identity, explicitly approve that
  security/attribution tradeoff rather than treating token ownership as a human
  actively requesting each release.
- **Automated proposal + team approval:** allow a specifically approved machine
  principal to request publication only after protected approval by a verified
  actual-team human. This requires an explicit downstream policy change by the
  release maintainers if the principal cannot satisfy team-member initiation. Do not
  pretend a bot is a human team member or accept an `approved=true` input. The
  receiver must enforce a verifiable protected approval, not just trust this
  entry point to have run. Direct dispatch must not bypass that approval.

In both cases, authoritative numeric team IDs and membership semantics (direct
vs inherited, active membership, initiating actor and rerun actor) must be agreed
and verified with an org-authorized read-only Members credential, separate from
publisher and dispatch credentials. Unknown/403/empty policy must deny access.
No implicit `GITHUB_TOKEN`, website bot, app, or user allowlist is proposed.

## Activation dependencies — require separate approval

- Approve the policy above, authoritative teams, principal, and least-privilege
  credential type/scope. A fine-grained dispatch credential needs Actions write
  and Contents read for this repository, not registry or organization write.
  Do not use a personal token as an unreviewed bot-policy workaround.
- Land and verify the authorization guard and exact-SHA receiver contract.
  Prove nonmembers, bots without approved policy, and stale SHAs cannot reach
  publisher credentials, including direct dispatch and reruns.
- Create/verify the proposed `release-automation-approval` environment with
  approved **actual teams** only, self-review prevention, no admin bypass, and
  main-branch-only deployments (no tags). Scope `RELEASE_DISPATCH_TOKEN` to that
  protected environment; no repository-level fallback. The environment name
  here is a proposal, not an existing protected resource. Referencing a nonexistent
  environment may create it unprotected, so never activate first and configure later.
- Require the receiver's own protected publication approval/authorization before
  registry or GitHub write credentials. Secure workflow/helper edits through the
  approved repository review/ruleset policy; verify the rules actually cover
  release code and tags.
- Explicitly approve moving the template into `.github/workflows` and replacing
  the literal disabled gate. Test rejection cases without publishing; only a
  separately authorized future release can verify live dispatch/publication.

Rollback before activation is deletion of these proposal files. After a separately
approved activation, disable the entry point first and revoke its dispatch token;
preserve published packages/tags and use README partial-release recovery. Code
rollback cannot unpublish a registry version or undo a leaked credential.

## Local verification

Run from the repository root with Node 24+ (matching `github-script@v9`):

```bash
node --test scripts/release-automation.test.cjs
```

Tests exercise the public helper with a fake GitHub API: exact dispatch payload,
untrusted events, stale commits (including movement during checks), each required
CI workflow and newest failures, version parsing, prior partial releases, API
denials, ambiguous dispatch failure without retries, and the template's inert
state. They make no network calls and use no credentials. These are local contract
tests, not evidence of live team authorization or a successful release.
