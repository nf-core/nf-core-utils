package nfcore.plugin.nfcore

import groovy.transform.CompileStatic

/**
 * Coerces pipeline parameter values to booleans.
 *
 * Under Nextflow's v2 syntax parser, command-line flags such as `--plaintext_email`
 * reach pipeline code as the strings "true" or "false", not as Booleans.
 */
@CompileStatic
class BooleanParam {

    static boolean coerce(Object value, boolean defaultValue = false) {
        if (value == null) {
            return defaultValue
        }
        if (value instanceof Boolean) {
            return (Boolean) value
        }
        if (value instanceof CharSequence) {
            def text = value.toString().trim().toLowerCase()
            if (text == 'true') {
                return true
            }
            if (text == 'false') {
                return false
            }
        }
        throw new IllegalArgumentException("Expected a boolean value (true or false), got: ${value} (${value.getClass().simpleName})")
    }
}
