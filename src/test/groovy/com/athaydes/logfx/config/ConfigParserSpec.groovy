package com.athaydes.logfx.config

import javafx.scene.paint.Color
import spock.lang.Specification
import spock.lang.Unroll

import java.util.regex.Pattern

import static com.athaydes.logfx.config.ConfigParser.ConfigVersion.V1
import static com.athaydes.logfx.config.ConfigParser.ConfigVersion.V2

@Unroll
class ConfigParserSpec extends Specification {

    def "Can parse highlight expressions from config (V1)"() {
        when: 'highlight expressions in V1 format are parsed'
        def result = ConfigParser.parseHighlightExpression( expression, V1 )

        then: 'the expected values are obtained'
        result.bkgColor == expectedBkg
        result.fillColor == expectedFill
        result.pattern.toString() == Pattern.compile( expectedExpression ).toString()
        result.filtered == expectedFiltered

        where:
        expression                   | expectedExpression | expectedBkg     | expectedFill     | expectedFiltered
        'aliceBlue aquamarine .*hej' | '.*hej'            | Color.ALICEBLUE | Color.AQUAMARINE | false
        'blue   yellow .*d.* x'      | '.*d.* x'          | Color.BLUE      | Color.YELLOW     | false
        'red white   a b  c'         | 'a b  c'           | Color.RED       | Color.WHITE      | false
    }

    def "Can parse highlight expressions from config (V2)"() {
        when: 'highlight expressions in V2 format are parsed'
        def result = ConfigParser.parseHighlightExpression( expression, V2 )

        then: 'the expected values are obtained'
        result.bkgColor == expectedBkg
        result.fillColor == expectedFill
        result.pattern.toString() == Pattern.compile( expectedExpression ).toString()
        result.filtered == expectedFiltered

        where:
        expression                         | expectedExpression | expectedBkg     | expectedFill     | expectedFiltered
        'aliceBlue aquamarine false .*hej' | '.*hej'            | Color.ALICEBLUE | Color.AQUAMARINE | false
        'blue   yellow true .*d.* x'       | '.*d.* x'          | Color.BLUE      | Color.YELLOW     | true
        'red white true  a b  c'           | 'a b  c'           | Color.RED       | Color.WHITE      | true
        'red white false  a b  c'          | 'a b  c'           | Color.RED       | Color.WHITE      | false
    }

    def "Errors when parsing invalid highlight expressions"() {
        when:
        ConfigParser.parseHighlightExpression( expression, version )

        then:
        IllegalArgumentException exception = thrown()
        exception.message.contains( expectedPartialError )

        where:
        version | expression         | expectedPartialError
        V1      | ''                 | 'empty highlight expression'
        V1      | ' '                | 'highlight expression contains invalid spaces at start/end'
        V1      | ' red'             | 'highlight expression contains invalid spaces at start/end'
        V1      | 'red '             | 'highlight expression contains invalid spaces at start/end'
        V1      | 'red'              | 'fill color and regular expression not specified'
        V1      | 'wrong'            | 'fill color and regular expression not specified'
        V1      | 'wrong white .*.*' | 'invalid background color'
        V1      | 'red white'        | 'regular expression not specified'
        V1      | 'red wrong'        | 'regular expression not specified'
        V1      | 'red wrong .*.*'   | 'invalid fill color'
        V1      | 'red white ++'     | 'cannot parse regular expression'

        V2      | ''                 | 'empty highlight expression'
        V2      | ' '                | 'highlight expression contains invalid spaces at start/end'
        V2      | ' red'             | 'highlight expression contains invalid spaces at start/end'
        V2      | 'red '             | 'highlight expression contains invalid spaces at start/end'
        V2      | 'red'              | 'fill color and regular expression not specified'
        V2      | 'wrong'            | 'fill color and regular expression not specified'
        V2      | 'wrong white .*.*' | 'invalid background color'
        V2      | 'red white'        | 'regular expression not specified'
        V2      | 'red wrong'        | 'regular expression not specified'
        V2      | 'red wrong .*.*'   | 'invalid fill color'
        V2      | 'red white ++'     | 'regular expression not specified'
        V2      | 'red white true'   | 'regular expression not specified'
        V2      | 'red white x expr' | 'invalid value for filtered property'
    }


}
