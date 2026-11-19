package nfcore.plugin.nfcore

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Coerces boolean pipeline parameters, which reach the plugin as Strings when set on the command line.
 */
@Slf4j(category = 'nextflow.nfcore.BooleanParam')
@CompileStatic
class BooleanParam {

    /**
     * Converts a Boolean or case-insensitive "true"/"false" string.
     * Null uses the default; any other value warns and uses the default, so a typo
     * does not drop a completion notification.
     */
    static boolean coerce(Object value, boolean defaultValue, String name = 'value') {
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
        log.warn("Expected ${name} to be true or false, got: ${value} (${value.getClass().simpleName}); using ${defaultValue}")
        return defaultValue
    }
}
