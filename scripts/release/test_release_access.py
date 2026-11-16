"""Offline contract: python3 -m unittest discover -s scripts/release -v."""

import copy
import io
import json
import unittest
import urllib.error
from unittest.mock import patch

import check_release_access as access

ENV_PATH = f"repos/{access.REPOSITORY}/environments/plugin-release"


class ReleaseAccessTest(unittest.TestCase):
    def setUp(self):
        self.context = {
            "GITHUB_REPOSITORY": access.REPOSITORY,
            "GITHUB_REF": "refs/heads/main",
            "GITHUB_EVENT_NAME": "workflow_dispatch",
            "GITHUB_WORKFLOW_REF": f"{access.REPOSITORY}/.github/workflows/release.yml@refs/heads/main",
            "GITHUB_SHA": "a" * 40,
            "EXPECTED_SHA": "a" * 40,
            "GH_TOKEN": "synthetic-repo-token",
        }
        # Reviewer identities are deliberately arbitrary: the check must not care who they are.
        self.responses = {
            ENV_PATH: {
                "can_admins_bypass": False,
                "deployment_branch_policy": {
                    "protected_branches": False,
                    "custom_branch_policies": True,
                },
                "protection_rules": [
                    {
                        "type": "required_reviewers",
                        "prevent_self_review": True,
                        "reviewers": [{"type": "User", "reviewer": {"id": 7}}],
                    }
                ],
            },
            ENV_PATH
            + "/deployment-branch-policies": {
                "total_count": 1,
                "branch_policies": [{"name": "main", "type": "branch"}],
            },
        }

    def get(self, path, token):
        self.assertEqual(token, "synthetic-repo-token")
        response = self.responses[path]
        if isinstance(response, Exception):
            raise response
        return copy.deepcopy(response)

    def check(self):
        with patch.object(access, "api_get", self.get):
            access.authorize(self.context)

    def test_reviewed_main_only_environment_passes_for_any_reviewers(self):
        self.check()
        self.context["EXPECTED_SHA"] = ""  # Existing manual version-only dispatch.
        self.check()
        rule = self.responses[ENV_PATH]["protection_rules"][0]
        rule["reviewers"] = [
            {"type": "Team", "reviewer": {"id": 1}},
            {"type": "Team", "reviewer": {"id": 2}},
            {"type": "User", "reviewer": {"id": 3}},
        ]
        self.check()

    def test_expected_sha_mismatch_is_denied_before_any_lookup(self):
        self.responses = {}
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

    def test_missing_token_is_denied(self):
        self.context["GH_TOKEN"] = ""
        with self.assertRaises(access.AccessDenied):
            self.check()

    def test_missing_environment_denies(self):
        # GitHub would otherwise auto-create an unprotected environment.
        self.responses[ENV_PATH] = access.AccessDenied("GitHub lookup failed (HTTP 404)")
        with self.assertRaisesRegex(access.AccessDenied, "404"):
            self.check()

    def test_environment_without_required_reviewers_denies(self):
        rules = self.responses[ENV_PATH]["protection_rules"]
        for protection_rules in (
            [],
            [{"type": "wait_timer"}],
            [{**rules[0], "reviewers": []}],
            rules + rules,
        ):
            self.responses[ENV_PATH]["protection_rules"] = protection_rules
            with self.subTest(rules=protection_rules), self.assertRaises(
                access.AccessDenied
            ):
                self.check()

    def test_self_review_allowed_denies(self):
        rule = self.responses[ENV_PATH]["protection_rules"][0]
        for value in (False, None):
            rule["prevent_self_review"] = value
            with self.subTest(value=value), self.assertRaises(access.AccessDenied):
                self.check()

    def test_admin_bypass_denies(self):
        for value in (True, None):
            self.responses[ENV_PATH]["can_admins_bypass"] = value
            with self.subTest(value=value), self.assertRaises(access.AccessDenied):
                self.check()

    def test_branch_policy_not_main_only_denies(self):
        self.responses[ENV_PATH]["deployment_branch_policy"] = None
        with self.assertRaises(access.AccessDenied):
            self.check()
        self.responses[ENV_PATH]["deployment_branch_policy"] = {
            "protected_branches": True,
            "custom_branch_policies": False,
        }
        with self.assertRaises(access.AccessDenied):
            self.check()
        self.responses[ENV_PATH]["deployment_branch_policy"] = {
            "protected_branches": False,
            "custom_branch_policies": True,
        }
        branches = self.responses[ENV_PATH + "/deployment-branch-policies"]
        for policies in (
            [{"name": "*", "type": "branch"}],
            [{"name": "main", "type": "tag"}],
            [{"name": "main", "type": "branch"}, {"name": "dev", "type": "branch"}],
        ):
            branches["branch_policies"] = policies
            branches["total_count"] = len(policies)
            with self.subTest(policies=policies), self.assertRaises(access.AccessDenied):
                self.check()

    def test_http_errors_deny(self):
        for status in (401, 403, 404, 429, 500):
            error = urllib.error.HTTPError(
                "https://api.github.com/test", status, "private response", {}, None
            )
            with (
                self.subTest(status=status),
                patch("urllib.request.urlopen", side_effect=error),
                self.assertRaisesRegex(access.AccessDenied, f"HTTP {status}"),
            ):
                access.api_get("test", "synthetic")
        with (
            patch("urllib.request.urlopen", return_value=io.StringIO("not-json")),
            self.assertRaises(json.JSONDecodeError),
        ):
            access.api_get("test", "synthetic")


if __name__ == "__main__":
    unittest.main()
