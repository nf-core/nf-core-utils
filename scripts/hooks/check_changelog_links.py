#!/usr/bin/env python3
"""Check CHANGELOG.md release links follow Keep a Changelog 1.1.0.

Every `## [version]` heading needs a matching `[version]: url` definition, in
heading order, comparing it with the release below it:

    [unreleased]: <repo>/compare/<latest>...HEAD
    [X.Y.Z]: <repo>/compare/<previous>...X.Y.Z
    [<oldest>]: <repo>/releases/tag/<oldest>

Tags have no `v` prefix. Other definitions, such as `[@user]`, are ignored.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = "https://github.com/nf-core/nf-core-utils"
HEADING = re.compile(r"^## \[([^\]]+)\]", re.M)
DEFINITION = re.compile(r"^\[([^\]]+)\]: (\S+)\s*$", re.M)
VERSION = re.compile(r"^(unreleased|\d+\.\d+\.\d+(-[0-9A-Za-z.-]+)?)$", re.I)


def expected_links(versions: list[str]) -> list[tuple[str, str]]:
    links = []
    for i, version in enumerate(versions):
        older = versions[i + 1] if i + 1 < len(versions) else None
        if version.lower() == "unreleased":
            url = f"{REPO}/compare/{older}...HEAD" if older else f"{REPO}/commits/HEAD"
            links.append(("unreleased", url))
        elif older:
            links.append((version, f"{REPO}/compare/{older}...{version}"))
        else:
            links.append((version, f"{REPO}/releases/tag/{version}"))
    return links


def main(path: str = "CHANGELOG.md") -> int:
    text = Path(path).read_text()
    versions = HEADING.findall(text)
    expected = expected_links(versions)
    actual = [(label.lower() if label.lower() == "unreleased" else label, url)
              for label, url in DEFINITION.findall(text) if VERSION.match(label)]
    if actual == expected:
        return 0

    print(f"{path}: release links do not follow Keep a Changelog 1.1.0. Expected, in this order:\n",
          file=sys.stderr)
    for label, url in expected:
        print(f"[{label}]: {url}", file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main(*sys.argv[1:]))
