#!/usr/bin/env python3
"""Guard validation nf-test snapshots from accidental staged deletion."""

from __future__ import annotations

import os
import subprocess
import sys
from pathlib import PurePosixPath


def staged_deleted_files() -> list[str]:
    result = subprocess.run(
        ["git", "diff", "--cached", "--name-only", "--diff-filter=D", "--", "validation/"],
        check=False,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    if result.returncode != 0:
        print(result.stderr, file=sys.stderr, end="")
        raise SystemExit(result.returncode)
    return [line for line in result.stdout.splitlines() if line]


def is_validation_nf_test_snapshot(path: str) -> bool:
    p = PurePosixPath(path)
    return (
        len(p.parts) >= 2
        and p.parts[0] == "validation"
        and (path.endswith(".nf.test.snap") or path.endswith(".snap"))
    )


def main() -> int:
    if os.environ.get("ALLOW_VALIDATION_SNAPSHOT_DELETE") == "1":
        return 0

    deleted_snapshots = [p for p in staged_deleted_files() if is_validation_nf_test_snapshot(p)]
    if not deleted_snapshots:
        return 0

    print(
        """Refusing to commit deleted validation nf-test snapshots.

These files are regression baselines for the validation harness. Agent-assisted
changes should update or regenerate snapshots intentionally, not silently remove
them and let nf-test recreate empty/unreviewed baselines later.

Deleted snapshots:""",
        file=sys.stderr,
    )
    for path in deleted_snapshots:
        print(path, file=sys.stderr)
    print(
        """
If this deletion is intentional, rerun the commit with:
  ALLOW_VALIDATION_SNAPSHOT_DELETE=1 git commit ...""",
        file=sys.stderr,
    )
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
