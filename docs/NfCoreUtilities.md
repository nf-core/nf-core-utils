# nf-core Utility Functions

This extension provides a DSL2-friendly wrapper for nf-core utility functions, making them easily accessible in your Nextflow pipelines. This document describes these utility functions provided by the nf-core-utils plugin for nf-core pipelines.

---

## Importing Functions

Import functions in your Nextflow DSL2 script as follows:

```nextflow
include { checkConfigProvided; completionEmail; logColours; paramsSummaryMultiqc;
          completionSummary; imNotification; getWorkflowVersion;
          generateModuleToolCitation; toolCitationText; toolBibliographyText;
          generateComprehensiveReport } from 'plugin/nf-core-utils'
```

---

## Quick Reference Table

### Core Functions
| Function             | Purpose                                      | Typical Usage Example                       |
| -------------------- | -------------------------------------------- | ------------------------------------------- |
| checkConfigProvided  | Warn if no custom config/profile is provided | `checkConfigProvided()`                     |
| checkProfileProvided | Validate profile argument                    | `checkProfileProvided(args)`                |
| getWorkflowVersion   | Get workflow version string                  | `getWorkflowVersion()`                      |
| paramsSummaryMultiqc | Generate MultiQC summary YAML                | `paramsSummaryMultiqc([Summary: ...])`      |
| workflowSummaryMQC   | Create MultiQC summary template              | `workflowSummaryMQC(...)`                   |
| sectionLogs          | Generate colored section logs                | `sectionLogs(sections, monochrome)`         |
| logColours           | Get ANSI color codes for logs                | `logColours(params.monochrome_logs)`        |
| completionSummary    | Print summary at pipeline completion         | `completionSummary(params.monochrome_logs)` |
| completionEmail      | Send completion email                        | `completionEmail(...)`                      |
| imNotification       | Send Slack/Teams notification                | `imNotification(..., hook_url)`             |
| getSingleReport      | Get a single report from Path/List           | `getSingleReport(multiqc_report)`           |

### Citation Functions
| Function                        | Purpose                                           | Typical Usage Example                                    |
| ------------------------------- | ------------------------------------------------- | -------------------------------------------------------- |
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

### Basic Citation Extraction

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
checkProfileProvided(args)

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

```nextflow
include { completionSummary; completionEmail; imNotification } from 'plugin/nf-core-utils'

workflow.onComplete {
    completionSummary(params.monochrome_logs)

    if (params.email || params.email_on_fail) {
        def summary = [Run_Name: workflow.runName]
        completionEmail(
            [Summary: summary],
            params.email,
            params.email_on_fail,
            params.plaintext_email,
            params.outdir,
            params.monochrome_logs,
            multiqc_report
        )
    }

    if (params.hook_url) {
        def summary = [Run_name: workflow.runName]
        imNotification([Summary: summary], params.hook_url)
    }
}
```

---

## Citation Function Details

### Citation Extraction and Processing

#### `generateModuleToolCitation(metaFilePath)`

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

#### `checkProfileProvided(args)`

**Description:**  
Checks if the `-profile` argument is valid and warns about positional arguments.

**Parameters:**

- `args` (Array): Command-line arguments passed to the pipeline

**Example:**

```nextflow
checkProfileProvided(args)
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
Prints a summary of the pipeline run at completion.

**Parameters:**

- `monochrome_logs` (Boolean, default: `true`): If true, disables color codes

**Example:**

```nextflow
workflow.onComplete {
    completionSummary(params.monochrome_logs)
}
```

---

#### `completionEmail(summary_params, email, email_on_fail, plaintext_email, outdir, monochrome_logs=true, multiqc_report=null)`

**Description:**  
Constructs and sends a completion email with pipeline summary and optional MultiQC report.

**Parameters:**

- `summary_params` (Map): Summary parameters
- `email` (String): Email address to notify
- `email_on_fail` (String): Email for failures only
- `plaintext_email` (Boolean): Send plaintext email if true
- `outdir` (String): Output directory for reports
- `monochrome_logs` (Boolean, default: `true`): Use monochrome logs
- `multiqc_report` (Path|List, optional): MultiQC report file(s)

**Example:**

```nextflow
workflow.onComplete {
    def summary = [Run_Name: workflow.runName]
    completionEmail(
        [Summary: summary],
        params.email,
        params.email_on_fail,
        params.plaintext_email,
        params.outdir,
        params.monochrome_logs,
        multiqc_report
    )
}
```

---

#### `imNotification(summary_params, hook_url)`

**Description:**  
Sends a notification to a webhook (e.g., Slack, Teams) with pipeline summary.

**Parameters:**

- `summary_params` (Map): Summary parameters
- `hook_url` (String): Webhook URL

**Example:**

```nextflow
if (params.hook_url) {
    def summary = [Run_Name: workflow.runName]
    imNotification([Summary: summary], params.hook_url)
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
