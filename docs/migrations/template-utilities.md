# Migrating from the template utilities

This guide is for pipelines built from the nf-core pipeline template that carry their own copies of the template helpers (`getWorkflowVersion`, `dumpParametersToJSON`, `completionEmail`, `getGenomeAttribute` and others) and the `utils_nextflow_pipeline` and `utils_nfcore_pipeline` subworkflows. It replaces those copies with imports from nf-core-utils 1.0. Most call sites keep their shape; a few change, and a few behave differently. Each section below shows the template code, the plugin call, and what differs.

Pipelines that already load a 0.x release of the plugin should read [Migrating to 1.0](1.0.md) instead.

:::warning{title="Pending a verified run"}
The examples are syntax-checked only. They have not yet been run against an installed 1.0 candidate with a migrated template (nf-core/nf-core-utils#119). Items marked "In 1.0 (PR ..., not merged yet)" describe pull requests that are still open.
:::

## Requirements

- Nextflow 25.10.4 or newer. CI tests the minimum and every supported stable release line (25.10, 26.04 and 26.10).
- nf-core-utils 1.0 in `nextflow.config`:

```groovy
plugins {
    id 'nf-core-utils@1.0.0'
}
```

The template migration itself is carried by nf-core/tools#3870.

:::note{title="Migrating with an agent"}
The [`nf-core-utils-migration`](https://github.com/nf-core/nf-core-utils/tree/main/skills/nf-core-utils-migration) skill walks a coding agent through this migration on a real pipeline: inventory, signatures from the pinned plugin, the replacements, external `plugins {}` blocks, verification and the PR. Install it into the pipeline with the [skills CLI](https://github.com/vercel-labs/skills):

```bash
npx skills add nf-core/nf-core-utils --skill nf-core-utils-migration
```

:::

## Updating your pipeline

Import from the plugin in the file that calls the function:

```nextflow
include { checkConfigProvided; checkProfileProvided; getWorkflowVersion; dumpParametersToJSON; checkCondaChannels; collectVersions; paramsSummaryMultiqc; methodsDescriptionText; completionEmail; completionSummary; getGenomeAttribute; genomeExistsError; logColours } from 'plugin/nf-core-utils'
```

The "before" blocks show the shape of the template code, not a verbatim copy. The template at nf-core/tools `9ae33b4` is the reference.

:::note{title="Template pins lag nf-core/modules"}
The template pins older copies of the utils subworkflows than nf-core/modules master. Compare against the pin in your `modules.json`.
:::

### Startup checks

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

`checkConfigProvided()` returns a boolean. `checkProfileProvided(List args, Object monochromeLogs = true)` throws an `AbortOperationException` with the same `ERROR ~ ...` text as the template's `error()`. Note the `monochromeLogs` default is `true`.

:::note{title="One set of startup messages"}
Keep the call where the template made it, and call it once, so startup messages appear once.
:::

### Workflow version

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

### Parameters as JSON

Before:

```nextflow
dumpParametersToJSON(params.outdir)
```

After, `dumpParametersToJSON(String outdir, Map params)`. **The params map is now an explicit second argument:**

```nextflow
dumpParametersToJSON(params.outdir, params)
```

A null `outdir` returns without writing. Errors are logged, not thrown, and the temporary file is written under `java.io.tmpdir`.

### Conda channels

Before:

```nextflow
checkCondaChannels()
```

After, `checkCondaChannels()` returns a boolean and adds a warning when no channels are found:

```nextflow
def channels_ok = checkCondaChannels()
```

The decision to run the check when a conda profile is active stays in the pipeline.

### Software versions

Before, topic tuples grouped and written per process:

```nextflow
topic_versions = channel.topic('versions')
    .map { process, tool, version -> [process, "${tool}: ${version}"] }
    .unique()
    .map { process, line -> "${process}:\n  ${line}" }
```

After, `collectVersions(Object input, Object nextflowVersion = null)`. **Collect the channel to a list first;** one call handles topic tuples, records from typed processes, `Path` objects and strings:

```nextflow
channel.topic('versions')
    .toList()
    .map { collected -> collectVersions(collected) }
```

:::warning{title="Output differs"}
The result is **one merged YAML string**, not a channel of per-process strings. Name and store the file in the pipeline (the template's `collectFile` name and `storeDir` stay pipeline-owned). The `Workflow` block comes last, and the Nextflow version resolves from `nextflow.version` in config, then `NXF_VER`, then the running version.
:::

:::warning{title="Version quoting"}
`collectVersions()` writes YAML with SnakeYAML, which quotes numeric-looking versions (`fastqc: '2.0'`, `ten: '1.10'`). The template writes topic versions bare. Migrated pipelines will see a one-time change in the versions file and in nf-test snapshots.

In 1.0 (PR #129, not merged yet), versions read from a legacy `versions.yml` keep their text: `1.20` stays `1.20` instead of becoming `1.2` (nf-core/tools#4500). Those values render unquoted, as before.
:::

### MultiQC parameter summary

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

### Methods description

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

The plugin defaults `tool_citations` to `No tools used in the workflow.` and `tool_bibliography` to `No bibliography entries found.`, where the template defaults both to an empty string. Pass the `meta` override above to keep the template output. The template file itself stays in the pipeline, and may be on a remote file system.

### Completion handlers

Before:

```nextflow
completionEmail(summary_params, email, email_on_fail, plaintext_email, outdir, monochrome_logs, multiqc_report.toList())
completionSummary(monochrome_logs)
```

After, the same calls with the plugin signatures `completionEmail(Map summaryParams, String email, String emailOnFail, Object plaintextEmail, String outdir, Object monochromeLogs, List multiqcReports)` and `completionSummary(Object monochromeLogs)`. The flag arguments accept a Boolean or the text `true` or `false` (any case), so values set on the command line work. Any other value logs a warning and uses the default. Nothing is renamed:

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
- Delivery goes through Nextflow's `sendMail()` with a 30-second limit, configured by the `mail` scope (SMTP or SES) as for `nextflow run -N`. A delivery failure is a warning, not a failure.
- Without a pipeline copy of `assets/email_template.{txt,html}`, the plugin falls back to placeholder content.
:::

:::note{title="Bundled email templates"}
In 1.0 (PR #126, not merged yet; nf-core/nf-core-utils#118) the plugin bundles default HTML and text email templates, adapted from the pipeline template. A pipeline can then delete its generic `assets/email_template.html` and `assets/email_template.txt`. To customize, keep either file in `assets/`: each format uses the pipeline file when present and the bundled default otherwise. The bundled HTML shows the logo only when the pipeline has one. Slack and Teams JSON templates are not bundled yet.
:::

### Genome attributes and references

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
params.fasta = getGenomeAttribute('fasta')
```

The plugin reads `params` from the session config and needs `genome` to be a string.

For reference datasheets, the named-argument functions `getReferencePath(Map args)`, `getReferenceMetadata(Map args)` and `getReferenceAttribute(Map args)` have no template equivalent.

### Genome check and log colours

- Replace the local `genomeExistsError()` with the plugin's. It takes no arguments, reads `params` from the session and throws an `AbortOperationException` with the template's message when `params.genome` is not a key of `params.genomes`. A null genome or an empty `genomes` map passes.
- Replace the local `logColours(params.monochrome_logs)` with the same call imported from the plugin. `logColours(boolean monochromeLogs = true)` returns the template's map of colour names to escape codes, all empty when `monochromeLogs` is true.

`sectionLogs`, `workflowSummaryMQC` and `getSingleReport` stay internal. Keep local copies if you use them.

### Citations

The template's `toolCitationText()` and `toolBibliographyText()` are hard-coded prose stubs and stay pipeline-owned. To generate citations from module `meta.yml` files, use the opt-in functions `toolCitationText(Map collectedCitations)`, `autoToolCitationText(List topicCitations = [])`, `citationsOnTheFly(List topicVersions, List<String> metaFilePaths, List<String> extraTools = [])` and `citationOutputs(Map citations, List<String> formats = ['html'], Object mqcMethodsYaml = null)`. `toolCitationText()` returns HTML, so write citation files as `.html`.

## What stays pipeline-owned

The plugin does not replace these:

- **nf-schema:** `validateParameters`, `paramsHelp`, `paramsSummaryLog`, `paramsSummaryMap`, `samplesheetToList` and the `UTILS_NFSCHEMA_PLUGIN` wrapper.
- **Samplesheet checks:** `validateInputSamplesheet` and the endedness check.
- **Startup control flow:** the `--version` early exit, the help exit, the profile-conditional conda check and the ordering of initialisation.
- **Completion wiring:** the `email || email_on_fail` guard, the `onError` log and handler registration.
- **Channel collection:** the topic branch, `collectFile` name and `storeDir` for the versions file, and the MultiQC wiring.
- **Pipeline-specific content:** `assets/methods_description_template.yml`, the pipeline logo, `conf/igenomes*.config` and the citation prose for your tools.

## Removing the copied code

Once your pipeline calls the plugin, delete the local copies, the `utils_nextflow_pipeline` and `utils_nfcore_pipeline` entries in `modules.json`, and `assets/sendmail_template.txt`. Check `nf-core pipelines lint` afterwards.

## Verified runs

:::warning{title="TODO: verified before and after run"}
Not done. The before and after examples above have been syntax-checked only. They have not been run against an installed 1.0 candidate with a migrated template (nf-core/nf-core-utils#119). It is also not yet confirmed that the plugin registry accepts pre-release versions such as `1.0.0-rc.1`. Candidate evidence will come from the generated template, nf-core/rnaseq and nf-core/createpanelrefs, and verified run evidence (candidate commit, Nextflow version, observable outputs) will replace this note.
:::

## Going back

To return to the template helpers, revert the migration commit so the local copies and subworkflows return, and remove the plugin from `nextflow.config` or pin the release you had.
