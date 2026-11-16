# Migrating to nf-core-utils 1.0

This guide covers two moves: upgrading a pipeline that already loads nf-core-utils 0.7.0, and replacing the utility code that nf-core pipelines copy from the pipeline template and the `utils_nextflow_pipeline` / `utils_nfcore_pipeline` subworkflows with imports from the plugin. Version 1.0 keeps every exported function name and default and renames nothing, so a 0.7.0 pipeline needs no code change. Pipelines that migrate from template-local helpers change a few call sites and see a few behavior differences, which this page lists.

:::warning{title="Pending a verified run"}
The maintainer decisions behind this guide were made on 2026-11-16. The one open item is a verified before and after run against an installed 1.0 candidate (section 5, nf-core/nf-core-utils#119). Until it lands, the examples are syntax-checked only. Items marked "In 1.0 (PR ...)" describe changes delivered by pull requests that merge before the release.
:::

## 1. Who this is for and the supported runtime

This guide is for:

- Pipelines that load `nf-core-utils@0.7.0` (or an earlier release) and move to 1.0.
- Pipelines that carry local copies of the template helpers (`getWorkflowVersion`, `dumpParametersToJSON`, `completionEmail`, `getGenomeAttribute` and others) and want to import them from the plugin.

**Runtime:** Nextflow 25.10.4 or newer. Older runtimes must be upgraded before the plugin loads. CI tests the minimum and every supported stable release line (25.10, 26.04 and 26.10).

```groovy
plugins {
    id 'nf-core-utils@1.0.0'
}
```

:::note{title="No renames in 1.0"}
1.0 adds no breaking renames. The functions already marked `@Deprecated` still work and still delegate to `collectVersions()`. Their removal, the wider boolean-flag redesign (nf-core/nf-core-utils#113, #114 and #121) and any other breaking change belong to the 2.0 migration (nf-core/nf-core-utils#123). Two functions are added (section 3.9) and three accept text flags (section 3.8).
:::

## 2. Removals and behavior changes from earlier releases

Pipelines upgrading from an older release than 0.7.0 should read these in order. The [CHANGELOG](https://github.com/nf-core/nf-core-utils/blob/main/CHANGELOG.md) has the full entries.

| Change                                                                                                                        | Since | What to do                                                                                                                    |
| ----------------------------------------------------------------------------------------------------------------------------- | ----- | ----------------------------------------------------------------------------------------------------------------------------- |
| `NfcorePipelineUtils` removed                                                                                                 | 0.6.0 | Use the exported functions in section 3.                                                                                      |
| `getReferencesFile()` and `getReferencesValue()` removed                                                                      | 0.7.0 | Use `getReferencePath()`, `getReferenceMetadata()` or `getReferenceAttribute()`.                                              |
| Loading the plugin no longer validates startup                                                                                | 0.7.0 | Call `checkConfigProvided()` and `checkProfileProvided(args)` during initialization.                                          |
| `completionEmail()` sends through Nextflow's `sendMail()`; `emailCommands()` and `runEmailCommand()` removed                  | 0.7.0 | Delete `assets/sendmail_template.txt`. Configure the `mail` scope (SMTP or SES) as for `nextflow run -N`.                     |
| Mail is skipped on Seqera Platform launches                                                                                   | 0.7.0 | Set the Boolean `params.force_completion_email = true` to keep nf-core mail on Platform, or use Platform's own notifications. |
| Nextflow floor is 25.10.4                                                                                                     | 0.7.0 | Upgrade the runtime.                                                                                                          |
| Citation metadata reads `publication.*`; flat `author`, `year`, `title` and `journal` still work when no `publication` is set | 0.7.0 | None required.                                                                                                                |
| `toolCitationText()` returns HTML, not plain text                                                                             | 0.7.0 | Write citation files as `.html`.                                                                                              |

### 2.1. From 0.7.0 to 1.0

Nothing is removed and nothing new is deprecated. Additions and fixes:

- `citationOutputs()` returns citations as MultiQC HTML, BibTeX or CSL-JSON.
- Citation map entries carry `info` and `version`.
- `methodsDescriptionText()` no longer modifies the `meta` map you pass.
- Remote paths (`s3://`, and so on) work for versions files and methods templates.
- A `doi` that is not a bare DOI is ignored with a warning.
- `genomeExistsError()` and `logColours()` are exported (PR #128).
- `completionEmail()`, `completionSummary()` and `checkProfileProvided()` accept `"true"` and `"false"` (PR #127).
- Default email templates are bundled (PR #126).
- Versions such as `1.20` keep their text when read from a legacy `versions.yml` (PR #129).

### 2.2. Current deprecations

These four exports are `@Deprecated`, delegate to `collectVersions()` and are not removed in 1.0.

| Deprecated                     | Use instead         |
| ------------------------------ | ------------------- |
| `processMixedVersionSources()` | `collectVersions()` |
| `processVersionsFromTopic()`   | `collectVersions()` |
| `processVersionsFromFile()`    | `collectVersions()` |
| `softwareVersionsToYAML()`     | `collectVersions()` |

## 3. Replacing template-local utilities

Import from the plugin in the file that calls the function:

```nextflow
include { checkConfigProvided; checkProfileProvided; getWorkflowVersion; dumpParametersToJSON; checkCondaChannels; collectVersions; paramsSummaryMultiqc; methodsDescriptionText; completionEmail; completionSummary; getGenomeAttribute } from 'plugin/nf-core-utils'
```

The "before" blocks show the shape of the template code, not a verbatim copy. The template at nf-core/tools `9ae33b4` is the reference.

:::note{title="Template pins lag nf-core/modules"}
The template pins older copies of the utils subworkflows than nf-core/modules master. Compare against the pin in your `modules.json`.
:::

### 3.1. Startup checks

Before, in the `utils_nfcore_pipeline` subworkflow:

```nextflow
UTILS_NFCORE_PIPELINE(nextflow_cli_args)
```

After, call the checks directly in the initialisation subworkflow:

```nextflow
workflow PIPELINE_INITIALISATION {
    take:
    nextflow_cli_args

    main:
    checkConfigProvided()
    checkProfileProvided(nextflow_cli_args, params.monochrome_logs)
}
```

`checkConfigProvided()` returns a boolean. `checkProfileProvided(List args, boolean monochromeLogs = true)` throws an `AbortOperationException` with the same `ERROR ~ ...` text as the template's `error()`. Note the `monochromeLogs` default is `true`.

:::note{title="One set of startup messages"}
Keep the call where the template made it, and call it once, so startup messages appear once.
:::

### 3.2. Workflow version

Before:

```nextflow
def getWorkflowVersion() {
    def version_string = "" as String
    if (workflow.manifest.version) {
        def prefix_v = workflow.manifest.version[0] != 'v' ? 'v' : ''
        version_string += "${prefix_v}${workflow.manifest.version}"
    }
    if (workflow.commitId) {
        version_string += "-g${workflow.commitId[0..6]}"
    }
    return version_string
}
```

After, `getWorkflowVersion(String version = null, String commitId = null)`:

```nextflow
def version = getWorkflowVersion()
```

The `v` prefix and the `-g<7-character sha>` suffix are the same.

### 3.3. Parameters as JSON

Before:

```nextflow
dumpParametersToJSON(params.outdir)
```

After, `dumpParametersToJSON(String outdir, Map params)`. **The params map is now an explicit second argument:**

```nextflow
dumpParametersToJSON(params.outdir, params)
```

A null `outdir` returns without writing. Errors are logged, not thrown, and the temporary file is written under `java.io.tmpdir`.

### 3.4. Conda channels

Before:

```nextflow
checkCondaChannels()
```

After, `checkCondaChannels()` returns a boolean and adds a warning when no channels are found:

```nextflow
def channels_ok = checkCondaChannels()
```

The decision to run the check when a conda profile is active stays in the pipeline.

### 3.5. Software versions

Before, topic tuples grouped and written per process:

```nextflow
topic_versions = channel.topic('versions')
    .map { process, tool, version -> [process, "${tool}: ${version}"] }
    .unique()
    .map { process, line -> "${process}:\n  ${line}" }
```

After, `collectVersions(Object input, Object nextflowVersion = null)`. **Collect the channel to a list first;** one call handles topic tuples, `Path` objects and strings:

```nextflow
channel.topic('versions')
    .toList()
    .map { collected -> collectVersions(collected) }
```

:::warning{title="Output differs"}
The result is **one merged YAML string**, not a channel of per-process strings. Name and store the file in the pipeline (the template's `collectFile` name and `storeDir` stay pipeline-owned). The `Workflow` block comes last, and the Nextflow version resolves from `nextflow.version` in config, then `NXF_VER`, then the running version.
:::

:::warning{title="Version quoting"}
`collectVersions()` writes YAML with SnakeYAML, which quotes numeric-looking versions and keeps doing so in 1.0 (`fastqc: '2.0'`, `ten: '1.10'`). The template writes topic versions bare. Migrated pipelines will see a one-time change in the versions file and in nf-test snapshots.

In 1.0 (PR #129), versions read from a legacy `versions.yml` keep their text: `1.20` stays `1.20` instead of becoming `1.2` (nf-core/tools#4500). Those values render unquoted, as before.
:::

### 3.6. MultiQC parameter summary

Before:

```nextflow
def paramsSummaryMultiqc(summary_params) {
    // builds the MultiQC YAML from the grouped parameter map
}
```

After, `paramsSummaryMultiqc(Map summaryParams)`:

```nextflow
def summary_params = paramsSummaryMap(workflow, parameters_schema: 'nextflow_schema.json')
def summary_yaml = paramsSummaryMultiqc(summary_params)
```

`paramsSummaryMap` comes from nf-schema and stays. The only difference is cosmetic: the plugin closes the `N/A` span correctly.

### 3.7. Methods description

Before, a local function that fills `assets/methods_description_template.yml`:

```nextflow
def methodsDescriptionText(mqc_methods_yaml) {
    // reads the template and fills it from workflow metadata
}
```

After, `methodsDescriptionText(Object mqcMethodsYaml, Map collectedCitations = [:], Map meta = [:])`:

```nextflow
def methods_text = methodsDescriptionText(file("${projectDir}/assets/methods_description_template.yml"), [:], [tool_citations: '', tool_bibliography: ''])
```

The plugin defaults `tool_citations` to `No tools used in the workflow.` and `tool_bibliography` to `No bibliography entries found.`, where the template defaults both to an empty string. Pass the `meta` override above to keep the template output. The template file itself stays in the pipeline.

### 3.8. Completion handlers

Before:

```nextflow
completionEmail(summary_params, email, email_on_fail, plaintext_email, outdir, monochrome_logs, multiqc_report.toList())
completionSummary(monochrome_logs)
```

After, the same calls with the plugin signatures `completionEmail(Map summaryParams, String email, String emailOnFail, Object plaintextEmail, String outdir, Object monochromeLogs, List multiqcReports)` and `completionSummary(Object monochromeLogs)`. In 1.0 (PR #127) the flag arguments accept a Boolean or the text `true` or `false` (any case), so values set on the command line work. Any other value logs a warning and uses the default. `checkProfileProvided(List args, Object monochromeLogs = true)` accepts the same values. Nothing is renamed:

```nextflow
workflow.onComplete {
    if (params.email || params.email_on_fail) {
        completionEmail(summary_params, params.email, params.email_on_fail, params.plaintext_email, params.outdir, params.monochrome_logs, multiqc_reports)
    }
    completionSummary(params.monochrome_logs)
}
```

`imNotification(Map summaryParams, String hookUrl)` is not used by the template, so there is nothing to migrate.

:::warning{title="Behavior differences"}

- **`plaintext_email` is honored.** The template built an exception and never threw it, so it always sent HTML. The plugin sends plain text when asked.
- **Mail is suppressed on Seqera Platform launches** (non-empty `TOWER_WORKFLOW_ID`), unless `params.force_completion_email = true`. Reports are still written.
- Delivery goes through `sendMail()` with a 30-second limit and a delivery failure is a warning, not a failure.
- Without a pipeline copy of `assets/email_template.{txt,html}`, the plugin falls back to placeholder content.
  :::

:::note{title="Bundled email templates"}
In 1.0 (PR #126, nf-core/nf-core-utils#118) the plugin bundles default HTML and text email templates, adapted from the pipeline template. A pipeline can delete its generic `assets/email_template.html`, `assets/email_template.txt` and `assets/sendmail_template.txt`. To customize, keep either file in `assets/`: each format uses the pipeline file when present and the bundled default otherwise. The bundled HTML shows the logo only when the pipeline has one. Slack and Teams JSON templates are not bundled yet.
:::

### 3.9. Genome attributes and references

Before:

```nextflow
def getGenomeAttribute(attribute) {
    if (params.genomes && params.genome && params.genomes.containsKey(params.genome)) {
        if (params.genomes[params.genome].containsKey(attribute)) {
            return params.genomes[params.genome][attribute]
        }
    }
    return null
}
```

After, `getGenomeAttribute(String attribute)`:

```nextflow
include { getGenomeAttribute } from 'plugin/nf-core-utils'

params.fasta = getGenomeAttribute('fasta')
```

The plugin reads `params` from the session config and needs `genome` to be a string.

For reference datasheets, the named-argument functions `getReferencePath(Map args)`, `getReferenceMetadata(Map args)` and `getReferenceAttribute(Map args)` replace the removed `getReferencesFile()` and `getReferencesValue()`.

:::note{title="genomeExistsError and logColours"}
In 1.0 (PR #128) the plugin adds two exports:

- `genomeExistsError()` takes no arguments, reads `params` from the session and throws an `AbortOperationException` with the template's message when `params.genome` is not a key of `params.genomes`. A null genome or an empty `genomes` map passes. Replace the local copy with `genomeExistsError()`.
- `logColours(boolean monochromeLogs = true)` returns the template's map of colour names to escape codes (all empty when `monochromeLogs` is true). Replace `logColours(params.monochrome_logs)` with the same call imported from the plugin.

`sectionLogs`, `workflowSummaryMQC` and `getSingleReport` stay internal. Keep local copies if you use them.
:::

### 3.10. Citations

The template's `toolCitationText()` and `toolBibliographyText()` are hard-coded prose stubs and stay pipeline-owned. To generate citations from module `meta.yml` files, use the opt-in functions `toolCitationText(Map collectedCitations)`, `autoToolCitationText(List topicCitations = [])`, `citationsOnTheFly(List topicVersions, List<String> metaFilePaths, List<String> extraTools = [])` and `citationOutputs(Map citations, List<String> formats = ['html'], Object mqcMethodsYaml = null)`.

## 4. What stays pipeline-owned

The plugin does not replace these:

- **nf-schema:** `validateParameters`, `paramsHelp`, `paramsSummaryLog`, `paramsSummaryMap`, `samplesheetToList` and the `UTILS_NFSCHEMA_PLUGIN` wrapper.
- **Samplesheet checks:** `validateInputSamplesheet` and the endedness check.
- **Startup control flow:** the `--version` early exit, the help exit, the profile-conditional conda check and the ordering of initialisation.
- **Completion wiring:** the `email || email_on_fail` guard, the `onError` log and handler registration.
- **Channel collection:** the topic branch, `collectFile` name and `storeDir` for the versions file, and the MultiQC wiring.
- **Pipeline-specific content:** `assets/methods_description_template.yml`, the pipeline logo, `conf/igenomes*.config` and the citation prose for your tools.

:::note{title="Deleting copied code"}
Once your pipeline calls the plugin, delete the local copies, the `utils_nextflow_pipeline` and `utils_nfcore_pipeline` entries in `modules.json`, and `assets/sendmail_template.txt`. Check `nf-core pipelines lint` afterwards.
:::

## 5. Boolean flags and verified runs

From 1.0 (PR #127), `completionEmail`, `completionSummary` and `checkProfileProvided` accept the text values `true` and `false` for their flag arguments, as well as Booleans. Nothing is renamed. The wider redesign of flag handling (nf-core/nf-core-utils#113, #114 and #121) is part of 2.0. `params.force_completion_email` is still read as a Boolean.

The template migration is carried by nf-core/tools#3870. Candidate evidence will come from the generated template, nf-core/rnaseq and nf-core/createpanelrefs.

:::warning{title="TODO: verified before and after run"}
Not done. The before and after examples above have been syntax-checked only. They have not been run against an installed 1.0 candidate with a migrated template (nf-core/nf-core-utils#119). It is also not yet confirmed that the plugin registry accepts pre-release versions such as `1.0.0-rc.1`. Verified run evidence (candidate commit, Nextflow version, observable outputs) will replace this note.
:::

## 6. Pinning back

To return to a known-good combination, pin the plugin and restore the matching template code:

```groovy
plugins {
    id 'nf-core-utils@0.7.0'
}
```

Pin the version in `nextflow.config` and revert the migration commit so the template-local helpers return. Remember that a pipeline on 0.7.0 still needs Nextflow 25.10.4 or newer.

## 7. Not in 1.0

Removal of the deprecated version helpers and any renames are part of the 2.0 migration (nf-core/nf-core-utils#123), not this guide.
