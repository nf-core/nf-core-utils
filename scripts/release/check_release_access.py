#!/usr/bin/env -S uv run --script
# /// script
# dependencies = []
# ///
"""Read-only, fail-closed check that the release environment still requires review."""

import json
import os
import re
import sys
import urllib.error
import urllib.request

REPOSITORY = "nf-core/nf-core-utils"
ENVIRONMENT = "plugin-release"


class AccessDenied(Exception):
    pass


def api_get(path, token):
    request = urllib.request.Request(
        f"https://api.github.com/{path}",
        headers={
            "Authorization": f"Bearer {token}",
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2026-03-10",
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        raise AccessDenied(f"GitHub lookup failed (HTTP {error.code})") from None


def authorize(context):
    if (
        context.get("GITHUB_REPOSITORY") != REPOSITORY
        or context.get("GITHUB_REF") != "refs/heads/main"
        or context.get("GITHUB_EVENT_NAME") != "workflow_dispatch"
        or context.get("GITHUB_WORKFLOW_REF")
        != f"{REPOSITORY}/.github/workflows/release.yml@refs/heads/main"
    ):
        raise AccessDenied(
            "Only release.yml dispatched on this repository's main is allowed"
        )

    sha = context.get("GITHUB_SHA", "")
    expected = context.get("EXPECTED_SHA", "")
    if not re.fullmatch(r"[0-9a-f]{40}", sha):
        raise AccessDenied("Missing release commit")
    if expected and (
        not re.fullmatch(r"[0-9a-fA-F]{40}", expected) or expected.lower() != sha
    ):
        raise AccessDenied("expected_sha must be the full selected release commit")

    repo_token = context.get("GH_TOKEN", "")
    if not repo_token:
        raise AccessDenied("The workflow's read-only repository token is required")

    # Confirm the environment exists and is protected BEFORE referencing it in a
    # write-capable job. A missing environment would otherwise be auto-created.
    environment = api_get(f"repos/{REPOSITORY}/environments/{ENVIRONMENT}", repo_token)
    reviewers = [
        rule
        for rule in environment["protection_rules"]
        if rule["type"] == "required_reviewers"
    ]
    # Who the reviewers are is the environment's business; only require that some exist.
    if (
        environment.get("can_admins_bypass") is not False
        or len(reviewers) != 1
        or reviewers[0].get("prevent_self_review") is not True
        or not reviewers[0]["reviewers"]
        or environment.get("deployment_branch_policy")
        != {"protected_branches": False, "custom_branch_policies": True}
    ):
        raise AccessDenied("Release environment must require review by its reviewers")
    branches = api_get(
        f"repos/{REPOSITORY}/environments/{ENVIRONMENT}/deployment-branch-policies",
        repo_token,
    )
    if branches["total_count"] != 1 or [
        (branch["name"], branch["type"]) for branch in branches["branch_policies"]
    ] != [("main", "branch")]:
        raise AccessDenied(
            "Release environment must allow only the main branch, not tags"
        )


def main():
    try:
        authorize(os.environ)
    except AccessDenied as error:
        print(f"Release access denied: {error}", file=sys.stderr)
        return 1
    except (OSError, ValueError, KeyError, TypeError, IndexError, AttributeError):
        # Do not echo credentials or API response bodies.
        print(
            "Release access denied: unavailable or invalid environment evidence",
            file=sys.stderr,
        )
        return 1
    print("Release environment requires review; publication checks still apply")
    return 0


if __name__ == "__main__":
    sys.exit(main())
