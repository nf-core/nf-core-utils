package nfcore.plugin.nfcore

import spock.lang.*

@Title('Boolean pipeline parameters have explicit conversion rules')
@Narrative('Untyped CLI flags must behave like typed Booleans, never Groovy string truth.')
@Issue('https://github.com/nf-core/nf-core-utils/issues/94')
@See('https://github.com/nf-core/nf-core-utils/blob/main/ARCHITECTURE.md#boolean-cli-parameter-compatibility')
class BooleanParamTest extends Specification {
    def 'coerce #value with default #fallback returns #expected'() {
        expect:
        BooleanParam.coerce(value, fallback, 'flag') == expected

        where:
        value                  | fallback | expected
        true                   | false    | true
        false                  | true     | false
        'TrUe'                 | false    | true
        'FALSE'                | true     | false
        ' false '              | true     | false
        new StringBuilder('true') | false  | true
        null                   | false    | false
        null                   | true     | true
    }

    def 'invalid #value is rejected instead of converted using Groovy truth'() {
        when:
        BooleanParam.coerce(value, false, 'flag')

        then:
        def error = thrown(IllegalArgumentException)
        error.message.contains('Expected flag to be true or false')

        where:
        value << ['yes', '', '0', 0, 1, [], [:]]
    }

    def 'warning conversion respects both fallback values'() {
        expect:
        BooleanParam.coerceOrWarn('invalid', true, 'flag')
        !BooleanParam.coerceOrWarn('invalid', false, 'flag')
        !BooleanParam.coerceOrWarn('false', true, 'flag')
        BooleanParam.coerceOrWarn(null, true, 'flag')
    }
}
