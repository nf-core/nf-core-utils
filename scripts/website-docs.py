#!/usr/bin/env python3
# /// script
# requires-python = ">=3.12"
# dependencies = ["pyyaml"]
# ///
"""Write the user docs as pages of the nf-core website (https://nf-co.re/docs/nf-core-tools/nf-core-utils).

On each release, .github/workflows/website-docs.yml asks the website to run this script from the root of this
repository, checked out inside the website repository, as nf-core/tools does for its API docs:

    uv run scripts/website-docs.py --release 0.6.0 [--output <dir>]
"""

import argparse
from pathlib import Path

import yaml

DOCS = Path(__file__).resolve().parent.parent / "docs"
# The user docs, the other files in docs/ are for the developers of the plugin
USER_DOCS = ["NfCoreUtilities.md", "NextflowPipelineExtension.md", "ReferencesExtension.md", "utilities/*.md"]


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("-r", "--release", required=True, help="the release of the docs, e.g. 0.6.0")
    parser.add_argument(
        "-o",
        "--output",
        type=Path,
        default=Path("../sites/docs/src/content/docs/nf-core-tools/nf-core-utils"),
        help="the nf-core-utils folder of the website docs (default: %(default)s)",
    )
    args = parser.parse_args()

    # docs removed here are removed from the website too
    args.output.mkdir(parents=True, exist_ok=True)
    for page in args.output.rglob("*.md"):
        page.unlink()

    docs = [path for pattern in USER_DOCS for path in sorted(DOCS.glob(pattern))]
    for weight, path in enumerate(docs, start=1):
        doc = path.relative_to(DOCS).as_posix()
        text = path.read_text()
        # the frontmatter of the docs pages: the first heading as title and label in the sidebar, and the place in the
        # list as weight
        title = next(line[2:].strip() for line in text.splitlines() if line.startswith("# "))
        frontmatter = {"title": title, "shortTitle": title, "weight": weight}
        # in the nf-core/tools section, nf-core-utils comes after the CLI and API docs
        if "/" not in doc:
            frontmatter["parentWeight"] = 3
        # "Edit this page" opens the doc here, as changes to the website copy are overwritten
        frontmatter["md_github_url"] = f"https://github.com/nf-core/nf-core-utils/blob/main/docs/{doc}"

        page = args.output / doc
        page.parent.mkdir(parents=True, exist_ok=True)
        page.write_text(
            "---\n"
            f"# Copied from docs/{doc} of nf-core/nf-core-utils {args.release}"
            " by .github/workflows/add-nf-core-utils-docs.yml: edit it there\n"
            + yaml.safe_dump(frontmatter, sort_keys=False, allow_unicode=True, width=float("inf"))
            + "---\n\n"
            + text
        )


if __name__ == "__main__":
    main()
