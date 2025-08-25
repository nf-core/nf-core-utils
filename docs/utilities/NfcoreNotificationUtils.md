# NfcoreNotificationUtils

Pipeline notification and logging utilities for nf-core pipelines.

## Overview

The `NfcoreNotificationUtils` utility provides comprehensive notification and logging functionality for nf-core pipelines. These functions handle completion emails, terminal summaries, and instant messaging notifications (Slack/Teams) to keep users informed about pipeline execution status.

⚠️ **Critical Usage Requirement**: All notification functions require Nextflow session context and **must be called from `workflow.onComplete` or `workflow.onError` handlers only**. They cannot be called from the main workflow block.

## Available Functions

### `completionEmail(Map summaryParams, String email, String emailOnFail, boolean plaintextEmail, String outdir, boolean monochromeLogs, List multiqcReports)`

**Description:**  
Sends detailed completion emails with pipeline summary, workflow metadata, and optional MultiQC report attachments. The email includes comprehensive information about the pipeline run, parameters, and results.

**Function Signature:**
```nextflow
void completionEmail(Map summaryParams, String email, String emailOnFail, boolean plaintextEmail, String outdir, boolean monochromeLogs, List multiqcReports)
```

**Parameters:**
- `summaryParams` (Map): Nested map of grouped summary parameters (see structure below)
- `email` (String): Primary email address to notify on success
- `emailOnFail` (String): Email address to notify on failure (can be same or different)
- `plaintextEmail` (boolean): If true, sends plain text email instead of HTML
- `outdir` (String): Output directory path for locating reports
- `monochromeLogs` (boolean): Use monochrome logs in email content
- `multiqcReports` (List): List of MultiQC report paths to attach

**⚠️ Usage Requirements:**
- Must be called from `workflow.onComplete` or `workflow.onError` handlers only
- Requires Nextflow session context (workflow metadata, success status, etc.)
- Do not call from main workflow block

**Summary Parameters Structure:**
```nextflow
def summary_params = [
    'Core Nextflow options': [
        'revision': workflow.revision ?: 'N/A',
        'runName': workflow.runName,
        'containerEngine': workflow.containerEngine,
        'profile': workflow.profile
    ],
    'Input/output options': [
        'input': params.input ?: 'N/A',
        'outdir': params.outdir ?: 'N/A'
    ],
    'Reference genome options': [
        'genome': params.genome,
        'fasta': params.fasta
    ]
]
```

**Usage Example:**
```nextflow
include { completionEmail } from 'plugin/nf-core-utils'

workflow.onComplete {
    if (params.email || params.email_on_fail) {
        def summary_params = [
            'Core Nextflow options': [
                'revision': workflow.revision ?: 'N/A',
                'runName': workflow.runName,
                'containerEngine': workflow.containerEngine,
                'profile': workflow.profile
            ],
            'Input/output options': [
                'input': params.input ?: 'N/A',
                'outdir': params.outdir ?: 'N/A'
            ]
        ]
        
        completionEmail(
            summary_params,
            params.email,
            params.email_on_fail,
            params.plaintext_email,
            params.outdir,
            params.monochrome_logs,
            multiqc_report
        )
    }
}
```

**Email Template Features:**
- **HTML Template**: Rich formatting with sections, tables, and styling
- **Plain Text Option**: Simple text format for compatibility
- **Attachment Support**: Automatically attaches MultiQC reports
- **Status Indication**: Clear success/failure status with appropriate styling
- **Comprehensive Metadata**: Includes timing, resource usage, and configuration details

---

### `completionSummary(boolean monochromeLogs)`

**Description:**  
Prints a colored summary of the pipeline run at completion, showing success/failure status, execution time, and any ignored processes. Provides immediate visual feedback in the terminal.

**Function Signature:**
```nextflow
void completionSummary(boolean monochromeLogs)
```

**Parameters:**
- `monochromeLogs` (boolean): If true, disables color codes for plain text output

**⚠️ Usage Requirements:**
- Must be called from `workflow.onComplete` or `workflow.onError` handlers only
- Requires Nextflow session context (manifest, stats, success status)
- Do not call from main workflow block

**Usage Example:**
```nextflow
include { completionSummary } from 'plugin/nf-core-utils'

workflow.onComplete {
    // Show completion summary with colors (if enabled)
    completionSummary(params.monochrome_logs)
}

workflow.onError {
    // Show error summary
    completionSummary(params.monochrome_logs)
}
```

**Terminal Output Features:**
- **Color-coded Status**: Green for success, red for failure
- **Execution Statistics**: Duration, CPU time, memory usage
- **Process Summary**: Number of processes, cached tasks, ignored processes
- **Resource Information**: Peak memory, CPU efficiency
- **Configuration Summary**: Profile, container engine, work directory

**Example Output:**
```
-[nf-core/rnaseq] Pipeline completed successfully-
Completed at: 2024-01-15T10:30:45.123Z
Duration    : 1h 23m 45s
CPU hours   : 45.2 (85.3% cached)
Succeeded   : 156
Cached      : 12
Ignored     : 0
```

---

### `imNotification(Map summaryParams, String hookUrl)`

**Description:**  
Sends JSON notifications to instant messaging webhooks (Slack, Microsoft Teams) with pipeline summary and workflow metadata. Supports rich formatting for better readability.

**Function Signature:**
```nextflow
void imNotification(Map summaryParams, String hookUrl)
```

**Parameters:**
- `summaryParams` (Map): Nested map of grouped summary parameters (same structure as `completionEmail`)
- `hookUrl` (String): Webhook URL for Slack, Teams, or other compatible service

**⚠️ Usage Requirements:**
- Must be called from `workflow.onComplete` or `workflow.onError` handlers only
- Requires Nextflow session context (workflow metadata, timing, success status)
- Do not call from main workflow block
- Function handles null/empty hookUrl gracefully (logs warning and returns)

**Supported Webhook Types:**
- **Slack**: `https://hooks.slack.com/services/...`
- **Microsoft Teams**: `https://outlook.office.com/webhook/...`
- **Generic webhooks**: Any endpoint accepting JSON POST requests

**Usage Example:**
```nextflow
include { imNotification } from 'plugin/nf-core-utils'

workflow.onComplete {
    if (params.hook_url) {
        def summary_params = [
            'Pipeline Info': [
                'runName': workflow.runName,
                'success': workflow.success,
                'duration': workflow.duration,
                'exitStatus': workflow.exitStatus
            ],
            'Configuration': [
                'profile': workflow.profile,
                'container': workflow.containerEngine
            ]
        ]
        
        imNotification(summary_params, params.hook_url)
    }
}

workflow.onError {
    if (params.hook_url) {
        def summary_params = [
            'Error Details': [
                'runName': workflow.runName,
                'errorMessage': workflow.errorMessage,
                'exitStatus': workflow.exitStatus
            ]
        ]
        
        imNotification(summary_params, params.hook_url)
    }
}
```

**JSON Payload Structure:**
- **Service Detection**: Automatically detects Slack vs Teams format
- **Rich Formatting**: Uses service-specific formatting (markdown, cards)
- **Color Coding**: Green for success, red for failure
- **Comprehensive Data**: Includes all workflow metadata and custom parameters

---

## Complete Integration Example

Here's a comprehensive example showing all notification functions working together:

```nextflow
#!/usr/bin/env nextflow

include { completionSummary; completionEmail; imNotification } from 'plugin/nf-core-utils'

workflow {
    // Main workflow logic here
    log.info "Pipeline execution starting..."
    
    // Process data...
    
    log.info "Pipeline execution complete"
}

workflow.onComplete {
    // Always show terminal summary
    completionSummary(params.monochrome_logs)
    
    // Prepare summary parameters (reused for all notifications)
    def summary_params = [
        'Core Nextflow options': [
            'revision': workflow.revision ?: 'N/A',
            'runName': workflow.runName,
            'containerEngine': workflow.containerEngine,
            'profile': workflow.profile,
            'configFiles': workflow.configFiles.join(', ')
        ],
        'Input/output options': [
            'input': params.input ?: 'N/A',
            'outdir': params.outdir ?: 'N/A',
            'publish_dir_mode': params.publish_dir_mode
        ],
        'Reference genome options': [
            'genome': params.genome ?: 'None',
            'fasta': params.fasta ?: 'N/A'
        ],
        'Max job request options': [
            'max_cpus': params.max_cpus,
            'max_memory': params.max_memory,
            'max_time': params.max_time
        ]
    ]
    
    // Send completion email if configured
    if (params.email || params.email_on_fail) {
        completionEmail(
            summary_params,
            params.email,
            params.email_on_fail,
            params.plaintext_email,
            params.outdir,
            params.monochrome_logs,
            multiqc_report.ifEmpty([])
        )
    }
    
    // Send instant message notification if configured
    if (params.hook_url) {
        imNotification(summary_params, params.hook_url)
    }
    
    log.info "All notifications sent successfully"
}

workflow.onError {
    // Show error summary
    completionSummary(params.monochrome_logs)
    
    // Send error notifications if configured
    if (params.email_on_fail || params.hook_url) {
        def error_params = [
            'Error Details': [
                'runName': workflow.runName,
                'errorMessage': workflow.errorMessage ?: 'Unknown error',
                'exitStatus': workflow.exitStatus,
                'errorReport': workflow.errorReport ?: 'No error report available'
            ],
            'Configuration': [
                'profile': workflow.profile,
                'workDir': workflow.workDir,
                'container': workflow.containerEngine
            ]
        ]
        
        // Send error email
        if (params.email_on_fail) {
            completionEmail(
                error_params,
                null,  // No regular email
                params.email_on_fail,  // Only send to error email
                params.plaintext_email,
                params.outdir,
                params.monochrome_logs,
                null  // No MultiQC report on error
            )
        }
        
        // Send error IM notification
        if (params.hook_url) {
            imNotification(error_params, params.hook_url)
        }
    }
}
```

---

## Best Practices

### 1. Parameter Organization

Group parameters logically for better email/notification formatting:

```nextflow
// ✅ GOOD - Grouped structure
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

// ❌ AVOID - Flat structure
def summary_params = [
    runName: workflow.runName,
    profile: workflow.profile,
    input: params.input,
    outdir: params.outdir
]
```

### 2. Error Handling

Always provide fallback values for potentially null parameters:

```nextflow
def summary_params = [
    'Core Nextflow options': [
        'revision': workflow.revision ?: 'N/A',
        'runName': workflow.runName ?: 'Unknown',
        'profile': workflow.profile ?: 'default'
    ]
]
```

### 3. Testing Notifications

Test notification functions without sending actual notifications:

```nextflow
// Test mode - use mock webhook URL or disable actual sending
params.hook_url = params.test_mode ? 'https://httpbin.org/post' : params.hook_url
params.email = params.test_mode ? null : params.email
```

### 4. Security Considerations

- Store webhook URLs securely (use environment variables or secure config files)
- Don't log sensitive parameters in summary
- Use separate failure emails for security notifications

```nextflow
// Use environment variables for sensitive URLs
params.hook_url = System.getenv('SLACK_WEBHOOK_URL')

// Filter sensitive parameters from summary
def filtered_params = params.findAll { k, v -> 
    !k.toLowerCase().contains('password') && 
    !k.toLowerCase().contains('token') 
}
```

---

## Common Issues and Solutions

### Issue: "Session is null" Error

**Problem:**
```nextflow
// ❌ WRONG - This will cause null pointer exceptions
workflow {
    completionSummary(params.monochrome_logs)  // ERROR: session is null
}
```

**Solution:**
```nextflow
// ✅ CORRECT - Call from completion handlers
workflow.onComplete {
    completionSummary(params.monochrome_logs)  // ✅ Works correctly
}
```

### Issue: Email Not Sending

**Common Causes:**
1. SMTP configuration not set up in Nextflow config
2. Email parameters not provided
3. MultiQC report path incorrect

**Solution:**
```nextflow
// Check configuration
if (!params.email && !params.email_on_fail) {
    log.info "No email addresses provided - skipping email notification"
}

// Verify MultiQC report exists
def mqc_report = multiqc_report.ifEmpty([])
log.info "MultiQC reports to attach: ${mqc_report}"
```

### Issue: Webhook Failures

**Common Causes:**
1. Invalid webhook URL format
2. Network connectivity issues
3. Service-specific formatting problems

**Solution:**
```nextflow
// Validate webhook URL
if (params.hook_url && !params.hook_url.startsWith('http')) {
    log.warn "Invalid webhook URL format: ${params.hook_url}"
    params.hook_url = null
}

// Test webhook connectivity
if (params.hook_url) {
    log.info "Webhook configured: ${params.hook_url.take(20)}..."
}
```

---

## Testing

Test notification functions using nf-test:

```groovy
test("Notification functions work correctly") {
    when {
        params {
            email = "test@example.com"
            email_on_fail = "admin@example.com"
            hook_url = "https://hooks.slack.com/test"
            monochrome_logs = false
        }
    }
    
    then {
        assert workflow.success
        // Verify notification functions were called (check logs)
        assert workflow.stdout.contains("All notifications sent successfully")
    }
}

test("Error notifications work correctly") {
    when {
        // Force pipeline failure
        script """
        exit 1
        """
    }
    
    then {
        assert workflow.failed
        // Verify error notifications were sent
        assert workflow.stdout.contains("Error Details")
    }
}
```

---

## Integration with External Services

### Slack Integration

```nextflow
// Slack webhook configuration
params.hook_url = "https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK"

// Slack-specific parameter formatting
def slack_summary = [
    'Pipeline Status': [
        'name': workflow.manifest.name,
        'version': workflow.manifest.version,
        'status': workflow.success ? 'SUCCESS' : 'FAILED'
    ]
]
```

### Microsoft Teams Integration

```nextflow
// Teams webhook configuration  
params.hook_url = "https://outlook.office.com/webhook/YOUR/TEAMS/WEBHOOK"

// Teams supports rich card formatting
def teams_summary = [
    'Execution Summary': [
        'duration': workflow.duration,
        'cpu_hours': workflow.stats.computeTimeFmt,
        'memory_peak': workflow.stats.peakRss
    ]
]
```

For more information on setting up external service integrations, see the [nf-core documentation](https://nf-co.re/docs/usage/configuration#notifications).