#!/usr/bin/env bash
# Real strict-syntax CLI regression for https://github.com/nf-core/nf-core-utils/issues/94.
# No recipients or mail transport: verify dispatch and reports without sending email.
set -euo pipefail

export NXF_PLUGINS_DIR="${NXF_PLUGINS_DIR:-${NXF_HOME:-$HOME/.nextflow}/plugins}"
export NXF_SYNTAX_PARSER=v2 NXF_AGENT_MODE=false NXF_ANSI_LOG=false
unset AGENT CLAUDECODE
scratch=$(mktemp -d)
trap 'rm -rf "$scratch"' EXIT

# Explicit config excludes personal config (including external monitoring plugins).
cat > "$scratch/nextflow.config" <<'EOF'
plugins { id 'nf-core-utils@0.7.0' }
manifest.name = 'nf-core/boolean-params'
tower.enabled = false
params.monochrome_logs = false
params.plaintext_email = false
params.mode = 'all'
EOF
mkdir -p "$scratch/assets"
printf 'Plain CLI summary' > "$scratch/assets/email_template.txt"
printf '<p>HTML CLI summary</p>' > "$scratch/assets/email_template.html"
cat > "$scratch/main.nf" <<'EOF'
include { checkProfileProvided; completionSummary; completionEmail } from 'plugin/nf-core-utils'
workflow {
    main:
    def monochrome = params.monochrome_logs
    def plaintext = params.plaintext_email
    log.info "UNTYPED_FLAGS=${monochrome instanceof String},${plaintext instanceof String}"
    if (params.mode != 'completion-only') {
        checkProfileProvided([], monochrome)
    }
    workflow.onComplete {
        completionSummary(monochrome)
        completionEmail([:], null, null, plaintext, 'results', monochrome, [])
        log.info 'COMPLETION_DONE'
    }
}
EOF

check() {
    local name=$1 expected_status=$2 coloured=$3 warnings=$4
    shift 4
    local dir="$scratch/$name" status=0
    mkdir -p "$dir"
    (cd "$dir" && nextflow -C "$scratch/nextflow.config" run "$scratch/main.nf" "$@") > "$dir/output" 2>&1 || status=$?
    printf '%s: exit=%s\n' "$name" "$status"
    if [[ "$status" != "$expected_status" ]] || ! grep -q 'UNTYPED_FLAGS=true,true' "$dir/output"; then
        cat "$dir/output"
        return 1
    fi
    if [[ "$expected_status" != 0 ]]; then
        grep -q 'Expected monochromeLogs to be true or false' "$dir/output" || { cat "$dir/output"; return 1; }
        return
    fi
    # Nextflow can exit zero despite a failed onComplete handler: require its final marker and artifacts.
    if ! grep -q 'COMPLETION_DONE' "$dir/output" ||
       ! grep -q 'Pipeline completed successfully' "$dir/output" ||
       [[ "$(< "$dir/results/pipeline_info/pipeline_report.txt")" != 'Plain CLI summary' ]] ||
       [[ "$(< "$dir/results/pipeline_info/pipeline_report.html")" != '<p>HTML CLI summary</p>' ]]; then
        cat "$dir/output"
        grep -A30 'Failed to invoke' "$dir/.nextflow.log" || true
        return 1
    fi
    local summary
    summary=$(grep 'Pipeline completed successfully' "$dir/output")
    if { [[ "$coloured" == true ]] && [[ "$summary" != *$'\033['* ]]; } ||
       { [[ "$coloured" == false ]] && [[ "$summary" == *$'\033['* ]]; }; then
        cat "$dir/output"
        return 1
    fi
    if [[ "$warnings" == true ]]; then
        grep -q 'Expected plaintextEmail.*using false' "$dir/output" || { cat "$dir/output"; return 1; }
        grep -q 'Expected monochromeLogs.*using true' "$dir/output" || { cat "$dir/output"; return 1; }
    elif grep -q 'Expected .* to be true or false' "$dir/output"; then
        cat "$dir/output"
        return 1
    fi
}

check bare-flags 0 false false --monochrome_logs --plaintext_email
check explicit-true 0 false false --monochrome_logs true --plaintext_email true
check false-strings 0 true false --monochrome_logs false --plaintext_email false
check mixed-case 0 true false --monochrome_logs FaLsE --plaintext_email TrUe
check invalid-startup 1 false false --monochrome_logs yes --plaintext_email true
check invalid-completion 0 false true --monochrome_logs yes --plaintext_email bad --mode completion-only
echo 'PASS: 6 strict-syntax boolean CLI cases'
