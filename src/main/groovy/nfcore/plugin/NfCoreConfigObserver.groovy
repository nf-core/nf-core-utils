package nfcore.plugin

import nextflow.Session
import nextflow.trace.TraceObserverV2
import nextflow.processor.TaskHandler
import nextflow.trace.TraceRecord

/**
 * Trace observer that performs nf-core specific config and profile checks
 * at the beginning of workflow execution.
 */
class NfCoreConfigObserver implements TraceObserverV2 {
    private final List nextflow_cli_args

    NfCoreConfigObserver(Session session, List nextflow_cli_args) {
        this.nextflow_cli_args = nextflow_cli_args
    }

    @Override
    void onFlowCreate(Session session) {
        checkConfigProvided(session)
        checkProfileProvided(session)
    }

    /**
     * Warn if a -profile or Nextflow config has not been provided to run the pipeline
     */
    private void checkConfigProvided(Session session) {
        if (session.workflowProfile == 'standard' && session.configFiles.size() <= 1) {
            def name = session.config.navigate('workflow.manifest.name')
            System.err.println(
                "[${name}] You are attempting to run the pipeline without any custom configuration!\n\n" +
                "This will be dependent on your local compute environment but can be achieved via one or more of the following:\n" + 
                "   (1) Using an existing pipeline profile e.g. `-profile docker` or `-profile singularity`\n" + 
                "   (2) Using an existing nf-core/configs for your Institution e.g. `-profile crick` or `-profile uppmax`\n" + 
                "   (3) Using your own local custom config e.g. `-c /path/to/your/custom.config`\n\n" + 
                "Please refer to the quick start section and usage docs for the pipeline.\n "
            )
        }
    }

    /**
     * Exit pipeline if --profile contains spaces
     */
    private void checkProfileProvided(Session session) {
        if (session.workflowProfile.endsWith(',')) {
            throw new RuntimeException(
                "The `-profile` option cannot end with a trailing comma, please remove it and re-run the pipeline!\n" + 
                "HINT: A common mistake is to provide multiple values separated by spaces e.g. `-profile test, docker`.\n"
            )
        }
        if (nextflow_cli_args && nextflow_cli_args[0]) {
            System.err.println(
                "nf-core pipelines do not accept positional arguments. The positional argument `${nextflow_cli_args[0]}` has been detected.\n" + 
                "HINT: A common mistake is to provide multiple values separated by spaces e.g. `-profile test, docker`.\n"
            )
        }
    }

    // Implement V2 methods
    @Override void onProcessSubmit(String name, String process, int attempt) {}
    @Override void onProcessStart(String name, String process, int attempt, String module, String container, String cpus, String memory, String disk, String time, String queue, Map env) {}
    @Override void onProcessComplete(String name, String process, int attempt, String script, String status, long submit, long start, long complete, String module, String container, String cpus, String memory, String disk, String time, String queue, Map env, Map errorAction, String machineType, String cloudZone, double cost, String errorMessage) {}
    @Override void onProcessCached(String name, String process, int attempt, String module, String container, String cpus, String memory, String disk, String time, String queue, Map env, String machineType, String cloudZone, double cost) {}
    @Override void onFlowComplete() {}
} 