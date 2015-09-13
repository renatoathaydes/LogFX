package com.athaydes.logfx.config

import javafx.scene.paint.Color
import spock.lang.Specification
import spock.lang.Unroll

import java.util.regex.Pattern

@Unroll
class ConfigSpec extends Specification {

    def "Can parse highlight expressions from config"() {
        when:
        def result = Config.parseHighlightExpression( expression )

        then:
        result.bkgColor == expectedBkg
        result.fillColor == expectedFill
        result.pattern.toString() == Pattern.compile( expectedExpression ).toString()

        where:
        expression                   | expectedExpression | expectedBkg     | expectedFill
        'aliceBlue aquamarine .*hej' | '.*hej'            | Color.ALICEBLUE | Color.AQUAMARINE
        'blue   yellow .*d.* x'      | '.*d.* x'          | Color.BLUE      | Color.YELLOW
        'red white   a b  c'         | 'a b  c'           | Color.RED       | Color.WHITE
    }

    def "Errors when parsing invalid highlight expressions"() {
        when:
        Config.parseHighlightExpression( expression )

        then:
        IllegalArgumentException exception = thrown()
        exception.message.contains( expectedPartialError )

        where:
        expression         | expectedPartialError
        ''                 | 'empty highlight expression'
        ' '                | 'highlight expression contains invalid spaces at start/end'
        ' red'             | 'highlight expression contains invalid spaces at start/end'
        'red '             | 'highlight expression contains invalid spaces at start/end'
        'red'              | 'fill color and regular expression not specified'
        'wrong'            | 'fill color and regular expression not specified'
        'wrong white .*.*' | 'invalid background color'
        'red white'        | 'regular expression not specified'
        'red wrong'        | 'regular expression not specified'
        'red wrong .*.*'   | 'invalid fill color'
        'red white ++'     | 'cannot parse regular expression'
    }


}
