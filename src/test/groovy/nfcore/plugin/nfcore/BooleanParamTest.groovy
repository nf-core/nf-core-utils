package nfcore.plugin.nfcore

import spock.lang.Issue
import spock.lang.Specification

@Issue("https://github.com/nf-core/nf-core-utils/issues/94")
class BooleanParamTest extends Specification {

    def 'coerce reads Booleans and case-insensitive true/false text: #value'() {
        expect:
        BooleanParam.coerce(value, !expected) == expected

        where:
        value     || expected
        true      || true
        false     || false
        'true'    || true
        'false'   || false
        'TRUE'    || true
        ' False ' || false
    }

    def 'coerce returns the default for null and for garbage: #value'() {
        expect:
        BooleanParam.coerce(value, defaultValue, 'monochrome_logs') == defaultValue

        where:
        value | defaultValue
        null  | true
        null  | false
        'yes' | true
        'yes' | false
        ''    | true
        42    | false
    }
}
