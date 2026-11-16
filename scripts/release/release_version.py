#!/usr/bin/env python3
"""Validate a release version and print `prerelease=true|false` for $GITHUB_OUTPUT.

Accepts a stable X.Y.Z or a release candidate X.Y.Z-rc.N (no v prefix, no leading zeros).
"""

import re
import sys

NUMBER = r"(?:0|[1-9][0-9]*)"
VERSION = re.compile(rf"{NUMBER}\.{NUMBER}\.{NUMBER}(-rc\.[1-9][0-9]*)?")


def is_prerelease(version):
    """Return whether the version is a release candidate; raise ValueError if unsupported."""
    # fullmatch, not `$`: `$` would accept a trailing newline.
    match = VERSION.fullmatch(version)
    if not match:
        raise ValueError(
            "Expected a stable version such as 0.7.0 or a release candidate such as 1.0.0-rc.1"
        )
    return match.group(1) is not None


def main(argv):
    if len(argv) != 2:
        print("usage: release_version.py <version>", file=sys.stderr)
        return 2
    try:
        prerelease = is_prerelease(argv[1])
    except ValueError as error:
        print(f"::error::{error}", file=sys.stderr)
        return 1
    print(f"prerelease={str(prerelease).lower()}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
