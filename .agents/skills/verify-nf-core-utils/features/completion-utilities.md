# Completion and pipeline utilities

Pipelines print a coloured or monochrome completion summary, send a completion email and a Slack/Teams notification when configured, show a version string such as `v2.1.0-gabc123d`, and save their parameters to `params.json`.

## Sub-features

- `completion-summary` prints the completion summary in colour and in monochrome.
- `completion-email-disabled` builds the email report without sending when no address is set.
- `im-disabled` skips the notification when no hook URL is set.
- `workflow-version` formats versions with and without a commit, and with a `v` prefix.
- `dump-params` writes `params.json` with the given parameters.

## How to get to it (user POV)

- Call the completion functions from `workflow.onComplete`.
- Call `getWorkflowVersion()` and `dumpParametersToJSON(outdir, params)` from the workflow body.

## Driving it with verify

Preconditions:

- Baseline preconditions from the index hold.
- No `--email`, `--email_on_fail` or `--hook_url` is passed.

- **Completion handlers.** Run `verify drive completion validation/notification-system`. `exit-code.txt` is `0`. `stderr.txt` contains `Pipeline completed successfully` twice (once with ANSI colour codes, once plain; match that phrase, since escape codes split the coloured line), `✅ completionEmail exercised` and `✅ imNotification exercised`.
- **Version strings and params.** Run `verify drive utilities validation/pipeline-utilities`. `exit-code.txt` is `0` and `stderr.txt` contains `Explicit version (2.1.0): v2.1.0` and `Version with v prefix (v3.0.0): v3.0.0`. Before `cleanup`, copy `$VERIFY_RUN/state/launch/utilities/work/validation_output/pipeline_info/params_*.json` into the evidence dir; it holds `"max_cpus": 16`.

## Gotchas

- Email and IM are proven only on their disabled paths. Real delivery needs a mail server or webhook and the user's approval; it is covered by unit tests, not by this map.
- `dumpParametersToJSON(outdir, params)` writes `<outdir>/pipeline_info/params_<timestamp>.json`, inside the work dir here, which `drive` does not copy and `cleanup` deletes.
- The journey checks `validation_output/params.json`, which never exists, so it logs `WARN: JSON file not found` and skips its content asserts; its snapshot records that warning. Its `✅ dumpParametersToJSON() function tested successfully` line proves nothing. Check the timestamped file yourself.- `pipeline-utilities` writes timeline/report/trace into `${launchDir}/validation_results`, not `results/`.
