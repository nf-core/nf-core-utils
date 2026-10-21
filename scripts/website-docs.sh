#!/bin/bash
# Write the user docs as pages of the nf-core website (https://nf-co.re/docs/nf-core-tools/nf-core-utils).
# On each release, .github/workflows/website-docs.yml asks the website to run this script from the root of this
# repository, checked out inside the website repository, as nf-core/tools does for its API docs:
#   bash scripts/website-docs.sh --release 0.6.0 [--output <dir>]
set -euo pipefail

release=""
output_dir="../sites/docs/src/content/docs/nf-core-tools/nf-core-utils"
while [[ $# -gt 0 ]]; do
    case $1 in
        -r | --release )
            shift
            release="$1"
            ;;
        -o | --output )
            shift
            output_dir="$1"
            ;;
        * )
            echo "Invalid argument: $1"
            exit 1
            ;;
    esac
    shift
done
if [[ -z "$release" ]]; then
    echo "Missing --release"
    exit 1
fi

# The user docs, the other files in docs/ are for the developers of the plugin. Each doc gets the frontmatter of the
# docs pages: its first heading as title and label in the sidebar, and its place in the list as weight. In the
# nf-core/tools section, nf-core-utils comes after the CLI and API docs (parentWeight). "Edit this page" links to the
# doc here (md_github_url), as changes to the website copy are overwritten.
mkdir -p "$output_dir"
target=$(cd "$output_dir" && pwd)
# docs removed here are removed from the website too
find "$target" -name "*.md" -delete
cd "$(dirname "$0")/../docs"
weight=0
for file in NfCoreUtilities.md NextflowPipelineExtension.md ReferencesExtension.md utilities/*.md; do
    weight=$((weight + 1))
    title=$(grep -m 1 '^# ' "$file" | sed -e 's/^# *//' -e 's/\\/\\\\/g' -e 's/"/\\"/g')
    mkdir -p "$target/$(dirname "$file")"
    {
        echo "---"
        echo "# Copied from docs/$file of nf-core/nf-core-utils $release by .github/workflows/add-nf-core-utils-docs.yml: edit it there"
        echo "title: \"$title\""
        echo "shortTitle: \"$title\""
        echo "weight: $weight"
        case "$file" in */*) ;; *) echo "parentWeight: 3" ;; esac
        echo "md_github_url: https://github.com/nf-core/nf-core-utils/blob/main/docs/$file"
        echo "---"
        echo
        cat "$file"
    } > "$target/$file"
done
