# nf-core Utility Functions

This extension provides a DSL2-friendly wrapper for nf-core utility functions, making them easily accessible in your Nextflow pipelines. This document provides an overview of the utility functions and links to detailed documentation for each utility class.

## Documentation Structure

The nf-core-utils plugin is organized into focused utility classes, each with dedicated documentation:

### 🔧 **Configuration & Validation**
- **[NfcoreConfigValidator](utilities/NfcoreConfigValidator.md)** - Pipeline configuration validation and profile checking

### 📧 **Notifications & Logging** 
- **[NfcoreNotificationUtils](utilities/NfcoreNotificationUtils.md)** - Email notifications, Slack/Teams integration, and completion summaries

### 📊 **Reporting & MultiQC**
- **[NfcoreReportingUtils](utilities/NfcoreReportingUtils.md)** - MultiQC integration and pipeline reporting utilities

### 🔖 **Version Management**
- **[NfcoreVersionUtils](utilities/NfcoreVersionUtils.md)** - Version tracking with topic channel support and migration utilities

### 📚 **Citation Management**
- **[NfcoreCitationUtils](utilities/NfcoreCitationUtils.md)** - Citation extraction, processing, and topic channel integration

### 🎼 **Orchestrated Reporting**
- **[NfcoreReportingOrchestrator](utilities/NfcoreReportingOrchestrator.md)** - Comprehensive reporting coordination for versions and citations

---

## Quick Start

### Basic Import Pattern

Import functions in your Nextflow DSL2 script as follows:

```nextflow
include { checkConfigProvided; completionEmail; getWorkflowVersion;
          getCitation; autoToolCitationText; generateComprehensiveReport } from 'plugin/nf-core-utils'
```

---

## Quick Reference Table

### Core Functions
| Function             | Purpose                                      | Usage Context | Typical Usage Example                       |
| -------------------- | -------------------------------------------- | ------------- | ------------------------------------------- |
| checkConfigProvided  | Warn if no custom config/profile is provided | Main workflow | `checkConfigProvided()`                     |
| checkProfileProvided | Validate profile argument                    | Main workflow | `checkProfileProvided(args, monochrome_logs)` |
| getWorkflowVersion   | Get workflow version string                  | Anywhere      | `getWorkflowVersion()`                      |
| paramsSummaryMultiqc | Generate MultiQC summary YAML                | Main workflow/Process | `paramsSummaryMultiqc([Summary: ...])`      |
| workflowSummaryMQC   | Create MultiQC summary template              | Main workflow/Process | `workflowSummaryMQC(...)`                   |
| sectionLogs          | Generate colored section logs                | Anywhere      | `sectionLogs(sections, monochrome)`         |
| logColours           | Get ANSI color codes for logs                | Anywhere      | `logColours(params.monochrome_logs)`        |
| **completionSummary**    | **Print summary at pipeline completion**         | **⚠️ onComplete/onError only** | `completionSummary(params.monochrome_logs)` |
| **completionEmail**      | **Send completion email**                        | **⚠️ onComplete/onError only** | `completionEmail(summary_params, ...)`                      |
| **imNotification**       | **Send Slack/Teams notification**                | **⚠️ onComplete/onError only** | `imNotification(summary_params, hook_url)`             |
| getSingleReport      | Get a single report from Path/List           | Anywhere      | `getSingleReport(multiqc_report)`           |

**⚠️ Important**: Functions marked with **⚠️ onComplete/onError only** require Nextflow session context and must be called from `workflow.onComplete` or `workflow.onError` handlers, not from the main workflow block.

### Citation Functions
| Function                        | Purpose                                           | Typical Usage Example                                    |
| ------------------------------- | ------------------------------------------------- | -------------------------------------------------------- |
| getCitation                     | Extract citation for topic channel emission      | `getCitation("${moduleDir}/meta.yml")`                 |
| autoToolCitationText            | Generate citation text from topic channels       | `autoToolCitationText(citationTopics)`                 |
| autoToolBibliographyText        | Generate bibliography HTML from topic channels   | `autoToolBibliographyText(citationTopics)`             |
| generateModuleToolCitation      | Extract citations from meta.yml file             | `generateModuleToolCitation('meta.yml')`               |
| toolCitationText                | Generate citation text from collected citations  | `toolCitationText(citationsMap)`                       |
| toolBibliographyText            | Generate bibliography HTML from citations        | `toolBibliographyText(citationsMap)`                   |
| collectCitationsFromFiles       | Collect citations from multiple meta.yml files   | `collectCitationsFromFiles(metaFilePaths)`             |
| processCitationsFromTopic       | Process citations from topic channels             | `processCitationsFromTopic(topicData)`                 |
| processMixedCitationSources     | Combine topic and file-based citations           | `processMixedCitationSources(topics, files)`          |
| convertMetaYamlToTopicFormat    | Convert meta.yml to topic channel format         | `convertMetaYamlToTopicFormat(metaPath, moduleName)`  |

---

## Version and Citation Reporting (New Architecture)

### Overview

Version and citation management are now cleanly separated for maintainability and clarity. Use the following classes for advanced reporting:

- **NfcoreVersionUtils**: Handles version aggregation and formatting (YAML, topic channels, etc.)
- **NfcoreCitationUtils**: Handles citation extraction and formatting from module meta.yml files
- **NfcoreReportingOrchestrator**: Orchestrates both for comprehensive reporting (versions, citations, bibliography, methods)

### Usage Examples

#### Version-only Reporting

```groovy
import nfcore.plugin.nfcore.NfcoreReportingOrchestrator

def versionReport = NfcoreReportingOrchestrator.generateVersionReport(
    topicVersions, legacyVersions, session
)
println versionReport.versions_yaml
```

#### Citation-only Reporting

```groovy
import nfcore.plugin.nfcore.NfcoreReportingOrchestrator

def citationReport = NfcoreReportingOrchestrator.generateCitationReport(
    metaFilePaths, mqcMethodsYaml, session
)
println citationReport.tool_citations
println citationReport.tool_bibliography
```

#### Comprehensive Reporting (Versions + Citations)

```groovy
import nfcore.plugin.nfcore.NfcoreReportingOrchestrator

def report = NfcoreReportingOrchestrator.generateComprehensiveReport(
    topicVersions, legacyVersions, metaFilePaths, mqcMethodsYaml, session
)
println report.versions_yaml
println report.tool_citations
println report.tool_bibliography
println report.methods_description
```

### Topic Channel Migration Strategy

The citation system supports progressive migration from file-based meta.yml citations to topic channels, similar to the version system:

#### Migration Path
1. **Legacy Stage**: Use file-based meta.yml citations
2. **Transition Stage**: Mix both file-based and topic channel citations
3. **Modern Stage**: Use only topic channel citations

#### Topic Channel Formats
- **`citations`** topic: New eval syntax `[module, tool, citation_data]`
- **`citations_file`** topic: Legacy file paths to meta.yml files

#### Example Migration

```groovy
// Stage 1: Legacy file-based approach
def legacyCitations = collectCitationsFromFiles(metaFilePaths)

// Stage 2: Mixed approach during migration
def mixedCitations = processMixedCitationSources(
    topicCitations,  // New format from some modules
    citationFiles    // Legacy files from other modules
)

// Stage 3: Pure topic channel approach
def modernCitations = processCitationsFromTopic(topicCitations)
```

---

## Citation Management Examples

### Automatic Topic Channel Citations (Recommended)

```nextflow
include { getCitation; autoToolCitationText; autoToolBibliographyText } from 'plugin/nf-core-utils'

// In your process definitions
process FASTQC {
    input:
    val sample_id
    
    output:
    path "*.html", emit: html
    val citation_data, topic: citation
    
    script:
    citation_data = getCitation("${moduleDir}/meta.yml")
    """
    fastqc ${sample_id}
    """
}

// In your workflow
workflow {
    FASTQC(samples)
    
    // Collect all citations automatically
    citation_ch = FASTQC.out.citation.collect()
    
    // Generate citation text and bibliography
    citation_ch.view { citations ->
        def citationText = autoToolCitationText(citations)
        def bibliography = autoToolBibliographyText(citations)
        println "Citations: ${citationText}"
        println "Bibliography: ${bibliography}"
    }
}
```

### Basic Citation Extraction (Legacy)

```nextflow
include { generateModuleToolCitation; toolCitationText; toolBibliographyText } from 'plugin/nf-core-utils'

// Extract citations from a single module
def fastqcCitations = generateModuleToolCitation('modules/nf-core/fastqc/meta.yml')

// Generate citation text and bibliography
def citationText = toolCitationText(fastqcCitations)
def bibliography = toolBibliographyText(fastqcCitations)

println "Citations: ${citationText}"
println "Bibliography: ${bibliography}"
```

### Topic Channel Citation Processing

```nextflow
include { processCitationsFromTopic; processMixedCitationSources } from 'plugin/nf-core-utils'

// Process citations from new topic channel format
def topicCitations = [
    ['NFCORE_FASTQC', 'fastqc', [doi: '10.1093/bioinformatics/btv033', author: 'Andrews S']],
    ['NFCORE_SAMTOOLS', 'samtools', [description: 'SAM/BAM processing utilities']]
]
def citationsFromTopic = processCitationsFromTopic(topicCitations)

// Combine with legacy file-based citations
def legacyFiles = ['modules/local/custom/meta.yml']
def allCitations = processMixedCitationSources(topicCitations, legacyFiles)
```

### Comprehensive Reporting with Citations

```nextflow
include { generateComprehensiveReport } from 'plugin/nf-core-utils'

// Generate complete report with versions and citations
def report = generateComprehensiveReport(
    topicVersions,      // Version data from topic channels
    legacyVersions,     // Legacy version YAML strings
    metaFilePaths,      // Paths to meta.yml files for citations
    'multiqc_methods.yml' // MultiQC methods template
)

// Access different parts of the report
println "Versions: ${report.versions_yaml}"
println "Citations: ${report.tool_citations}"
println "Bibliography: ${report.tool_bibliography}"
println "Methods: ${report.methods_description}"
```

---

## Usage Examples

### Pipeline Initialization

```nextflow
#!/usr/bin/env nextflow

include { checkConfigProvided; checkProfileProvided } from 'plugin/nf-core-utils'
checkConfigProvided()
checkProfileProvided(args, params.monochrome_logs)

include { logColours } from 'plugin/nf-core-utils'
def colors = logColours(params.monochrome_logs)
log.info "${colors.purple}Pipeline started${colors.reset}"
```

---

### In Processes

<!-- TODO Idk if this will work? -->

```nextflow
include { paramsSummaryMultiqc } from 'plugin/nf-core-utils'

process MULTIQC {
    // ... process definition ...
    script:
    def summary = [Run_name: workflow.runName, Output_dir: params.outdir]
    def yaml = paramsSummaryMultiqc([Summary: summary])
    """
    echo '$yaml' > workflow_summary_mqc.yaml
    multiqc -f .
    """
}
```

---

### At Pipeline Completion

**⚠️ IMPORTANT**: Notification functions (`completionSummary`, `completionEmail`, `imNotification`) must be called from within `workflow.onComplete` or `workflow.onError` handlers, not from the main workflow block. They require the Nextflow session context which is only available in completion handlers.

```nextflow
include { completionSummary; completionEmail; imNotification } from 'plugin/nf-core-utils'

workflow.onComplete {
    // Always call completionSummary to show pipeline completion status
    completionSummary(params.monochrome_logs)

    // Send completion email if configured
    if (params.email || params.email_on_fail) {
        // Prepare summary parameters - group related parameters together
        def summary_params = [
            'Core Nextflow options': [
                'revision': workflow.revision ?: 'N/A',
                'runName': workflow.runName,
                'containerEngine': workflow.containerEngine,
                'profile': workflow.profile
            ],
            'Input/output options': [
                'input': params.input ?: 'N/A',
                'outdir': params.outdir ?: 'N/A',
                'email': params.email ?: 'N/A'
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

    // Send Slack/Teams notification if webhook URL is provided
    if (params.hook_url) {
        def summary_params = [
            'Pipeline Info': [
                'runName': workflow.runName,
                'success': workflow.success,
                'duration': workflow.duration
            ]
        ]
        imNotification(summary_params, params.hook_url)
    }
}

workflow.onError {
    // Send error notifications
    if (params.email_on_fail) {
        def summary_params = [
            'Error Info': [
                'runName': workflow.runName,
                'errorMessage': workflow.errorMessage ?: 'Unknown error',
                'exitStatus': workflow.exitStatus
            ]
        ]
        
        completionEmail(
            summary_params,
            null,  // No regular email
            params.email_on_fail,  // Only send to error email
            params.plaintext_email,
            params.outdir,
            params.monochrome_logs,
            null  // No MultiQC report on error
        )
    }
}
```

---

## Citation Function Details

### Citation Extraction and Processing

#### `getCitation(metaFilePath)` ⭐ Recommended

**Description:**  
Extracts citation information from a module's meta.yml file for topic channel emission. This is the modern approach for automatic citation collection that only includes citations for tools that actually execute.

**Parameters:**
- `metaFilePath` (String): Path to the meta.yml file, typically `"${moduleDir}/meta.yml"`

**Returns:**
- `List`: Citation data formatted for topic channel emission `[module, tool, citation_data]`

**Usage in Process:**
```nextflow
process FASTQC {
    output:
    val citation_data, topic: citation
    
    script:
    citation_data = getCitation("${moduleDir}/meta.yml")
    """
    # Process script here
    """
}
```

#### `autoToolCitationText(citationTopics)`

**Description:**  
Generates formatted citation text from topic channel citation data. Processes the automatic citation collection from executed processes.

**Parameters:**
- `citationTopics` (List<List>): Citation data from topic channels

**Returns:**
- `String`: Formatted citation text for methods descriptions

**Example:**
```nextflow
def citationText = autoToolCitationText(citation_ch.collect())
// Returns: "Tools used in the workflow included: fastqc (DOI: ...), samtools (DOI: ...)."
```

#### `autoToolBibliographyText(citationTopics)`

**Description:**  
Generates HTML bibliography from topic channel citation data. Creates bibliography entries for tools that were actually executed.

**Parameters:**
- `citationTopics` (List<List>): Citation data from topic channels

**Returns:**
- `String`: HTML bibliography for MultiQC reports

**Example:**
```nextflow
def bibliography = autoToolBibliographyText(citation_ch.collect())
// Returns HTML list items for bibliography
```

#### `generateModuleToolCitation(metaFilePath)` (Legacy)

**Description:**  
Extracts citation information from a module's meta.yml file.

**Parameters:**
- `metaFilePath` (String|File): Path to the meta.yml file

**Returns:**
- `Map`: Citations map with tool names as keys and citation/bibliography data as values

**Example:**
```nextflow
def citations = generateModuleToolCitation('modules/nf-core/fastqc/meta.yml')
// Returns: [fastqc: [citation: 'fastqc (DOI: ...)', bibliography: '<li>...</li>']]
```

#### `toolCitationText(collectedCitations)`

**Description:**  
Generates formatted citation text from collected citations map.

**Parameters:**
- `collectedCitations` (Map): Map of tool citations

**Returns:**
- `String`: Formatted citation text for use in methods descriptions

**Example:**
```nextflow
def citationText = toolCitationText(citations)
// Returns: "Tools used in the workflow included: fastqc (DOI: ...), samtools (DOI: ...)."
```

#### `toolBibliographyText(collectedCitations)`

**Description:**  
Generates HTML bibliography from collected citations.

**Parameters:**
- `collectedCitations` (Map): Map of tool citations

**Returns:**
- `String`: HTML bibliography for MultiQC reports

#### `processCitationsFromTopic(topicData)`

**Description:**  
Processes citations from topic channel format (new eval syntax).

**Parameters:**
- `topicData` (List<List>): List of [module, tool, citation_data] tuples

**Returns:**
- `Map`: Processed citations map

**Example:**
```nextflow
def topicCitations = [
    ['NFCORE_FASTQC', 'fastqc', [doi: '10.1093/...', author: 'Andrews S']]
]
def citations = processCitationsFromTopic(topicCitations)
```

#### `processMixedCitationSources(topicCitations, citationFiles)`

**Description:**  
Combines citations from both topic channels and legacy files for progressive migration.

**Parameters:**
- `topicCitations` (List<List>): Topic channel citation data
- `citationFiles` (List<String>): List of meta.yml file paths

**Returns:**
- `Map`: Combined citations from both sources

---

## Function Details

---

### Configuration and Validation

#### `checkConfigProvided()`

**Description:**  
Checks if a custom Nextflow config or profile has been provided. Logs a warning if not.

**Returns:**

- `true` if a custom config/profile is provided
- `false` otherwise

**Example:**

```nextflow
if (!checkConfigProvided()) {
    log.warn "No custom configuration provided! Please provide a profile or custom config."
}
```

---

#### `checkProfileProvided(args, monochromeLogs=true)`

**Description:**  
Checks if the `-profile` argument is valid and warns about positional arguments. Error messages include color formatting when colors are enabled.

**Parameters:**

- `args` (Array): Command-line arguments passed to the pipeline
- `monochromeLogs` (Boolean, default: `true`): If true, disables color codes in error messages

**Example:**

```nextflow
checkProfileProvided(args)
// or with colors enabled
checkProfileProvided(args, params.monochrome_logs)
```

---

### Workflow Information

#### `getWorkflowVersion()`

**Description:**  
Returns a string representing the workflow version, including the git commit short SHA if available.

**Returns:**

- `String` (e.g., `v1.2.3-gabcdef1`)

**Example:**

```nextflow
log.info "Pipeline version: ${getWorkflowVersion()}"
```

---

### Reporting and MultiQC

#### `paramsSummaryMultiqc(summaryParams)`

**Description:**  
Generates a YAML-formatted string for MultiQC workflow summary.

**Parameters:**

- `summaryParams` (Map): Map of parameter groups and their parameters

**Returns:**

- `String`: YAML for MultiQC

**Example:**

```nextflow
def summary = [Run_Name: workflow.runName, Output_Dir: params.outdir]
def yaml = paramsSummaryMultiqc([Summary: summary])
```

---

#### `workflowSummaryMQC(summary, nfMetadataList, results)`

**Description:**  
Creates a workflow summary template for MultiQC.

**Parameters:**

- `summary` (Map): Map of parameters
- `nfMetadataList` (List): List of metadata fields to include
- `results` (Map): Map of pipeline results

**Returns:**

- `Map` with HTML and text summaries for MultiQC

**Example:**

```nextflow
def summary = [Run_id: workflow.runName, User: workflow.userName]
def metadata = ['version', 'start', 'complete', 'duration']
def results = [Reads_Processed: '1,000,000', Failed_QC: '0.1%']
def mqc_summary = workflowSummaryMQC(summary, metadata, results)
```

---

#### `sectionLogs(sections, monochrome=false)`

**Description:**  
Generates summary logs for each section of a pipeline, optionally with color.

**Parameters:**

- `sections` (Map): Section names and log messages
- `monochrome` (Boolean, default: `false`): Use monochrome logs

**Returns:**

- `Map` of colored section logs

**Example:**

```nextflow
def sections = [
    FastQC: 'Processed 10 samples',
    Alignment: 'Aligned to reference genome',
    MultiQC: 'Report generated'
]
def logs = sectionLogs(sections, params.monochrome_logs)
log.info logs.FastQC
```

---

### Notification and Logging

#### `logColours(monochrome_logs=true)`

**Description:**  
Returns a map of ANSI color codes for terminal logging.

**Parameters:**

- `monochrome_logs` (Boolean, default: `true`): If true, disables color codes

**Returns:**

- `Map<String, String>`: Color codes

**Example:**

```nextflow
def colors = logColours(params.monochrome_logs)
log.info "${colors.purple}Pipeline started${colors.reset}"
```

---

#### `completionSummary(monochrome_logs=true)`

**Description:**  
Prints a colored summary of the pipeline run at completion, showing success/failure status and any ignored processes.

**Parameters:**

- `monochrome_logs` (Boolean, default: `true`): If true, disables color codes

**⚠️ Usage Requirements:**
- Must be called from `workflow.onComplete` or `workflow.onError` handlers only
- Requires Nextflow session context (manifest, stats, success status)
- Do not call from main workflow block

**Example:**

```nextflow
workflow.onComplete {
    // Show completion summary with colors (if enabled)
    completionSummary(params.monochrome_logs)
}

workflow.onError {
    // Show error summary
    completionSummary(params.monochrome_logs)
}
```

---

#### `completionEmail(summary_params, email, email_on_fail, plaintext_email, outdir, monochrome_logs=true, multiqc_report=null)`

**Description:**  
Constructs and sends a completion email with pipeline summary, workflow metadata, and optional MultiQC report attachments.

**Parameters:**

- `summary_params` (Map): Map of grouped summary parameters (see structure below)  
- `email` (String): Primary email address to notify on success
- `email_on_fail` (String): Email address to notify on failure (can be same or different)
- `plaintext_email` (Boolean): If true, sends plain text email instead of HTML
- `outdir` (String): Output directory path for locating reports
- `monochrome_logs` (Boolean, default: `true`): Use monochrome logs in email content
- `multiqc_report` (Path|List, optional): MultiQC report file(s) to attach

**⚠️ Usage Requirements:**
- Must be called from `workflow.onComplete` or `workflow.onError` handlers only
- Requires Nextflow session context (workflow metadata, success status, etc.)
- Do not call from main workflow block

**Summary Parameters Structure:**
```nextflow
def summary_params = [
    'Core Nextflow options': [
        'revision': workflow.revision,
        'runName': workflow.runName,
        'containerEngine': workflow.containerEngine,
        // ... other core options
    ],
    'Input/output options': [
        'input': params.input,
        'outdir': params.outdir,
        // ... other I/O options  
    ],
    'Reference genome options': [
        'genome': params.genome,
        // ... other genome options
    ]
    // ... other parameter groups
]
```

**Example:**

```nextflow
workflow.onComplete {
    if (params.email || params.email_on_fail) {
        // Group parameters logically for better email formatting
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

---

#### `imNotification(summary_params, hook_url)`

**Description:**  
Sends a JSON notification to instant messenger webhooks (e.g., Slack, Microsoft Teams) with pipeline summary and workflow metadata.

**Parameters:**

- `summary_params` (Map): Map of grouped summary parameters (same structure as `completionEmail`)
- `hook_url` (String): Webhook URL for Slack, Teams, or other compatible service

**⚠️ Usage Requirements:**
- Must be called from `workflow.onComplete` or `workflow.onError` handlers only
- Requires Nextflow session context (workflow metadata, timing, success status)
- Do not call from main workflow block
- Function handles null/empty hook_url gracefully (logs warning and returns)

**Supported Webhook Types:**
- **Slack**: `https://hooks.slack.com/services/...`
- **Microsoft Teams**: `https://outlook.office.com/webhook/...`
- **Generic webhooks**: Any endpoint accepting JSON POST requests

**Example:**

```nextflow
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
    // Send error notification to Slack/Teams
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

---

#### `getSingleReport(multiqc_reports)`

**Description:**  
Returns a single report file from a Path or List of Paths.

**Parameters:**

- `multiqc_reports` (Path or List): MultiQC report(s)

**Returns:**

- `Path` or `null`

**Example:**

```nextflow
def mqc_report = getSingleReport(multiqc_report)
```

---

## Common Mistakes and Troubleshooting

### ❌ Common Mistake: Calling Notification Functions in Main Workflow

**Problem:**
```nextflow
// ❌ WRONG - This will cause null pointer exceptions
workflow {
    completionSummary(params.monochrome_logs)  // ERROR: session is null
    
    if (params.email) {
        completionEmail(summary_params, params.email, ...)  // ERROR: session is null
    }
}
```

**Solution:**
```nextflow  
// ✅ CORRECT - Call from completion handlers
workflow {
    // Main workflow logic here...
    log.info "Pipeline execution complete"
}

workflow.onComplete {
    // Now session context is available
    completionSummary(params.monochrome_logs)  // ✅ Works correctly
    
    if (params.email) {
        completionEmail(summary_params, params.email, ...)  // ✅ Works correctly
    }
}
```

### ❌ Common Mistake: Incorrect Summary Parameters Format

**Problem:**
```nextflow
// ❌ WRONG - Flat structure makes email hard to read
def summary = [
    runName: workflow.runName,
    input: params.input,
    outdir: params.outdir,
    genome: params.genome
]
```

**Solution:**
```nextflow
// ✅ CORRECT - Grouped structure for better email formatting  
def summary_params = [
    'Core Nextflow options': [
        'runName': workflow.runName,
        'profile': workflow.profile
    ],
    'Input/output options': [
        'input': params.input,
        'outdir': params.outdir
    ],
    'Reference genome options': [
        'genome': params.genome
    ]
]
```

### 🔧 Testing Notification Functions

If you need to test notification functions in a development pipeline:

```nextflow
// Create a minimal test pipeline
workflow {
    log.info "Testing notification functions..."
    log.info "Functions imported successfully"
}

workflow.onComplete {
    // Test completionSummary
    completionSummary(false)
    
    // Test with mock parameters (won't actually send emails)
    def test_params = [
        'Test Parameters': [
            'runName': workflow.runName,
            'success': workflow.success
        ]
    ]
    
    // These will run but won't send actual notifications in test mode
    completionEmail(test_params, 'test@example.com', null, true, null, false, null)
    imNotification(test_params, 'https://hooks.slack.com/test')
    
    log.info "Notification functions tested successfully"
}
```

### 🐛 Debugging Session Context Issues

If you see null pointer exceptions, verify:

1. **Function location**: Are you calling from `workflow.onComplete`/`onError`?
2. **Session availability**: Is `nextflow.Nextflow.session` accessible?
3. **Parameter structure**: Are summary parameters properly grouped?

```nextflow
workflow.onComplete {
    // Debug session context
    log.info "Session available: ${nextflow.Nextflow.session != null}"
    log.info "Workflow success: ${workflow.success}"
    log.info "Workflow runName: ${workflow.runName}"
    
    // Then proceed with notification functions
    completionSummary(params.monochrome_logs)
}
```

---

## Internal Helper Classes

The plugin organizes functionality into several internal utility classes, now located in `src/main/groovy/nfcore/plugin/nfcore/`:

- **NfcoreConfigValidator:** Validates pipeline configurations and profiles.
- **NfcoreNotificationUtils:** Handles notifications, emails, and terminal output formatting.
- **NfcoreReportingUtils:** Manages reporting functions for MultiQC and pipeline summaries.
- **NfcoreVersionUtils:** Provides version-related utility functions with topic channel support.
- **NfcoreCitationUtils:** Handles citation extraction, processing, and topic channel support.
- **NfcoreReportingOrchestrator:** Orchestrates version and citation utilities for comprehensive reporting (versions, citations, bibliography, methods).

### Citation System Architecture

The citation system follows the same progressive migration pattern as the version system:

- **Topic Channel Support**: Both `citations` and `citations_file` topics
- **Format Conversion**: Utilities to convert between legacy and modern formats
- **Mixed Processing**: Handle both formats simultaneously during migration
- **Comprehensive Integration**: Seamless integration with version reporting

For advanced usage or troubleshooting, see the source code in `src/main/groovy/nfcore/plugin/nfcore/`.

---
