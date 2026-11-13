# Pipeline Notifications Tutorial

Keeping users informed about pipeline execution is crucial for building user-friendly bioinformatics workflows. This tutorial teaches you how to implement notifications using the `NfcoreNotificationUtils` utility, from terminal summaries to email and messaging integrations.

## 1. Overview

Modern bioinformatics pipelines often run for hours or days, making it essential to notify users about completion status, errors, and results. Users need to know:

- When their pipeline completes successfully or fails
- What parameters were used and what results were generated
- Where to find outputs and reports

The `NfcoreNotificationUtils` utility provides three notification channels:

- **Terminal Summaries**: Color-coded completion status
- **Email Notifications**: Detailed reports with the MultiQC report attached
- **Instant Messaging**: Slack/Teams integration for team collaboration

### 1.1. Critical Session Context Requirement

:::warning{title="Call from completion handlers"}
**Call notification functions from the `onComplete:` or `onError:` section of the entry workflow.** They read the workflow status, timing and task statistics, which are only final once the run has finished. Called from the workflow body, they report an unfinished run.
:::

## 2. Getting Started with Notifications

### 2.1. Your First Notification

```nextflow title="basic_notification.nf"
include { completionSummary } from 'plugin/nf-core-utils'

params {
    monochrome_logs: Boolean = false
}

workflow {
    main:
    log.info "Analysis complete"

    onComplete:
    completionSummary(params.monochrome_logs)
}
```

```console title="Terminal Output"
-[nf-core/example] Pipeline completed successfully-
```

## 3. Core Notification Functions

### 3.1. completionSummary - Terminal Status Display

This function logs a one-line, color-coded completion status.

**Function Parameters**

```groovy title="Function signature"
void completionSummary(boolean monochromeLogs)
```

| Parameter        | Type    | Description                                           |
| ---------------- | ------- | ----------------------------------------------------- |
| `monochromeLogs` | Boolean | If `true`, disables color codes for plain text output |

**Output**

```console title="Possible statuses"
-[nf-core/rnaseq] Pipeline completed successfully-
-[nf-core/rnaseq] Pipeline completed successfully, but with errored process(es) -
-[nf-core/rnaseq] Pipeline completed with errors-
```

The pipeline name comes from `manifest.name`. With colors enabled, the name is purple and the status green, yellow or red.

### 3.2. completionEmail - Detailed Email Reports

Email notifications provide comprehensive pipeline reports that users can refer to later.

Emails are delivered with Nextflow's built-in [`sendMail()`](https://docs.seqera.io/nextflow/notifications), so `--email` uses the same mail settings as `nextflow run -N`. Nextflow picks the transport in this order:

1. AWS Simple Email Service when `NXF_ENABLE_AWS_SES=true` (requires the `nf-amazon` plugin)
2. SMTP when the `mail.smtp` config scope is set, for example:

   ```groovy title="nextflow.config"
   mail {
       from = 'pipeline@example.com'
       smtp.host = 'email-smtp.us-east-1.amazonaws.com'
       smtp.port = 587
       smtp.user = secrets.SMTP_USER
       smtp.password = secrets.SMTP_PASSWORD
       smtp.auth = true
       smtp.starttls.enable = true
       smtp.starttls.required = true
       // Transport timeouts are in milliseconds
       smtp.connectiontimeout = 5000
       smtp.timeout = 5000
       smtp.writetimeout = 5000
   }
   ```

3. AWS SES when `AWS_REGION` or `AWS_DEFAULT_REGION` is set and `nf-amazon` is loaded. Launch environments often export these variables for other AWS tools, so email can switch to SES without any mail configuration. The launch machine's IAM principal needs `ses:SendRawEmail`, and `mail.from` must be an SES-verified identity; otherwise sending fails with a warning. Configure `mail.smtp` to override this region-based selection. Explicit `NXF_ENABLE_AWS_SES=true` still takes precedence over SMTP.
4. The local `sendmail` command, then `mail` (the `mail` command sends the plain text only, without attachments)

The `onComplete` handler runs on the machine that launched the pipeline, so mail credentials and environment variables must be available there, not in task containers.

HTML emails contain the rendered `assets/email_template.html` with `assets/email_template.txt` as the plain-text alternative, the pipeline logo `assets/<name>_logo_light.png` (the manifest name with `/` replaced by `-`) inline as `cid:nfcorepipelinelogo`, and the MultiQC report when it is smaller than `params.max_multiqc_email_size`. Plaintext emails (`plaintext_email`) contain only the text template. `assets/sendmail_template.txt` is no longer used.

HTML and text reports are saved under `outdir/pipeline_info` before delivery is attempted. If delivery fails, the function logs a warning and the run is not failed. Success means the mail transport accepted the message, not that the recipient received it.

Attachment preparation and delivery run on a daemon thread with a 30-second wait limit. On timeout, the function logs a warning and returns so `onComplete` can finish and Nextflow can stop running tasks. Cancellation is best-effort: a transport that ignores interruption may continue sending, so delivery status is unknown and the function does not retry. The sender removes temporary attachments when it exits, not while it might still be reading them; if it remains blocked until the JVM exits, temporary copies may remain on disk.

MultiQC attachments retain the original report basename, including reports on remote filesystems such as object storage. Remote reports are copied into a temporary directory only when an HTML email has a recipient; both the copy and directory are removed after sending, including when sending fails, and cleanup failures are warnings. Reports at or above `params.max_multiqc_email_size` are not attached or downloaded. Some transports, such as AWS SES, limit the message size further. Plaintext emails and successful runs with only `email_on_fail` do not read or download attachments. The HTML and text summary reports are still saved even when no email is sent.

**Seqera Platform launches**

When [`TOWER_WORKFLOW_ID`](https://docs.seqera.io/platform-cloud/launch/advanced#environment-variables) is non-empty, `completionEmail()` skips delivery for both `email` and `email_on_fail`, logs the reason, and does not read or download attachments. It still saves the HTML and text summary reports. CLI runs using `-with-tower`, `tower.enabled`, or `TOWER_ACCESS_TOKEN` alone are not muted.

This default applies even when Platform's own completion notification is disabled: the head job cannot inspect that account setting. Enable Platform notifications separately if you want Platform to send the completion email. The Launch form's Email parameter and Platform's notification toggle are independent settings.

To retain nf-core email, including MultiQC attachments, for a Platform launch, set the boolean `params.force_completion_email = true` in Nextflow configuration (or pass `--force_completion_email true` where the pipeline's parameter schema permits it). You must still supply `email` or `email_on_fail`; the override does not change recipient selection or failure-only behavior. Pipeline authors exposing this option through schema validation should declare it as a boolean with default `false`. Enabling both this override and Platform notifications can produce two emails.

The override still needs a working mail transport on the compute-environment head job. For AWS SES, configure head environment variable `NXF_ENABLE_AWS_SES=true`, load `nf-amazon`, and grant the head IAM role `ses:SendRawEmail` for the verified sender, subject to SES region and sandbox restrictions. Alternatively, configure `mail.smtp` on the head. Platform server settings such as `TOWER_ENABLE_AWS_SES` and `TOWER_SMTP_*` only configure Platform's own mail; they do not configure Nextflow's mailer.

**Function Parameters Reference**

```groovy title="completionEmail signature"
void completionEmail(
    Map summaryParams,       // Grouped parameter summary
    String email,            // Primary email address
    String emailOnFail,      // Failure notification email
    boolean plaintextEmail,  // Use plain text instead of HTML
    String outdir,           // Output directory path
    boolean monochromeLogs,  // Color settings for email content
    List multiqcReports      // MultiQC report attachments
)
```

`summaryParams` groups parameters by section; each group becomes a table in the email:

```nextflow
def summary_params = [
    'Core Nextflow options': [
        'runName': workflow.runName,
        'profile': workflow.profile,
        'revision': workflow.revision ?: 'N/A'
    ],
    'Input/output options': [
        'input': params.input,
        'outdir': params.outdir
    ]
]
```

**Email with MultiQC Report**

```nextflow title="email_notification.nf"
include { completionEmail } from 'plugin/nf-core-utils'

params {
    input: String
    outdir: String
    email: String?
    email_on_fail: String?
    plaintext_email: Boolean = false
    monochrome_logs: Boolean = false
}

workflow {
    main:
    MULTIQC(ch_multiqc_files.collect())
    multiqc_reports = MULTIQC.out.report.toList()

    onComplete:
    def summary_params = [
        'Core Nextflow options': [
            'runName': workflow.runName,
            'profile': workflow.profile
        ],
        'Input/output options': [
            'input': params.input,
            'outdir': params.outdir
        ]
    ]
    completionEmail(
        summary_params,
        params.email,
        params.email_on_fail,
        params.plaintext_email,
        params.outdir,
        params.monochrome_logs,
        multiqc_reports.isBound() ? multiqc_reports.getVal() : []
    )
}
```

:::warning{title="Guard the MultiQC report"}
Collect the output with `multiqc_reports = MULTIQC.out.report.toList()` in the workflow, then pass `multiqc_reports.isBound() ? multiqc_reports.getVal() : []` in the `onComplete:` section. `ifEmpty([])` returns a channel, not the resolved list that `completionEmail()` expects. Never use an unguarded `getVal()`: when a run aborts, `onComplete` runs before Nextflow stops running tasks, and the MultiQC output may never be produced. An unguarded read can then wait forever and prevent task cleanup. Arguments are evaluated before entering `completionEmail()`, so its delivery deadline cannot protect this read. See [nf-core/tools#4486](https://github.com/nf-core/tools/pull/4486).
:::

### 3.3. imNotification - Instant Messaging Integration

Sends JSON notifications to instant messaging webhooks (Slack, Microsoft Teams) with the pipeline summary and workflow metadata.

Notification logs omit webhook URLs, response bodies, and exception messages because they can contain credentials. HTTP failures report the status code; other failures report the exception type.

**Function Signature:**

```groovy
void imNotification(Map summaryParams, String hookUrl)
```

**Parameters:**

- `summaryParams` (Map): Nested map of grouped summary parameters (same structure as `completionEmail`)
- `hookUrl` (String): Webhook URL for Slack, Teams, or other compatible service

**Usage Requirements:**

- Call from the `onComplete:` or `onError:` section of the entry workflow
- A null or empty `hookUrl` logs a warning and returns

**Supported Webhook Types:**

- **Slack**: URLs containing `hooks.slack.com` use the pipeline's `assets/slackreport.json` template
- **Microsoft Teams and others**: any other URL uses `assets/adaptivecard.json` ([Adaptive Cards](https://adaptivecards.io))

When the template is missing, a plain `{"text": ...}` message with the run name and status is sent.

**Usage Example:**

```nextflow
include { imNotification } from 'plugin/nf-core-utils'

params {
    hook_url: String?
}

workflow {
    main:
    log.info "Running pipeline"

    onComplete:
    def summary_params = [
        'Pipeline Info': [
            'runName': workflow.runName,
            'profile': workflow.profile
        ]
    ]
    imNotification(summary_params, params.hook_url)
}
```

---

## Complete Integration Example

All notification functions together, as in an nf-core pipeline:

```nextflow
include { completionSummary; completionEmail; imNotification } from 'plugin/nf-core-utils'

params {
    input: String
    outdir: String
    email: String?
    email_on_fail: String?
    plaintext_email: Boolean = false
    monochrome_logs: Boolean = false
    hook_url: String?
}

workflow {
    main:
    MULTIQC(ch_multiqc_files.collect())
    multiqc_reports = MULTIQC.out.report.toList()

    onComplete:
    def summary_params = [
        'Core Nextflow options': [
            'revision': workflow.revision ?: 'N/A',
            'runName': workflow.runName,
            'containerEngine': workflow.containerEngine,
            'profile': workflow.profile,
            'configFiles': workflow.configFiles.join(', ')
        ],
        'Input/output options': [
            'input': params.input,
            'outdir': params.outdir
        ]
    ]

    if (params.email || params.email_on_fail) {
        completionEmail(
            summary_params,
            params.email,
            params.email_on_fail,
            params.plaintext_email,
            params.outdir,
            params.monochrome_logs,
            multiqc_reports.isBound() ? multiqc_reports.getVal() : []
        )
    }

    completionSummary(params.monochrome_logs)

    if (params.hook_url) {
        imNotification(summary_params, params.hook_url)
    }

    onError:
    log.error "Pipeline failed: ${workflow.errorMessage}"
}
```

`onComplete:` also runs after a failure, so `completionEmail()` sends to `email_on_fail` and `imNotification()` reports the failure from there; `onError:` needs no notification calls.

---

## Best Practices

### 1. Parameter Organization

Group parameters by section, as `completionEmail()` and `imNotification()` render one table per group. Provide fallbacks for values that may be null, such as `workflow.revision ?: 'N/A'`.

### 2. Testing Notifications

Leave `email`, `email_on_fail` and `hook_url` unset in test profiles: the functions then save the email reports under `outdir/pipeline_info` without sending anything. See `validation/notification-system/` in this repository for a tested example.

### 3. Security Considerations

- Store webhook URLs securely (Nextflow secrets or environment variables, not committed config)
- Don't log sensitive parameters in summary
- Use separate failure emails for security notifications

---

## Common Issues and Solutions

### Issue: Wrong or Incomplete Status

**Problem:** `completionSummary()` reports `Pipeline completed with errors` for a run that is still going.

**Solution:** Move the call from the workflow body to the `onComplete:` section.

### Issue: Email Not Sending

**Common Causes:**

1. No mail transport available: configure `mail.smtp` or `NXF_ENABLE_AWS_SES`, or install `sendmail` on the launch machine (see section 3.2)
2. Neither `email` nor `email_on_fail` provided
3. MultiQC report missing or above `params.max_multiqc_email_size`

The warning `Could not send the summary e-mail to ...` includes the error from Nextflow's mailer. Set `mail.debug = true` to log the SMTP session.

### Issue: Webhook Failures

**Common Causes:**

1. Invalid webhook URL
2. Network connectivity issues
3. Missing or invalid `assets/slackreport.json` / `assets/adaptivecard.json` template

Failures are logged as warnings with the HTTP status code or exception type; the run is not failed.

For more information on setting up external service integrations, see the [nf-core documentation](https://nf-co.re/docs/running/run-pipelines#notifications).
