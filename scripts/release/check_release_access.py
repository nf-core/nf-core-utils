#!/usr/bin/env -S uv run --script
# /// script
# dependencies = []
# ///
"""Read-only, fail-closed release authorization. Never publishes or changes policy."""

import json
import os
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

REPOSITORY = "nf-core/nf-core-utils"
ORGANIZATION = "nf-core"
ENVIRONMENT = "plugin-release"


class AccessDenied(Exception):
    pass


def api_get(path, token, *, missing_ok=False):
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
        if error.code == 404 and missing_ok:
            return None
        raise AccessDenied(f"GitHub lookup failed (HTTP {error.code})") from None


def authorize(context, policy):
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

    if policy.get("approved") is not True:
        raise AccessDenied("Release access policy has not been approved")
    teams = policy["teams"]
    if (
        len(teams) != 2
        or {team["role"] for team in teams} != {"core", "infrastructure"}
        or len({team["id"] for team in teams}) != 2
        or len({team["slug"] for team in teams}) != 2
        or any(
            type(team["id"]) is not int
            or team["id"] <= 0
            or not re.fullmatch(r"[a-z0-9]+(?:-[a-z0-9]+)*", team["slug"])
            for team in teams
        )
    ):
        raise AccessDenied(
            "Policy needs exactly two verified, distinct team IDs and slugs"
        )

    repo_token = context.get("GH_TOKEN", "")
    members_token = context.get("RELEASE_MEMBERS_READ_TOKEN", "")
    if not repo_token or not members_token:
        raise AccessDenied(
            "Read-only repository and organization membership credentials are required"
        )

    # Confirm the environment exists and is protected BEFORE referencing it in a
    # write-capable job. A missing environment would otherwise be auto-created.
    environment = api_get(f"repos/{REPOSITORY}/environments/{ENVIRONMENT}", repo_token)
    reviewers = [
        rule
        for rule in environment["protection_rules"]
        if rule["type"] == "required_reviewers"
    ]
    if (
        environment.get("can_admins_bypass") is not False
        or len(reviewers) != 1
        or reviewers[0].get("prevent_self_review") is not True
        or len(reviewers[0]["reviewers"]) != 2
        or any(reviewer["type"] != "Team" for reviewer in reviewers[0]["reviewers"])
        or {reviewer["reviewer"]["id"] for reviewer in reviewers[0]["reviewers"]}
        != {team["id"] for team in teams}
        or environment.get("deployment_branch_policy")
        != {"protected_branches": False, "custom_branch_policies": True}
    ):
        raise AccessDenied(
            "Protected release environment does not match approved policy"
        )
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

    for team in teams:
        actual = api_get(f"orgs/{ORGANIZATION}/teams/{team['slug']}", members_token)
        if (
            actual["id"] != team["id"]
            or actual["slug"] != team["slug"]
            or actual["organization"]["login"] != ORGANIZATION
        ):
            raise AccessDenied("Team identity no longer matches approved policy")
        # Native environment team reviewers include child-team members. Deny
        # hierarchy expansion beyond the two approved teams, including for approvers.
        children = api_get(
            f"orgs/{ORGANIZATION}/teams/{team['slug']}/teams?per_page=100",
            members_token,
        )
        if not isinstance(children, list) or any(
            child["id"] not in {approved["id"] for approved in teams}
            for child in children
        ):
            raise AccessDenied(
                "Reviewer team hierarchy includes unapproved child teams"
            )

    actors = {
        context.get("GITHUB_ACTOR", ""),
        context.get("GITHUB_TRIGGERING_ACTOR", ""),
    }
    for actor in actors:
        if not re.fullmatch(r"[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,37}[a-zA-Z0-9])?", actor):
            raise AccessDenied(
                "Missing or unsupported release actor; bots are not implicitly allowed"
            )
        user = api_get(f"users/{actor}", repo_token)
        if user["type"] != "User" or user["login"].lower() != actor.lower():
            raise AccessDenied(
                "Only approved team users may initiate or rerun releases"
            )
        if actor == context["GITHUB_ACTOR"] and str(user["id"]) != context.get(
            "GITHUB_ACTOR_ID"
        ):
            raise AccessDenied("Original actor ID does not match GitHub")

        allowed = False
        for team in teams:
            base = f"orgs/{ORGANIZATION}/teams/{team['slug']}"
            membership = api_get(
                f"{base}/memberships/{actor}", members_token, missing_ok=True
            )
            if membership is None or membership["state"] != "active":
                continue
            page = 1
            while True:
                members = api_get(
                    f"{base}/members?per_page=100&page={page}", members_token
                )
                if not isinstance(members, list):
                    raise AccessDenied("Invalid team membership response")
                if any(
                    member["id"] == user["id"]
                    and member["login"].lower() == actor.lower()
                    and member["type"] == "User"
                    and member.get("inherited") is False
                    for member in members
                ):
                    allowed = True
                    break
                if len(members) < 100:
                    break
                page += 1
            if allowed:
                break
        if not allowed:
            raise AccessDenied(
                "Both original and triggering actors need active, direct approved-team membership"
            )


def main():
    try:
        policy = json.loads(Path(sys.argv[1]).read_text())
        authorize(os.environ, policy)
    except AccessDenied as error:
        print(f"Release access denied: {error}", file=sys.stderr)
        return 1
    except (OSError, ValueError, KeyError, TypeError, IndexError, AttributeError):
        # Do not echo credentials, API response bodies, or malformed policy values.
        print(
            "Release access denied: unavailable or invalid policy/API evidence",
            file=sys.stderr,
        )
        return 1
    print("Release access authorized for this attempt; publication checks still apply")
    return 0


if __name__ == "__main__":
    sys.exit(main())
