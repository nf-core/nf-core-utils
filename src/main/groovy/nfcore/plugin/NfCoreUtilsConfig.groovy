package nfcore.plugin

import nextflow.config.schema.ConfigOption
import nextflow.config.schema.ConfigScope
import nextflow.config.schema.ScopeName
import nextflow.script.dsl.Description

/**
 * Configuration scope for nf-core utilities
 */
@ScopeName('nfcoreutils')
@Description('''
    The `nfcoreutils` scope allows you to configure the nf-core utilities plugin.
''')
class NfCoreUtilsConfig implements ConfigScope {

    NfCoreUtilsConfig(Map opts) {
        this.checks = opts.checks instanceof Map ? opts.checks : [:]
    }

    @ConfigOption
    @Description('Configuration options for automatic pipeline checks')
    Map checks
} 