# nf-core Utilities Guide

This comprehensive guide covers the nf-core-utils plugin, a powerful extension that brings essential nf-core functionality directly into your Nextflow pipelines. Whether you're building a new pipeline or migrating from legacy subworkflows, this guide will help you leverage these utilities effectively.

## 1. Overview

The nf-core-utils plugin transforms common pipeline tasks from complex subworkflows into simple function calls. It provides battle-tested utilities for configuration validation, notifications, reporting, and citation management that work seamlessly across all nf-core pipelines.

### 1.1. Core Capabilities

The plugin organizes functionality into six focused areas:

- **Configuration & Validation**: Ensure proper pipeline setup and profile usage
- **Notifications & Logging**: Send completion emails, Slack/Teams messages, and terminal summaries
- **Reporting & MultiQC**: Generate pipeline reports and integrate with MultiQC
- **Version Management**: Track software versions with topic channel support
- **Citation Management**: Extract and format tool citations automatically
- **Orchestrated Reporting**: Coordinate comprehensive reporting workflows

### 1.2. Documentation Structure

Each utility class has dedicated documentation with examples and best practices:

| Category                       | Utility                                                                     | Purpose                                                                |
| ------------------------------ | --------------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| **Configuration & Validation** | **[NfcoreConfigValidator](utilities/NfcoreConfigValidator.md)**             | Pipeline configuration validation and profile checking                 |
| **Notifications & Logging**    | **[NfcoreNotificationUtils](utilities/NfcoreNotificationUtils.md)**         | Email notifications, Slack/Teams integration, and completion summaries |
| **Reporting & MultiQC**        | **[NfcoreReportingUtils](utilities/NfcoreReportingUtils.md)**               | MultiQC integration and pipeline reporting utilities                   |
| **Version Management**         | **[NfcoreVersionUtils](utilities/NfcoreVersionUtils.md)**                   | Version tracking with topic channel support and migration utilities    |
| **Citation Management**        | **[NfcoreCitationUtils](utilities/NfcoreCitationUtils.md)**                 | Citation extraction, processing, and topic channel integration         |
| **Orchestrated Reporting**     | **[NfcoreReportingOrchestrator](utilities/NfcoreReportingOrchestrator.md)** | Comprehensive reporting coordination for versions and citations        |

:::tip{title="Where to Start"}
New to nf-core utilities? Begin with the **Configuration & Validation** section to understand pipeline setup, then explore **Notifications & Logging** for completion handling.
:::

## 2. Quick Start Tutorial

This section provides a hands-on introduction to the most commonly used nf-core utilities. We'll build from basic imports to a fully-featured pipeline with comprehensive logging and reporting.

### 2.1. Your First nf-core Utility

Let's start with the simplest possible example - checking pipeline configuration:

```nextflow title="minimal_example.nf"
#!/usr/bin/env nextflow

include { checkConfigProvided } from 'plugin/nf-core-utils'

workflow {
    checkConfigProvided()
    log.info "Pipeline started with proper validation!"
}
```

```console title="Output"
N E X T F L O W  ~  version 25.04.0
Launching `minimal_example.nf` [focused-turing] - revision: abc1234 [main]

WARN: No custom Nextflow configuration detected! Please provide a custom config file or profile.
INFO: Pipeline started with proper validation!
```

### 2.2. Adding Completion Notifications

Let's expand our example to include completion summaries:

```nextflow title="notification_example.nf" hl_lines="3 10-11"
#!/usr/bin/env nextflow

include { checkConfigProvided; completionSummary } from 'plugin/nf-core-utils'

workflow {
    main:
    checkConfigProvided()
    log.info "Pipeline processing samples..."

    onComplete:
    completionSummary(params.monochrome_logs)
}
```

```console title="Output"
Pipeline completed successfully!

-[nf-core/example] Pipeline completed successfully-
Completed at: 2024-01-15T10:30:45.123Z
Duration    : 45s
CPU hours   : 0.1
Succeeded   : 3
```

### 2.3. Complete Import Pattern

For production pipelines, you'll typically need multiple utilities. Here's a recommended import pattern:

```nextflow title="production_imports.nf"
#!/usr/bin/env nextflow

// Essential utilities for most pipelines
include {
    checkConfigProvided; checkProfileProvided;
    completionSummary; completionEmail;
    paramsSummaryMultiqc; getWorkflowVersion
} from 'plugin/nf-core-utils'

// Advanced features for comprehensive pipelines
include {
    citationsOnTheFly; citationOutputs; getCitation; autoToolCitationText;
    generateComprehensiveReport
} from 'plugin/nf-core-utils'
```

:::note{title="Import Strategy"}
Start with essential utilities and add advanced features as your pipeline grows. Each function is independent and can be imported separately.
:::

## 3. Function Categories and Usage

Understanding when and how to use each function is key to building robust pipelines. Let's explore the main categories with practical examples.

### 3.1. Pipeline Initialization Functions

These functions should be called early in your pipeline to validate setup:

Startup validation is owned by the pipeline. Loading the plugin does not run these
checks automatically. Call each check once, using either the exported function or
your existing template-local implementation, not both. Pipelines that previously
relied only on the observer must add these calls. In particular, call
`checkProfileProvided(args, params.monochrome_logs)` to reject a trailing comma in
`-profile` with a nonzero exit status; observer-only runs do not validate it.

| Function                                      | When to Use                            | Example                                              |
| --------------------------------------------- | -------------------------------------- | ---------------------------------------------------- |
| `checkConfigProvided()`                       | Always, to ensure custom configuration | `checkConfigProvided()`                              |
| `checkProfileProvided(args, monochrome_logs)` | Always, to validate profile arguments  | `checkProfileProvided(args, params.monochrome_logs)` |

```nextflow title="Initialization pattern"
#!/usr/bin/env nextflow

include { checkConfigProvided; checkProfileProvided } from 'plugin/nf-core-utils'

workflow {
    checkConfigProvided()
    checkProfileProvided(args, params.monochrome_logs)
    log.info "Configuration validated successfully"
}
```

### 3.2. Workflow Information Functions

These functions provide metadata and can be called anywhere in your pipeline:

| Function                    | Purpose                                     | Example                     |
| --------------------------- | ------------------------------------------- | --------------------------- |
| `getWorkflowVersion()`      | Get git-aware version string                | `getWorkflowVersion()`      |
| `collectVersions(versions)` | Combine versions from mixed sources to YAML | `collectVersions(versions)` |

### 3.3. MultiQC Integration Functions

| Function                               | Purpose                       | Usage Context            |
| -------------------------------------- | ----------------------------- | ------------------------ |
| `paramsSummaryMultiqc(summary_params)` | Generate MultiQC YAML summary | Process or main workflow |

### 3.4. Completion Handler Functions

:::warning{title="Session Context Required"}
These functions report on the finished run (status, duration, task counts), so call them from the `onComplete:` or `onError:` section of the entry workflow, not from its body.
:::

| Function                                   | Purpose                                   | Usage Context               |
| ------------------------------------------ | ----------------------------------------- | --------------------------- |
| `completionSummary(monochrome_logs)`       | Print colored pipeline completion summary | `onComplete:` or `onError:` |
| `completionEmail(summary_params, ...)`     | Send detailed completion email            | `onComplete:` or `onError:` |
| `imNotification(summary_params, hook_url)` | Send Slack/Teams notification             | `onComplete:` or `onError:` |

```nextflow title="Completion handler pattern"
workflow {
    main:
    log.info "Running ${workflow.manifest.name}"

    onComplete:
    def summary_params = ['Core Options': [runName: workflow.runName, success: workflow.success]]
    completionSummary(params.monochrome_logs)
    if (params.email || params.email_on_fail) {
        completionEmail(summary_params, params.email, params.email_on_fail, params.plaintext_email, params.outdir, params.monochrome_logs, [])
    }
    if (params.hook_url) {
        imNotification(summary_params, params.hook_url)
    }
}
```

### 3.5. Citation Management Functions

Citation management comes in two flavors: modern topic-channel based (recommended) and legacy file-based approaches.

**Citations on the Fly (Recommended)**

The lowest-friction option: derive citations from the tools that actually ran (the `versions` topic every nf-core module already emits) and resolve their metadata from each module's `meta.yml`. No per-module changes, no hand-maintained tool list.

| Function                                          | Purpose                                                 | Usage Context       |
| ------------------------------------------------- | ------------------------------------------------------- | ------------------- |
| `toolsFromVersionsTopic(topicVersions)`           | Tool names that actually ran, from the `versions` data  | Workflow completion |
| `citationsOnTheFly(topicVersions, metaFilePaths)` | Citations for only the tools that ran, from `meta.yml`  | Workflow completion |
| `citationOutputs(citations, formats, template)`   | MultiQC HTML, BibTeX and CSL-JSON citations in one call | Workflow completion |

```nextflow title="Citations on the fly"
include { citationsOnTheFly; citationOutputs } from 'plugin/nf-core-utils'

workflow {
    def meta_yml_paths = files("${projectDir}/modules/**/meta.yml").collect { f -> f.toString() }

    ch_methods_description = channel.topic('versions')
        .toList()
        .map { versions ->
            def citations = citationsOnTheFly(versions, meta_yml_paths)
            citationOutputs(citations, ['html'], file("${projectDir}/assets/methods_description_template.yml")).methods_description
        }
}
```

See [NfcoreCitationUtils](utilities/NfcoreCitationUtils.md#citations-on-the-fly-recommended) for the full walkthrough.

**Modern Topic Channel Citations**

These functions work with a dedicated `citation` topic that each module emits to:

| Function                                   | Purpose                                        | Usage Context        |
| ------------------------------------------ | ---------------------------------------------- | -------------------- |
| `getCitation(metaFilePath)`                | Extract citation for topic channel emission    | Process output block |
| `autoToolCitationText(citationTopics)`     | Generate citation text from topic channels     | Workflow completion  |
| `autoToolBibliographyText(citationTopics)` | Generate bibliography HTML from topic channels | Workflow completion  |

```nextflow title="Modern citation pattern"
include { getCitation; autoToolCitationText } from 'plugin/nf-core-utils'

process FASTQC {
    input:
    path reads

    output:
    path "*.html", emit: html
    val citation_data, topic: citation

    script:
    citation_data = getCitation("${moduleDir}/meta.yml")
    """
    fastqc ${reads}
    """
}

workflow {
    FASTQC(channel.fromPath(params.input))

    channel.topic('citation')
        .toList()
        .map { citations -> autoToolCitationText(citations) }
        .view()
}
```

**Legacy File-Based Citations**

These functions support the older meta.yml file approach:

| Function                                   | Purpose                                         | Usage Context |
| ------------------------------------------ | ----------------------------------------------- | ------------- |
| `generateModuleToolCitation(metaFilePath)` | Extract citations from meta.yml file            | Anywhere      |
| `toolCitationText(citationsMap)`           | Generate citation text from collected citations | Anywhere      |
| `toolBibliographyText(citationsMap)`       | Generate bibliography HTML from citations       | Anywhere      |
| `collectCitationsFromFiles(metaFilePaths)` | Collect citations from multiple meta.yml files  | Anywhere      |

**Migration and Mixed Approaches**

For pipelines transitioning between approaches:

| Function                                             | Purpose                                     | Usage Context       |
| ---------------------------------------------------- | ------------------------------------------- | ------------------- |
| `processCitationsFromTopic(topicData)`               | Process citations from topic channel format | Workflow completion |
| `processMixedCitationSources(topics, files)`         | Combine topic and file-based citations      | Workflow completion |
| `convertMetaYamlToTopicFormat(metaPath, moduleName)` | Convert meta.yml to topic channel format    | Migration scripts   |

## 4. Advanced Reporting Architecture

The nf-core-utils plugin provides a sophisticated reporting system that coordinates version tracking, citation management, and comprehensive report generation.

### 4.1. Reporting Components

The reporting system consists of three specialized utility classes:

| Component                       | Purpose                             | Use When                         |
| ------------------------------- | ----------------------------------- | -------------------------------- |
| **NfcoreVersionUtils**          | Version aggregation and formatting  | Need version tracking only       |
| **NfcoreCitationUtils**         | Citation extraction and processing  | Need citation management only    |
| **NfcoreReportingOrchestrator** | Coordinates comprehensive reporting | Need complete reporting solution |

### 4.2. Simple Reporting Example

For most pipelines, you'll want comprehensive reporting that includes versions, citations, and methods descriptions:

```nextflow title="comprehensive_reporting.nf"
include { generateComprehensiveReport } from 'plugin/nf-core-utils'

workflow {
    def meta_yml_paths = files("${projectDir}/modules/**/meta.yml").collect { f -> f.toString() }

    ch_report = channel.topic('versions')
        .toList()
        .map { versions -> generateComprehensiveReport(versions, [], meta_yml_paths, file("${projectDir}/assets/methods_description_template.yml")) }

    ch_report
        .map { report -> report.versions_yaml }
        .collectFile(name: 'software_versions.yml', storeDir: "${params.outdir}/pipeline_info")
    ch_report
        .map { report -> report.methods_description }
        .collectFile(name: 'methods_description.html', storeDir: "${params.outdir}/pipeline_info")
}
```

### 4.3. Migration from Legacy Systems

The plugin supports a progressive migration path from legacy approaches to modern topic channels:

| Stage          | Description                     | Implementation                               |
| -------------- | ------------------------------- | -------------------------------------------- |
| **Legacy**     | File-based meta.yml citations   | `collectCitationsFromFiles(metaFilePaths)`   |
| **Transition** | Mixed file and topic approaches | `processMixedCitationSources(topics, files)` |
| **Modern**     | Pure topic channel approach     | `processCitationsFromTopic(topicCitations)`  |

:::tip{title="Migration Strategy"}
Start by converting high-priority modules to topic channels while keeping legacy modules unchanged. The mixed approach allows gradual migration without breaking existing functionality.
:::

### 4.4. Software Versions Collection with `collectVersions()`

`collectVersions()` merges versions from topic tuples, `versions.yml` files, YAML strings and maps into one YAML report, with the workflow and Nextflow versions.

```nextflow title="versions.nf"
include { collectVersions } from 'plugin/nf-core-utils'

workflow {
    channel.topic('versions')
        .toList()
        .map { versions -> collectVersions(versions) }
        .collectFile(name: 'software_versions.yml', storeDir: "${params.outdir}/pipeline_info")
}
```

Use `toList()`, not `collect()`: `collect()` flattens the tuples, and emits nothing when no process sent versions.

To report another Nextflow version, pass it as the second argument: `collectVersions(versions, '24.10.0')`.

`softwareVersionsToYAML()` is deprecated and delegates to `collectVersions()`. See [NfcoreVersionUtils](utilities/NfcoreVersionUtils.md#collectversions) for the inputs and the output format.

## 5. Complete Pipeline Example

Let's put everything together in a comprehensive example that shows a production-ready nf-core pipeline using all major utility functions:

```nextflow title="complete_nfcore_pipeline.nf"
#!/usr/bin/env nextflow

include {
    checkConfigProvided; checkProfileProvided;
    completionSummary; completionEmail; imNotification;
    paramsSummaryMultiqc; getWorkflowVersion; collectVersions
} from 'plugin/nf-core-utils'

params {
    input: String
    outdir: String = 'results'
    email: String?
    email_on_fail: String?
    plaintext_email: Boolean = false
    hook_url: String?
    monochrome_logs: Boolean = false
}

process FASTQC {
    input:
    path reads

    output:
    path "*.html", emit: html
    tuple val("${task.process}"), val('fastqc'), eval('fastqc --version | sed "s/FastQC v//"'), topic: versions

    script:
    """
    fastqc ${reads}
    """
}

workflow {
    main:
    checkConfigProvided()
    checkProfileProvided(args, params.monochrome_logs)
    log.info "Starting ${workflow.manifest.name} ${getWorkflowVersion()}"

    FASTQC(channel.fromPath(params.input))

    channel.topic('versions')
        .toList()
        .map { versions -> collectVersions(versions) }
        .collectFile(name: 'software_versions.yml', storeDir: "${params.outdir}/pipeline_info")

    ch_workflow_summary = channel.of(paramsSummaryMultiqc([Pipeline: [Input: params.input, Output: params.outdir]]))
        .collectFile(name: 'workflow_summary_mqc.yaml')

    onComplete:
    def summary_params = ['Core Options': [runName: workflow.runName, version: getWorkflowVersion(), success: workflow.success]]
    completionSummary(params.monochrome_logs)
    if (params.email || params.email_on_fail) {
        completionEmail(summary_params, params.email, params.email_on_fail, params.plaintext_email, params.outdir, params.monochrome_logs, [])
    }
    if (params.hook_url) {
        imNotification(summary_params, params.hook_url)
    }
}
```

This example demonstrates:

- **Configuration validation** at pipeline startup
- **Version collection** from the `versions` topic
- **MultiQC integration** with parameter summaries
- **Comprehensive notifications** on completion

## 6. Best Practices

### 6.1. Pipeline Structure

Follow this recommended structure for consistent nf-core pipeline development:

```nextflow title="Recommended pipeline structure"
#!/usr/bin/env nextflow

include { checkConfigProvided; checkProfileProvided; completionSummary } from 'plugin/nf-core-utils'

workflow {
    main:
    checkConfigProvided()
    checkProfileProvided(args, params.monochrome_logs)
    // pipeline logic

    onComplete:
    completionSummary(params.monochrome_logs)
}
```

### 6.2. Function Usage Guidelines

| Do                                        | Don't                                        | Why                                                    |
| ----------------------------------------- | -------------------------------------------- | ------------------------------------------------------ |
| Call validation functions early           | Skip configuration checks                    | Early validation prevents runtime failures             |
| Use completion handlers for notifications | Call notification functions in main workflow | Session context required for metadata access           |
| Group summary parameters logically        | Use flat parameter structure                 | Grouped parameters create better email formatting      |
| Handle null values in parameters          | Assume parameters always exist               | Defensive programming prevents null pointer exceptions |

### 6.3. Error Handling

:::warning{title="Common Pitfall: Session Context"}
The most common error is calling notification functions outside completion handlers. Always call them from the `onComplete:` or `onError:` section.
:::

## 7. Troubleshooting

### 7.1. Common Issues

| Issue                       | Symptom                                   | Solution                                     |
| --------------------------- | ----------------------------------------- | -------------------------------------------- |
| **Email not sending**       | No completion email received              | Check SMTP configuration in nextflow.config  |
| **Webhook failing**         | No Slack/Teams notification               | Verify webhook URL format and network access |
| **Parameter summary empty** | Empty parameter sections in notifications | Use grouped parameter structure              |

## 8. Takeaway

The nf-core-utils plugin provides a comprehensive toolkit for building robust, production-ready pipelines:

1. **Start with validation**: Use `checkConfigProvided()` and `checkProfileProvided()` early
2. **Add notifications**: Implement `completionSummary()`, `completionEmail()`, and `imNotification()` in completion handlers
3. **Integrate reporting**: Use `paramsSummaryMultiqc()` and citation functions for comprehensive documentation
4. **Follow best practices**: Group parameters, handle null values, and use completion handlers correctly

## 9. What's Next?

Explore specialized utilities for specific use cases:

- **[NfcoreConfigValidator](utilities/NfcoreConfigValidator.md)**: Deep dive into configuration validation
- **[NfcoreNotificationUtils](utilities/NfcoreNotificationUtils.md)**: Master email and messaging integrations
- **[NfcoreReportingUtils](utilities/NfcoreReportingUtils.md)**: Advanced MultiQC integration patterns
- **[NextflowPipelineExtension](NextflowPipelineExtension.md)**: Core pipeline utilities for version and parameter management
- **[ReferencesExtension](ReferencesExtension.md)**: Handle genome references and igenomes integration
