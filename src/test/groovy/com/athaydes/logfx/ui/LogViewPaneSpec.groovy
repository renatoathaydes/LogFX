package com.athaydes.logfx.ui

import spock.lang.Specification
import spock.lang.Unroll

class LogViewPaneSpec extends Specification {

    @Unroll
    def "LogViewPane can convert a line number to a reasonable v-value to scroll to"() {
        when: 'A line number is converted into a v-value'
        def result = LogViewPane.lineNumberToScrollVvalue( lineNumber )

        then: 'The result is a reasonable v-value to make the line number always visible'
        result == expectedVvalue

        where:
        lineNumber || expectedVvalue
        1          || 0.0
        2          || 0.0
        3          || 0.0
        9          || 0.0
        10         || 0.1
        11         || 0.1
        19         || 0.1
        20         || 0.2
        21         || 0.2
        22         || 0.2
        29         || 0.2
        30         || 0.3
        31         || 0.3
        39         || 0.3
        40         || 0.4
        41         || 0.4
        49         || 0.4
        50         || 0.5

        // start to round up
        51         || 0.6
        52         || 0.6
        59         || 0.6
        60         || 0.6
        61         || 0.7
        69         || 0.7
        70         || 0.7
        71         || 0.8
        72         || 0.8
        79         || 0.8
        80         || 0.8
        81         || 0.9
        89         || 0.9
        90         || 0.9
        91         || 1.0
        99         || 1.0
        100        || 1.0
    }

}
