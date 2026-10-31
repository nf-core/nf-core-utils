#!/usr/bin/env bash
# Check the default (all three tools execute) run using Spock's native reporting.
set -eu
outdir=$(realpath "${1:?Usage: bash check.sh OUTPUT_DIRECTORY}")
repo_dir=$(realpath "$(dirname "${BASH_SOURCE[0]}")/../..")
NF_CORE_REMOTE_CITATIONS_OUTDIR="$outdir" "$repo_dir/gradlew" --project-dir "$repo_dir" \
    test --tests 'nfcore.plugin.nfcore.RegistryModuleCitationsTest' --rerun-tasks --console=plain
