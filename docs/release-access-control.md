# Release access control — fail-closed, activation pending

Outcome: restrict release initiation and approval to the actual nf-core core and
infrastructure teams, without conflating repository write access with release
authority. Done for this proposal means an offline-tested local patch and an
explicit account of the live settings and decisions still required. This document
does not authorize changing GitHub settings, credentials, memberships, or publishing.

## Status and open design choices

[nf-core/ops#239](https://github.com/nf-core/ops/issues/239) tracks the policy and
implementation decisions. The intended teams are settled:
[nf-core/core](https://github.com/orgs/nf-core/teams/core) and
[nf-core/infrastructure](https://github.com/orgs/nf-core/teams/infrastructure).
Authenticated read-only inspection verified their numeric IDs as **2649377** and
**8796434**, respectively. Both currently have no parent or child teams, and
active direct membership evidence includes `inherited: false`.

**A second approval is mandatory:** a different eligible member of either team
must approve publication, with self-approval prevented. Existing workflow
initiation permissions remain unchanged; this proposal does not give additional
users dispatch rights. The guard accepts active direct members and rejects
hierarchy expansion; future inherited-member eligibility remains a separate
policy choice. No bot exception, machine identity or global release-access
change is approved by this code.

**Fail-closed landing:** the policy intentionally remains `approved: false`,
despite verified team IDs and the settled approval design. This flag gates
activation, not agreement with the design. Landing the workflow in that state
blocks publication until the prerequisites and native tests succeed. The automation
template is outside `.github/workflows` and unconditionally disabled.
The already published [0.7.0 release](https://github.com/nf-core/nf-core-utils/releases/tag/0.7.0)
must not be republished to test these proposals.

Validation requires organization Members read for team identities and membership,
repository Actions read for environment metadata, repository Secrets read for
repository/shared-organization secret metadata, and Environments read for
environment-secret metadata. An authorized administrator can provide sanitized
metadata instead; no secret values are needed. Successful publication alone
does not prove environment-only credential isolation. API access errors do not
prove credentials or controls are absent.

### API contracts and activation blockers

GitHub's [2026-03-10 REST member documentation][team-members] explicitly defines
`inherited: false` as direct membership. Its [pinned OpenAPI source][rest-source],
at `components.schemas.team-member.properties.inherited`, additionally says this
field is **only present when the feature is enabled for the organization**. It is
not a required schema property. Therefore the guard's missing-field denial is
deliberate. Authenticated inspection confirmed the field for both selected teams
with API versions 2022-11-28 and 2026-03-10. Its availability with the intended
runtime membership credential must still be checked; selecting an API version
alone does not enable the feature.
The membership-state endpoint alone includes child members and is insufficient.
If an authorized reader cannot obtain this field, an explicitly tested direct-only
API design is required before activation, not treating absence as `false`.

GitHub's [App permission table][app-permissions] lists both get-environment and
list-deployment-branch-policies under repository **Actions: read**, not Contents
or Administration read. The authorization job explicitly requests `actions: read`
as well as checkout's `contents: read`. The actual protected environment still
needs a read-only workflow-token probe; public endpoint access is not proof of
the intended token's permissions. Missing fields or access failures deny.
`can_admins_bypass` is absent from the pinned environment schema, so its
availability with the intended token must be verified rather than assumed.

## Proposed workflow boundary

The existing `release.yml` remains the sole publisher. Its version input,
concurrency, prerequisite checks, validation, checksum verification, fresh-cache
download validation, tested-commit release/tag creation, documentation dispatch,
and always-uploaded evidence steps remain unchanged.

1. Default workflow token permissions become `contents: read`. A separate
   `authorize` job checks this repository, `workflow_dispatch`, main, and the
   canonical workflow path. It receives no registry credential or write token.
   It additionally requests `actions: read` for environment metadata checks.
2. Optional `expected_sha` must be a full 40-hex commit matching `github.sha` when
   supplied. Existing manual version-only calls can omit it. Automation must
   supply the exact main commit it checked, closing a check-main/dispatch race.
   This binds provenance, never grants authority.
3. Approved policy must contain exactly the two verified team slugs and numeric
   IDs, labelled `core` and `infrastructure`. Each run checks metadata against
   GitHub, then requires **both** original `github.actor` and current
   `github.triggering_actor` to be active **direct** members of either team.
   An org owner/admin is not exempt. Inherited child-team membership is not
   accepted; missing `inherited: false` evidence denies rather than broadens.
   Native environment reviewers inherit child-team access, so the guard also
   rejects children outside the two approved teams. If live hierarchy includes
   other teams, activation is blocked pending an approved narrower reviewer
   design; listing the parent team alone is not proof of limited approval rights.
4. Membership reads use a separately approved, read-only organization Members
   credential (`RELEASE_MEMBERS_READ_TOKEN`). Prefer a narrowly installed GitHub
   App or fine-grained token with Members read only; no repository write or
   publisher permissions. The normal `GITHUB_TOKEN` has no configurable org
   Members permission. No fallback to org membership, admin status, or writer
   status is allowed. Errors, missing credentials, and unknown identities deny.
5. Before the write-capable job can start, verify a pre-existing `plugin-release`
   environment has exactly the two team reviewers, self-review prevention,
   `can_admins_bypass: false`, and exactly one selected **branch** policy: `main`.
   A missing environment must deny: GitHub can otherwise auto-create an
   unprotected environment merely by referencing its name.
6. The release job needs successful authorization for the **current run attempt**,
   then waits for native environment approval. Only it requests contents/actions
   write permissions and receives publisher credentials. Rerun **all** jobs for
   a fresh authorization; rerunning only the failed release job cannot reuse an
   earlier attempt's output. A rerun must still follow the existing partial-release
   recovery checks; successful registry publication must never be repeated.
7. `dry_run: true` excludes the publisher job independently of authorization.
   A separate read-only job waits for the same environment approval and records
   success without referencing publisher secrets or writing release state.
   Dry runs do not override disabled policy, missing credentials or actor checks.

GitHub allows up to six environment reviewers, but **one** approval among them is
sufficient: two team reviewers mean core **OR** infrastructure, not one of each.
The required second person may belong to either selected team; approval from
both teams is not required. Self-review prevention is mandatory. Native approval
and rerun behavior still need validation in a non-publishing test repository.

No GitHub App/bot exception is included. GitHub `Bot` identities are denied;
machine accounts represented as ordinary `User` accounts cannot be distinguished
reliably from humans by that type alone. Do not add them to approved teams as a
workaround. Any automation identity, credential, and scope require separate approval.
The automation entry point may only dispatch this workflow on main with version
and expected SHA; it cannot upload directly or pass an authorization flag.

## Enforcement across all release paths

| Path                                              | Proposed control                                                                                                           | Remaining limit / required decision                                                                                                                                                                                                                                                        |
| ------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Manual workflow dispatch/rerun                    | Original and triggering actor checks, fresh attempt authorization, protected environment                                   | GitHub still lets writers request a dispatch. The proposal gates execution, not visibility of the button.                                                                                                                                                                                  |
| Nextflow registry upload                          | Store `NPR_API_KEY` **only** in `plugin-release`; approval required before use                                             | Remove/revoke repository and accessible organization copies after an approved migration. Other workflows can otherwise still use them. External holders can publish independently of GitHub.                                                                                               |
| Other workflows                                   | Protected environment plus main review rules covering workflows, guard, policy, and build/publish code                     | Environment approval is job-level, not an allowlist for one workflow file. A modified workflow on main could request it. Reviewers must inspect the selected commit and job.                                                                                                               |
| GitHub Releases UI/API, including an existing tag | Restrict write-capable identities/tokens outside the two teams; consider immutable releases as integrity defense           | Standard write access / Contents write can create, edit, or delete release records. Tag rules do not turn release metadata/assets into a team-only API. Immutable releases do not authorize creation.                                                                                      |
| New release tags                                  | A tag creation ruleset, no broad write/admin-role bypass; verified teams and a separately approved publisher identity only | The workflow's normal `GITHUB_TOKEN` must not be assumed to inherit its dispatcher's team bypass. A dedicated publisher App/token and its exact bypass identity need approval and verification before enabling rules. A broad GitHub Actions App bypass would empower unrelated workflows. |
| Existing tag update/delete                        | Separate no-bypass tag ruleset for update/deletion, covering all release tags                                              | Separating creation bypass from immutable tag rules avoids giving tag creators permission to rewrite/delete history. Emergency changes require an explicit policy-change process.                                                                                                          |
| Local release / leaked credentials                | Registry-side provider rights, narrowly held credentials, rotation after migration                                         | GitHub environment/ruleset controls cannot revoke a token held outside GitHub. Registry administrators remain trusted.                                                                                                                                                                     |
| Repository/org administrators                     | Disable runtime environment bypass; no blanket admin bypass in rulesets; minimize administration grants                    | Admins/org owners can still edit/remove environments, secrets, rulesets, workflow code or access grants. No workflow can enforce policy against its own controllers.                                                                                                                       |

Tag rules should cover **all tags** unless an approved inventory identifies
non-release tags needing an exception. A narrow `0.*` pattern misses future
major versions; GitHub ruleset patterns are not semantic-version validators.
Use a selected main **branch**, not "protected branches only": GitHub documents
that the latter allows all branches when no branch protection is configured.

The proposal does **not** yet claim "only core+infra can cut releases" globally.
That claim requires approved permission changes: audit org base permissions,
direct collaborators, all team grants, custom roles, installed Apps, deploy keys,
PATs, and automation tokens. Remove other effective writer/Contents-write paths
or explicitly accept that Releases UI/API remain outside the team gate. For a
public repository, contributors without write can still contribute through forks.

## Tier and administrator limits

- [Environments and protection rules][environments] are available for public
  repositories on current plans, not legacy Bronze/Silver/Gold plans. Required
  reviewers and disallowing admin runtime bypass do not require assuming this
  public repository has Enterprise. Private-repository support differs by tier;
  a visibility or plan change requires rechecking enforcement.
- [Branch/tag rulesets][rulesets] are available for public repositories on current
  plans; organization-level capabilities differ by plan. The observed org rule
  is not proof of the exact billing tier or all desired features.
- [Custom repository roles][roles] require Enterprise Cloud and are **additive**
  to inherited roles. They cannot subtract release rights from standard write.
  Higher org/team/direct grants win; a custom read role does not cancel another
  write grant. Do not propose "write except releases" as a standard role switch.
- Repository admins can configure environments and repository rules; organization
  policy controllers can change organization rules. Runtime bypass prevention
  is not immutable governance. Restricting those controllers requires an explicit
  access decision, outside the locally prepared code.

## Approval and staged activation (no actions taken)

The design requires human team initiation plus a different eligible team
approver and environment-scoped registry credentials, with no bot exception.
The implementation checks direct membership and denies hierarchy expansion;
inherited-member eligibility must not be broadened without a policy decision.
Landing code does not establish operational enforcement. Complete and verify the
following prerequisites before enabling the policy. A separate permission
audit/change is required for a global Releases UI/API restriction.

Before a separately authorized activation:

1. Recheck the verified team IDs, hierarchy and direct membership using the
   intended runtime credential. Confirm native reviewer selection and repository
   eligibility for both teams; existing initiation permissions stay unchanged.
2. Approve the read-only membership credential and how it is issued/rotated.
   Verify required API fields (`can_admins_bypass`, direct-member evidence) with
   that credential; absent fields deliberately block activation.
3. Securely migrate the publisher key into the protected environment, verify the
   replacement and recovery path, then remove broader accessible copies. Registry
   keys cannot be retrieved through GitHub's metadata API; secure input may be
   necessary. Do not reuse a broad operator OAuth token as the membership credential.
4. In a non-publishing test repository, test allowed/denied humans, unauthorized
   rerunners, wrong SHA, missing policy/environment, native approval, failed-job
   reruns, tag rules, and Releases API access. Offline fixtures are not proof of
   GitHub enforcing native policy or of token scope.
5. Precreate/protect the environment and read back both team reviewers,
   self-review prevention, no admin bypass and main-only selected branch policy.
   Never combine this with an in-progress release. Recheck the exact configuration
   and native dry-run results before changing `approved`. Team-grant changes,
   tag rules and global Releases UI/API restrictions must stay separately scoped.
6. Publish only under separate release authority. Keep the README's partial-release
   recovery path. Do not roll back to broadly exposed credentials to bypass a
   failure; pause and repair policy.

## Offline verification

Run `python3 -m unittest discover -s scripts/release -v`. Tests use only synthetic
fixtures. They distinguish allowed direct team membership from pending, inherited,
unknown, bot, or unauthorized-rerunner cases, exercise pagination, exact SHA
mismatch, environment drift, recreated teams, and API failures. No test publishes
or writes policy. The checked-in unapproved policy must exit nonzero before API
access. Existing publication steps must remain identical to the base workflow.

[team-members]: https://docs.github.com/en/rest/teams/members?apiVersion=2026-03-10#list-team-members
[rest-source]: https://github.com/github/rest-api-description/blob/f9ed74230d54847b5ca00a1343e8e349e858b8fb/descriptions/api.github.com/api.github.com.2026-03-10.json
[app-permissions]: https://docs.github.com/en/rest/authentication/permissions-required-for-github-apps?apiVersion=2026-03-10#repository-permissions-for-actions
[environments]: https://docs.github.com/en/actions/reference/workflows-and-actions/deployments-and-environments
[rulesets]: https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/creating-rulesets-for-a-repository
[roles]: https://docs.github.com/en/organizations/managing-user-access-to-your-organizations-repositories/managing-repository-roles/about-custom-repository-roles
