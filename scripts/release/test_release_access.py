"""Offline authorization contract: python3 -m unittest discover -s scripts/release -v."""

import copy
import io
import json
import unittest
import urllib.error
from unittest.mock import patch

import check_release_access as access


class ReleaseAccessTest(unittest.TestCase):
    def setUp(self):
        # Synthetic identities, not assertions about live nf-core team records.
        self.policy = {
            "approved": True,
            "teams": [
                {"role": "core", "slug": "team-a", "id": 101},
                {"role": "infrastructure", "slug": "team-b", "id": 202},
            ],
        }
        self.context = {
            "GITHUB_REPOSITORY": access.REPOSITORY,
            "GITHUB_REF": "refs/heads/main",
            "GITHUB_EVENT_NAME": "workflow_dispatch",
            "GITHUB_WORKFLOW_REF": f"{access.REPOSITORY}/.github/workflows/release.yml@refs/heads/main",
            "GITHUB_SHA": "a" * 40,
            "EXPECTED_SHA": "a" * 40,
            "GITHUB_ACTOR": "alice",
            "GITHUB_ACTOR_ID": "11",
            "GITHUB_TRIGGERING_ACTOR": "alice",
            "GH_TOKEN": "synthetic-repo-token",
            "RELEASE_MEMBERS_READ_TOKEN": "synthetic-members-token",
        }
        self.alice = {"login": "alice", "id": 11, "type": "User", "inherited": False}
        self.bob = {"login": "bob", "id": 22, "type": "User", "inherited": False}
        self.responses = {
            f"repos/{access.REPOSITORY}/environments/plugin-release": {
                "can_admins_bypass": False,
                "deployment_branch_policy": {
                    "protected_branches": False,
                    "custom_branch_policies": True,
                },
                "protection_rules": [
                    {
                        "type": "required_reviewers",
                        "prevent_self_review": True,
                        "reviewers": [
                            {"type": "Team", "reviewer": {"id": 101}},
                            {"type": "Team", "reviewer": {"id": 202}},
                        ],
                    }
                ],
            },
            f"repos/{access.REPOSITORY}/environments/plugin-release/deployment-branch-policies": {
                "total_count": 1,
                "branch_policies": [{"name": "main", "type": "branch"}],
            },
            "users/alice": self.alice,
            "users/bob": self.bob,
        }
        for team in self.policy["teams"]:
            base = f"orgs/nf-core/teams/{team['slug']}"
            self.responses[base] = {**team, "organization": {"login": "nf-core"}}
            self.responses[f"{base}/teams?per_page=100"] = []
            self.responses[f"{base}/members?per_page=100&page=1"] = [
                self.alice if team["id"] == 101 else self.bob,
            ]
            for actor in ("alice", "bob"):
                self.responses[f"{base}/memberships/{actor}"] = {"state": "active"}

    def get(self, path, token, *, missing_ok=False):
        return copy.deepcopy(self.responses[path])

    def check(self):
        with patch.object(access, "api_get", self.get):
            access.authorize(self.context, self.policy)

    def test_direct_members_of_either_team_and_separate_rerunner(self):
        self.context["GITHUB_TRIGGERING_ACTOR"] = "bob"
        self.check()
        self.context["EXPECTED_SHA"] = ""  # Existing manual version-only dispatch.
        self.check()

    def test_expected_sha_mismatch_is_denied_before_any_credential_lookup(self):
        self.context["RELEASE_MEMBERS_READ_TOKEN"] = ""
        for expected in ("b" * 40, "a" * 39, "a" * 41, "z" * 40, "a" * 40 + "\n"):
            with self.subTest(expected=expected):
                self.context["EXPECTED_SHA"] = expected
                with self.assertRaisesRegex(access.AccessDenied, "expected_sha"):
                    self.check()

    def test_wrong_repository_ref_workflow_or_event_is_denied(self):
        for key, value in (
            ("GITHUB_REPOSITORY", "other/nf-core-utils"),
            ("GITHUB_REF", "refs/tags/main"),
            ("GITHUB_EVENT_NAME", "pull_request_target"),
            ("GITHUB_WORKFLOW_REF", "other/workflow@refs/heads/main"),
        ):
            with (
                self.subTest(key=key),
                patch.dict(self.context, {key: value}),
                self.assertRaises(access.AccessDenied),
            ):
                self.check()

    def test_unapproved_missing_or_broadened_policy_denies(self):
        self.policy["approved"] = False
        with self.assertRaisesRegex(access.AccessDenied, "not been approved"):
            self.check()
        self.policy["approved"] = True
        self.policy["teams"].append({"role": "writers", "slug": "everyone", "id": 303})
        with self.assertRaises(access.AccessDenied):
            self.check()
        self.policy["teams"] = []
        with self.assertRaises(access.AccessDenied):
            self.check()

    def test_pending_inherited_unknown_or_nonmember_denies(self):
        base = "orgs/nf-core/teams/team-a"
        for member in (
            {**self.alice, "inherited": True},
            {k: v for k, v in self.alice.items() if k != "inherited"},
        ):
            with self.subTest(member=member):
                self.responses[f"{base}/members?per_page=100&page=1"] = [member]
                with self.assertRaises(access.AccessDenied):
                    self.check()
        self.responses[f"{base}/members?per_page=100&page=1"] = [self.alice]
        for membership in ({"state": "pending"}, None):
            self.responses[f"{base}/memberships/alice"] = membership
            with (
                self.subTest(membership=membership),
                self.assertRaises(access.AccessDenied),
            ):
                self.check()

    def test_paginated_direct_membership(self):
        base = "orgs/nf-core/teams/team-a/members?per_page=100&page="
        self.responses[base + "1"] = [self.bob] * 100
        self.responses[base + "2"] = [self.alice]
        self.check()

    def test_unapproved_rerunner_bot_and_identity_change_deny(self):
        self.context["GITHUB_TRIGGERING_ACTOR"] = "bob"
        self.responses["orgs/nf-core/teams/team-b/memberships/bob"] = None
        with self.assertRaises(access.AccessDenied):
            self.check()
        self.context["GITHUB_TRIGGERING_ACTOR"] = "github-actions[bot]"
        with self.assertRaises(access.AccessDenied):
            self.check()
        self.context["GITHUB_TRIGGERING_ACTOR"] = "alice"
        self.responses["users/alice"] = {**self.alice, "type": "Bot"}
        with self.assertRaises(access.AccessDenied):
            self.check()
        self.responses["users/alice"] = {**self.alice, "id": 999}
        with self.assertRaises(access.AccessDenied):
            self.check()

    def test_renamed_or_recreated_team_denies(self):
        team = self.responses["orgs/nf-core/teams/team-b"]
        team["id"] = 999
        with self.assertRaises(access.AccessDenied):
            self.check()

    def test_native_reviewer_inheritance_cannot_expand_approved_teams(self):
        path = "orgs/nf-core/teams/team-a/teams?per_page=100"
        self.responses[path] = [{"id": 202}]
        self.check()  # The other approved team is safe as a child.
        self.responses[path] = [{"id": 202}, {"id": 303}]
        with self.assertRaisesRegex(access.AccessDenied, "unapproved child teams"):
            self.check()

    def test_weakened_or_expanded_environment_denies(self):
        path = f"repos/{access.REPOSITORY}/environments/plugin-release"
        original = copy.deepcopy(self.responses[path])
        for change in (
            {"can_admins_bypass": True},
            {"can_admins_bypass": None},
            {"protection_rules": []},
            {"deployment_branch_policy": None},
        ):
            self.responses[path] = {**original, **change}
            with self.subTest(change=change), self.assertRaises(access.AccessDenied):
                self.check()
        self.responses[path] = original
        rule = original["protection_rules"][0]
        rule["prevent_self_review"] = False
        with self.assertRaises(access.AccessDenied):
            self.check()
        rule["prevent_self_review"] = True
        rule["reviewers"][1]["reviewer"]["id"] = 303
        with self.assertRaises(access.AccessDenied):
            self.check()
        rule["reviewers"][1]["reviewer"]["id"] = 202
        branches = self.responses[path + "/deployment-branch-policies"]
        for branch in (
            {"name": "*", "type": "branch"},
            {"name": "main", "type": "tag"},
        ):
            branches["branch_policies"] = [branch]
            with self.subTest(branch=branch), self.assertRaises(access.AccessDenied):
                self.check()

    def test_missing_membership_credential_never_falls_back_to_repo_token(self):
        self.context["RELEASE_MEMBERS_READ_TOKEN"] = ""
        with self.assertRaises(access.AccessDenied):
            self.check()

    def test_http_errors_deny_and_only_membership_404_is_absence(self):
        for status in (401, 403, 404, 429, 500):
            error = urllib.error.HTTPError(
                "https://api.github.com/test", status, "private response", {}, None
            )
            with (
                self.subTest(status=status),
                patch("urllib.request.urlopen", side_effect=error),
            ):
                with self.assertRaisesRegex(access.AccessDenied, f"HTTP {status}"):
                    access.api_get("test", "synthetic")
                if status == 404:
                    self.assertIsNone(
                        access.api_get("test", "synthetic", missing_ok=True)
                    )
        with (
            patch("urllib.request.urlopen", return_value=io.StringIO("not-json")),
            self.assertRaises(json.JSONDecodeError),
        ):
            access.api_get("test", "synthetic")


if __name__ == "__main__":
    unittest.main()
