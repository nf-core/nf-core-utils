package nfcore.plugin.nfcore

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/** Converts Boolean parameters and untyped CLI strings without Groovy truth semantics. */
@Slf4j(category = 'nextflow.nfcore.BooleanParam')
@CompileStatic
class BooleanParam {

    /**
     * Converts Booleans or trimmed, case-insensitive true/false strings; null uses the default.
     * @throws IllegalArgumentException for any other value, naming the parameter
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

    /** Warns and uses the default for invalid completion settings instead of aborting notifications. */
    static boolean coerceOrWarn(Object value, boolean defaultValue, String name) {
        try {
            return coerce(value, defaultValue, name)
        } catch (IllegalArgumentException e) {
            log.warn("${e.message}; using ${defaultValue}")
            return defaultValue
        }
    }
}
