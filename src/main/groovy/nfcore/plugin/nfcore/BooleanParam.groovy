package nfcore.plugin.nfcore

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Coerces pipeline parameter values to booleans.
 *
 * Under Nextflow's strict syntax parser, command-line values for params that are not declared
 * with a type reach pipeline code as strings, so `--plaintext_email` arrives as "true".
 */
@Slf4j(category = 'nextflow.nfcore.BooleanParam')
@CompileStatic
class BooleanParam {

    /**
     * Converts a Boolean, or the string "true" or "false" (case-insensitive), to a boolean.
     * @param value The value to convert; null returns the default
     * @param defaultValue Result for null
     * @param name Parameter name used in the error message
     * @throws IllegalArgumentException if the value is neither a Boolean nor "true" / "false"
     */
    static boolean coerce(Object value, boolean defaultValue = false, String name = 'value') {
        if (value == null) {
            return defaultValue
        }
        if (value instanceof Boolean) {
            return (Boolean) value
        }
        if (value instanceof CharSequence) {
            def text = value.toString().trim()
            if (text.equalsIgnoreCase('true')) {
                return true
            }
            if (text.equalsIgnoreCase('false')) {
                return false
            }
        }
        throw new IllegalArgumentException("Expected ${name} to be true or false, got: ${value} (${value.getClass().simpleName})")
    }

    /**
     * Like {@link #coerce}, but logs a warning and returns the default for an unrecognised value.
     * Used where a failure would silently drop work, such as completion notifications.
     */
    static boolean coerceOrWarn(Object value, boolean defaultValue, String name) {
        try {
            return coerce(value, defaultValue, name)
        } catch (IllegalArgumentException e) {
            log.warn("${e.message}; using ${defaultValue}")
            return defaultValue
        }
    }
}
