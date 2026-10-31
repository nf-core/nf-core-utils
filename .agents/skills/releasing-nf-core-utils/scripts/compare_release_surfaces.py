#!/usr/bin/env -S uv run --script
# /// script
# dependencies = []
# ///
"""Compare nf-core-utils versions on GitHub Releases and the Nextflow Plugin Registry."""

import json
import subprocess
import sys
import urllib.error
import urllib.request

PLUGIN = "nf-core-utils"
REGISTRY_URL = f"https://registry.nextflow.io/api/v1/plugins/{PLUGIN}"


def main():
    try:
        github_versions = set(
            subprocess.check_output(
                [
                    "gh",
                    "api",
                    "--paginate",
                    "repos/nf-core/nf-core-utils/releases?per_page=100",
                    "--jq",
                    ".[] | select(.draft == false) | .tag_name",
                ],
                text=True,
            ).splitlines()
        )
        with urllib.request.urlopen(REGISTRY_URL, timeout=30) as response:
            releases = json.load(response)["plugin"]["releases"]
        registry_versions = {
            release["version"]
            for release in releases
            if release["status"] == "PUBLISHED"
        }
        unpublished = {
            release["version"]
            for release in releases
            if release["status"] != "PUBLISHED"
        }
    except (
        OSError,
        subprocess.CalledProcessError,
        urllib.error.URLError,
        ValueError,
        KeyError,
        TypeError,
    ) as error:
        print(
            f"Release comparison failed ({type(error).__name__}); check GitHub/registry access and response schema.",
            file=sys.stderr,
        )
        return 1

    for label, versions in (
        ("GitHub", github_versions),
        ("Registry (PUBLISHED)", registry_versions),
        ("Registry (not PUBLISHED)", unpublished),
        ("Missing from registry", github_versions - registry_versions),
        ("Missing from GitHub", registry_versions - github_versions),
    ):
        print(f"{label}: {', '.join(sorted(versions)) or 'none'}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
