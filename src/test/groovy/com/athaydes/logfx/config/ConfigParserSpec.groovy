package com.athaydes.logfx.config

import com.athaydes.logfx.text.HighlightExpression
import javafx.geometry.Orientation
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

    def "Can parse log lines from config"() {
        when: 'a log-file line is parsed'
        def logLine = ConfigParser.parseLogFileLine( line )

        then: 'the expected LogFile is obtained'
        logLine.file == new File( expectedFile )
        logLine.use( { f -> null }, { f -> f.highlighGroupName } ) == expectedGroup

        where:
        line                   | expectedFile        | expectedGroup
        '  hello'              | 'hello'             | null
        'hello  '              | 'hello'             | null
        ' /var/log/serv.log'   | '/var/log/serv.log' | null
        '  []hello'            | 'hello'             | ''
        '  [my group]/var/log' | '/var/log'          | 'my group'
        '  [gr]  /var/log'     | '/var/log'          | 'gr'
        '  [gr]  /var/log[0]'  | '/var/log[0]'       | 'gr'
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

    def "Can parse simple V2 configuration"() {
        given: 'A simple V2 Config'
        def sampleConfig = '''\
        |version:
        |  V2
        |standard-log-colors:
        |  0x000000ff 0xd3d3d3ff
        |expressions:
        |  0xbd1a80ff 0xe6e6e6ff true =====
        |  0xe86b08ff 0x000000ff true wget
        |  0x1a3399ff 0xffccb3ff true OFF
        |filters:
        |  disable
        |files:
        |  /android-studio/Install-Linux-tar.txt
        |  /home/me/.logfx/config
        |gui:
        |  orientation HORIZONTAL
        |  pane-dividers 0.4840163934426229
        |  font 13.0 DejaVu Sans
        |'''.stripMargin()

        when: 'the config is parsed'
        def config = new ConfigProperties()
        new ConfigParser( config ).parseConfigFile( null, sampleConfig.split( '\n' ).iterator() )

        then: 'all configuration is loaded'
        config.standardLogColors.value.background == Color.BLACK
        config.standardLogColors.value.fill == Color.LIGHTGRAY

        def colorMap = config.highlightGroups.toMap()
        colorMap.keySet() == [ '' ] as Set
        def defaultHighlights = colorMap[ '' ]

        defaultHighlights.size() == 3
        defaultHighlights[ 0 ] == new HighlightExpression( '=====', Color.valueOf( '0xbd1a80ff' ), Color.valueOf( '0xe6e6e6ff' ), true )
        defaultHighlights[ 1 ] == new HighlightExpression( 'wget', Color.valueOf( '0xe86b08ff' ), Color.valueOf( '0x000000ff' ), true )
        defaultHighlights[ 2 ] == new HighlightExpression( 'OFF', Color.valueOf( '0x1a3399ff' ), Color.valueOf( '0xffccb3ff' ), true )

        !config.enableFilters.get()
        config.observableFiles.collect { it.file.path }.toSet() ==
                [ '/android-studio/Install-Linux-tar.txt', '/home/me/.logfx/config' ] as Set

        config.panesOrientation.get() == Orientation.HORIZONTAL
        config.paneDividerPositions.collect() == [ 0.4840163934426229 as double ]
        config.font.value?.name == 'DejaVu Sans'
        config.font.value?.size == 13.0 as double
    }

    def "Can parse simple V2 configuration with named highlight rules"() {
        given: 'A V2 Config containing named highlight rules'
        def sampleConfig = '''\
        |version:
        |  V2
        |standard-log-colors:
        |  0x000000ff 0xd3d3d3ff
        |expressions:
        |  0xbd1a80ff 0xe6e6e6ff true =====
        |expressions:
        |  @name@Extra Rules
        |  0xe86b08ff 0x000000ff true wget
        |filters:
        |  disable
        |files:
        |  /home/me/.logfx/config
        |gui:
        |  orientation HORIZONTAL
        |  pane-dividers 0.4840163934426229
        |  font 13.0 DejaVu Sans
        |'''.stripMargin()

        when: 'the config is parsed'
        def config = new ConfigProperties()
        new ConfigParser( config ).parseConfigFile( null, sampleConfig.split( '\n' ).iterator() )

        then: 'all configuration is loaded'
        config.standardLogColors.value.background == Color.BLACK
        config.standardLogColors.value.fill == Color.LIGHTGRAY

        def colorMap = config.highlightGroups.toMap()
        colorMap.keySet() == [ '', 'Extra Rules' ] as Set
        def defaultHighlights = colorMap[ '' ]

        defaultHighlights.size() == 1
        defaultHighlights[ 0 ] == new HighlightExpression( '=====', Color.valueOf( '0xbd1a80ff' ), Color.valueOf( '0xe6e6e6ff' ), true )

        def extraHighlights = colorMap[ 'Extra Rules' ]
        extraHighlights.size() == 1
        extraHighlights[ 0 ] == new HighlightExpression( 'wget', Color.valueOf( '0xe86b08ff' ), Color.valueOf( '0x000000ff' ), true )

        !config.enableFilters.get()
        config.observableFiles.collect { it.file.path }.toSet() ==
                [ '/home/me/.logfx/config' ] as Set

        config.panesOrientation.get() == Orientation.HORIZONTAL
        config.paneDividerPositions.collect() == [ 0.4840163934426229 as double ]
        config.font.value?.name == 'DejaVu Sans'
        config.font.value?.size == 13.0 as double

        and: 'The highlightGroups can return the correct rules by group name'
        config.highlightGroups.getDefault().collect() == defaultHighlights
        config.highlightGroups.getByName( '' ).collect() == defaultHighlights
        config.highlightGroups.getByName( 'Extra Rules' )?.collect() == extraHighlights
    }


}
