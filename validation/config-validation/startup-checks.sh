#!/usr/bin/env bash
# Real CLI regression for https://github.com/nf-core/nf-core-utils/issues/73.
# nf-test supplies extra config files, which would mask the missing-config warning.
set -euo pipefail

export NXF_PLUGINS_DIR="${NXF_PLUGINS_DIR:-${NXF_HOME:-$HOME/.nextflow}/plugins}"
export NXF_AGENT_MODE=false NXF_ANSI_LOG=false
scratch=$(mktemp -d)
trap 'rm -rf "$scratch"' EXIT
# Not even an empty home config is allowed: it counts as a custom config.
export NXF_HOME="$scratch/home"
mkdir -p "$NXF_HOME"

cat > "$scratch/nextflow.config" <<'EOF'
plugins { id 'nf-core-utils@0.6.0' }
params.color = false
profiles {
    docker { docker.enabled = false }
    test { }
}
EOF
touch "$scratch/custom.config"
cat > "$scratch/explicit.nf" <<'EOF'
include { checkConfigProvided; checkProfileProvided } from 'plugin/nf-core-utils'
workflow {
    checkConfigProvided()
    checkProfileProvided(args, !params.color)
    println 'STARTUP_CHECKS_DONE'
}
EOF
cat > "$scratch/observer.nf" <<'EOF'
workflow { println 'STARTUP_CHECKS_DONE' }
EOF
# Representative template-local checks, independent of exported plugin functions.
cat > "$scratch/local.nf" <<'EOF'
def checkConfigProvided() {
    if (workflow.profile == 'standard' && workflow.configFiles.size() <= 1)
        log.warn 'You are attempting to run the pipeline without any custom configuration!'
}
def checkProfileProvided(args) {
    if (workflow.profile.endsWith(','))
        error 'ERROR ~ The `-profile` option cannot end with a trailing comma'
    if (args)
        log.warn "nf-core pipelines do not accept positional arguments. The positional argument `${args[0]}` has been detected."
}
workflow {
    checkConfigProvided()
    checkProfileProvided(args)
    println 'STARTUP_CHECKS_DONE'
}
EOF
# rnaseq-style migration: exported config check, template-local profile check.
{
    echo "include { checkConfigProvided } from 'plugin/nf-core-utils'"
    sed '/^def checkConfigProvided()/,/^}/d' "$scratch/local.nf"
} > "$scratch/mixed.nf"

failures=0
check() {
    local name=$1 script=$2 expected_status=$3 expected_config=$4 expected_args=$5 expected_error=$6
    shift 6
    local output="$scratch/$name.out" status=0
    # Isolate both launchDir and projectDir; no user/repository config can mask warnings.
    (cd "$scratch" && nextflow run "$script.nf" "$@") > "$output" 2>&1 || status=$?
    local config_count args_count error_count done_count
    config_count=$(grep -c 'without any custom configuration' "$output" || true)
    args_count=$(grep -c 'nf-core pipelines do not accept positional arguments' "$output" || true)
    error_count=$( (grep -o 'ERROR' "$output" || true) | wc -l | tr -d ' ')
    done_count=$(grep -c '^STARTUP_CHECKS_DONE$' "$output" || true)
    printf '%s: exit=%s config=%s positional=%s ERROR=%s done=%s\n' \
        "$name" "$status" "$config_count" "$args_count" "$error_count" "$done_count"
    if [[ "$status" != "$expected_status" || "$config_count" != "$expected_config" ||
          "$args_count" != "$expected_args" || "$error_count" != "$expected_error" ||
          "$done_count" != "$((expected_status == 0 ? 1 : 0))" ]] ||
       { [[ "$expected_error" == 1 ]] && ! grep -q 'cannot end with a trailing comma' "$output"; } ||
       { [[ "$expected_error" == 0 ]] && grep -q 'cannot end with a trailing comma' "$scratch/.nextflow.log"; }; then
        cat "$output"
        grep 'cannot end with a trailing comma' "$scratch/.nextflow.log" || true
        failures=$((failures + 1))
    fi
}

for caller in explicit local mixed; do
    check "$caller-default" "$caller" 0 1 0 0
    check "$caller-positional" "$caller" 0 1 1 0 extra
    check "$caller-comma" "$caller" 1 0 0 1 -profile docker,
done
check explicit-color explicit 1 0 0 1 -profile docker, --color true
check explicit-profile explicit 0 0 0 0 -profile test,docker
check explicit-custom-config explicit 0 0 0 0 -c "$scratch/custom.config"
check observer-default observer 0 0 0 0
check observer-positional observer 0 0 0 0 extra
check observer-comma observer 0 0 0 0 -profile docker,
if (( failures )); then
    echo "FAIL: $failures startup validation cases" >&2
    exit 1
fi
echo 'PASS: 15 startup validation cases'
