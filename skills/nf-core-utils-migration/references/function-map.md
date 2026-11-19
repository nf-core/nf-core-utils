# Function map: vendored helpers to plugin calls

Signatures below match nf-core-utils 0.7.0 and nf-schema 2.7.2. Check the version you pin with the commands in SKILL.md step 2 before relying on an overload.

## Imports

```groovy
include { checkCondaChannels   } from 'plugin/nf-core-utils'
include { checkConfigProvided  } from 'plugin/nf-core-utils'
include { checkProfileProvided } from 'plugin/nf-core-utils'
include { completionEmail      } from 'plugin/nf-core-utils'
include { completionSummary    } from 'plugin/nf-core-utils'
include { dumpParametersToJSON } from 'plugin/nf-core-utils'
include { getWorkflowVersion   } from 'plugin/nf-core-utils'
include { imNotification       } from 'plugin/nf-core-utils'
include { paramsHelp           } from 'plugin/nf-schema'
include { paramsSummaryLog     } from 'plugin/nf-schema'
include { paramsSummaryMap     } from 'plugin/nf-schema'
include { samplesheetToList    } from 'plugin/nf-schema'
include { validateParameters   } from 'plugin/nf-schema'
```

In `main.nf` / `workflows/<pipeline>.nf`, as needed:

```groovy
include { getGenomeAttribute     } from 'plugin/nf-core-utils'
include { methodsDescriptionText } from 'plugin/nf-core-utils'
include { paramsSummaryMultiqc   } from 'plugin/nf-core-utils'
include { softwareVersionsToYAML } from 'plugin/nf-core-utils'
```

## UTILS_NEXTFLOW_PIPELINE

```groovy
// before
UTILS_NEXTFLOW_PIPELINE(
    version,
    true,
    outdir,
    workflow.profile.tokenize(',').intersect(['conda', 'mamba']).size() >= 1,
)

// after
if (version) {
    log.info("${workflow.manifest.name} ${getWorkflowVersion()}")
    System.exit(0)
}
if (outdir) {
    dumpParametersToJSON(outdir, params)
}
if (workflow.profile.tokenize(',').intersect(['conda', 'mamba']).size() >= 1) {
    checkCondaChannels()
}
```

## UTILS_NFSCHEMA_PLUGIN

```groovy
// before
UTILS_NFSCHEMA_PLUGIN(workflow, validate_params, null, help, help_full, show_hidden, before_text, after_text, command, null)

// after
if (help || help_full) {
    log.info(
        paramsHelp(
            [
                beforeText: before_text,
                afterText: after_text,
                command: command,
                showHidden: show_hidden,
                fullHelp: help_full,
            ],
            (help instanceof String && help != "true") ? help : "",
        )
    )
    System.exit(0)
}

log.info(before_text)
log.info(paramsSummaryLog(workflow, parameters_schema: "nextflow_schema.json"))
log.info(after_text)

if (validate_params) {
    validateParameters(parameters_schema: "nextflow_schema.json")
}
```

nf-schema 2.x overloads: `paramsSummaryLog(Map, WorkflowMetadata)`, `paramsSummaryLog(WorkflowMetadata)`, same for `paramsSummaryMap`. nf-schema 3.x drops the `WorkflowMetadata` argument (`paramsSummaryLog(Map)`, `paramsSummaryLog()`).

## UTILS_NFCORE_PIPELINE

```groovy
// before
UTILS_NFCORE_PIPELINE(nextflow_cli_args)

// after
checkConfigProvided()
checkProfileProvided(nextflow_cli_args)          // or (nextflow_cli_args, monochrome_logs)
```

Completion helpers keep their call shape, only the import changes:

```groovy
completionEmail(summary_params, email, email_on_fail, plaintext_email, outdir, monochrome_logs, multiqc_reports)
completionSummary(monochrome_logs)
imNotification(summary_params, hook_url)
```

## Local helpers in main.nf / workflows

| Local helper                                                             | Plugin call                                                                                                       |
| ------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------- |
| `def getGenomeAttribute(attribute)`                                      | `getGenomeAttribute('fasta')`                                                                                     |
| `softwareVersionsToYAML(ch_versions)`                                    | `softwareVersionsToYAML(softwareVersions: channel.topic("versions"), nextflowVersion: workflow.nextflow.version)` |
| `def paramsSummaryMultiqc(summary_params)`                               | `paramsSummaryMultiqc(summary_params)`                                                                            |
| `def methodsDescriptionText(mqc_methods_yaml)` with empty tool citations | `methodsDescriptionText(mqc_methods_yaml.toString())`                                                             |
| `processVersionsFromFile` (rnavar#233 era)                               | replaced by `softwareVersionsToYAML(softwareVersions: ...)` from 0.4.0                                            |

`toolCitationText()` / `toolBibliographyText()` template stubs can be deleted when they are still the unfilled template versions. Filling citations from module `meta.yml` (`citationsOnTheFly`) is a separate change, see `citations.md`.
