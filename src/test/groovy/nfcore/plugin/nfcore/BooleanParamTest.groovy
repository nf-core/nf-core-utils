package nfcore.plugin.nfcore

import spock.lang.Specification
import spock.lang.Unroll

class BooleanParamTest extends Specification {

    @Unroll
    def "coerce(#input) returns #expected"() {
        expect:
        BooleanParam.coerce(input) == expected

        where:
        input   | expected
        true    | true
        false   | false
        'true'  | true
        'false' | false
        'TRUE'  | true
        ' false ' | false
    }

    def "coerceOrWarn returns the default for an unrecognised value"() {
        expect:
        BooleanParam.coerceOrWarn('yes', true, 'x') == true
        BooleanParam.coerceOrWarn('yes', false, 'x') == false
        BooleanParam.coerceOrWarn('false', true, 'x') == false
    }

    def "coerce names the parameter in its error"() {
        when:
        BooleanParam.coerce('yes', false, 'plaintextEmail')

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains('plaintextEmail')
    }

    def "coerce uses the default for null"() {
        expect:
        BooleanParam.coerce(null) == false
        BooleanParam.coerce(null, true) == true
    }

    @Unroll
    def "coerce rejects #input"() {
        when:
        BooleanParam.coerce(input)

        then:
        thrown(IllegalArgumentException)

        where:
        input << ['yes', '', 1, []]
    }
}
