package nfcore.plugin.nfcore

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Coerces boolean pipeline parameters, including strings from untyped strict-syntax CLI params.
 */
@Slf4j(category = 'nextflow.nfcore.BooleanParam')
@CompileStatic
class BooleanParam {

    /**
     * Converts a Boolean or case-insensitive "true"/"false" string; null uses the default.
     * @throws IllegalArgumentException for other values, naming the parameter
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

    /** Warns and uses the default for invalid values so completion notifications are not dropped. */
    static boolean coerceOrWarn(Object value, boolean defaultValue, String name) {
        try {
            return coerce(value, defaultValue, name)
        } catch (IllegalArgumentException e) {
            log.warn("${e.message}; using ${defaultValue}")
            return defaultValue
        }
    }
}
