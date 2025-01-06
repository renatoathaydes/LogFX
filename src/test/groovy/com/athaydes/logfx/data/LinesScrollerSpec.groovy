package com.athaydes.logfx.data

import spock.lang.Specification
import spock.lang.Subject

class LinesScrollerSpec extends Specification {

    def lines = ( 1..100 ).collect { it.toString() }

    @Subject
    def lineScroller = new LinesScroller( 100, { Integer i -> lines[ i ] },
            new LinesSetter( { List<LinesSetter.LineChange> changes ->
                for ( change in changes ) {
                    lines.set change.index(), change.text()
                }
            } ) )

    def 'LineScroller must be able to scroll by adding lines on the top'() {
        when: '10 lines are added to the top of the list'
        lineScroller.topLines = ( 'a'..'j' ).toList()

        then: 'the list scrolls up by pushing its lines down to accommodate the new lines on the top'
        lines.size() == 100
        lines[ 0..9 ] == ( 'a'..'j' ).toList()
        lines[ 10..99 ] == ( 1..90 ).collect { it.toString() }
    }

    def 'LineScroller must be able to scroll by adding lines on the bottom'() {
        when: '10 lines are added to the bottom of the list'
        lineScroller.bottomLines = ( 'a'..'j' ).toList()

        then: 'the list scrolls down by pushing its lines up to accommodate the new lines on the bottom'
        lines.size() == 100
        lines[ 0..89 ] == ( 11..100 ).collect { it.toString() }
        lines[ 90..99 ] == ( 'a'..'j' ).toList()
    }

    def 'Adding more lines that a LineScroller can hold on the top results in the top part of the lines being accepted'() {
        when: 'more lines that the scroller can hold are added on the top'
        lineScroller.topLines = ( 1000..1140 ).collect { it.toString() }

        then: 'the top lines of the added lines should be accepted'
        lines.size() == 100
        lines == ( 1000..1099 ).collect { it.toString() }
    }

    def 'Adding more lines that a LineScroller can hold on the bottom results in the bottom part of the lines being accepted'() {
        when: 'more lines that the scroller can hold are added on the bottom'
        lineScroller.bottomLines = ( 250..500 ).collect { it.toString() }

        then: 'the bottom lines of the added lines should be accepted'
        lines.size() == 100
        lines == ( 401..500 ).collect { it.toString() }
    }

}
