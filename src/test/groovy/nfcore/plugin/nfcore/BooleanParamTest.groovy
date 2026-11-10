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
