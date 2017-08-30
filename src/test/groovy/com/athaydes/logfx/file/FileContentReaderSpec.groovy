package com.athaydes.logfx.file

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files

class FileContentReaderSpec extends Specification {

    @Shared
    File file = Files.createTempFile( 'file-content-reader', '.log' ).toFile()

    def setup() {
        file.delete()
        file.deleteOnExit()
    }

    def "Can read the tail of a single-line file spanning multiple buffers"() {
        given: 'a file reader with a short byte buffer'
        FileContentReader reader = new FileReader( file, 5, 8 )

        when: 'A file with a very long line is created'
        file << ( 'Z' * 100 )

        then: 'The file tail can be read'
        reader.tail()
        def tail = reader.refresh()
        tail.isPresent()
        tail.get() == [ 'Z' * 100 ]

    }

    def "Can read the tail of a multi-line file with some lines spanning multiple buffers"() {
        given: 'a file reader with a short byte buffer'
        FileContentReader reader = new FileReader( file, 5, 8 )

        when: 'A file with some long and short lines is created'
        file << ( 'Z' * 100 ) << '\n' << 'abc' << '\n\n' << ( 'X' * 10 ) << '\n'

        then: 'The file tail can be read'
        reader.tail()
        def tail = reader.refresh()
        tail.isPresent()
        tail.get() == [ 'Z' * 100, 'abc', '', ( 'X' * 10 ), '' ]

    }

    @Unroll
    def "Can read the tail of a short file with a short buffer"() {
        given: 'a file reader with a short byte buffer'
        FileContentReader reader = new FileReader( file, windowSize, 8 )

        when: 'A file with 10 lines is created'
        file << ( 1..10 ).join( '\n' )

        then: 'The file tail can be read'
        reader.tail()
        def tail = reader.refresh()
        tail.isPresent()
        tail.get() == expectedLines

        where:
        windowSize || expectedLines
        1          || [ '10' ]
        2          || [ '9', '10' ]
        3          || [ '8', '9', '10' ]
        10         || ( 1..10 ).collect { it.toString() }
        100        || ( 1..10 ).collect { it.toString() }

    }

    @Unroll
    def "Can read the tail of a short file with the default buffer"() {
        given: 'a file reader with a short byte buffer'
        FileContentReader reader = new FileReader( file, windowSize )

        when: 'A file with 10 lines is created'
        file << ( 1..10 ).join( '\n' )

        then: 'The file tail can be read'
        reader.tail()
        def tail = reader.refresh()
        tail.isPresent()
        tail.get() == expectedLines

        where:
        windowSize || expectedLines
        1          || [ '10' ]
        2          || [ '9', '10' ]
        3          || [ '8', '9', '10' ]
        10         || ( 1..10 ).collect { it.toString() }
        100        || ( 1..10 ).collect { it.toString() }

    }

    @Unroll
    def "Can read the tail of a long file"() {
        given: 'a file reader with a default buffer'
        FileContentReader reader = new FileReader( file, windowSize )

        when: 'A file with 100,000 lines is created'
        file << ( 1..100_000 ).join( '\n' )

        then: 'The file tail can be read'
        reader.tail()
        def tail = reader.refresh()
        tail.isPresent()
        tail.get() == expectedLines

        where:
        windowSize || expectedLines
        1          || [ '100000' ]
        2          || [ '99999', '100000' ]
        3          || [ '99998', '99999', '100000' ]
        10         || ( 99991..100000 ).collect { it.toString() }
        100        || ( 99901..100000 ).collect { it.toString() }

    }

    @Unroll
    def "Can read the top of a long file"() {
        given: 'a file reader with a default buffer'
        FileContentReader reader = new FileReader( file, windowSize )

        when: 'A file with 100,000 lines is created'
        file << ( 1..100_000 ).join( '\n' )

        then: 'The file top can be read'
        reader.top()
        def top = reader.refresh()
        top.isPresent()
        top.get() == expectedLines

        where:
        windowSize || expectedLines
        1          || [ '1' ]
        2          || [ '1', '2' ]
        3          || [ '1', '2', '3' ]
        10         || ( 1..10 ).collect { it.toString() }
        100        || ( 1..100 ).collect { it.toString() }

    }

    @Unroll
    def "Can read the tail of a long file, then move up using a large or small buffer"() {
        given: 'a file reader with a buffer of size #bufferSize'
        FileContentReader reader = new FileReader( file, windowSize, bufferSize )

        when: 'A file with 100,000 lines is created'
        file << ( 1..100_000 ).join( '\n' )

        then: 'The file tail can be read'
        reader.tail()
        def tail = reader.refresh()
        tail.isPresent()

        when: 'we move up a certain number of lines'
        def result = reader.moveUp( windowSize )

        then: 'we get the expected lines'
        result.isPresent()
        result.get() == expectedLines

        when: 'we move up again'
        result = reader.moveUp( windowSize )

        then: 'we get the expected lines'
        result.get() == expectedLines2

        where:
        windowSize | bufferSize || expectedLines                 | expectedLines2
        1          | 2          || [ '99999' ]                   | [ '99998' ]
        2          | 3          || [ '99997', '99998' ]          | [ '99995', '99996' ]
        3          | 4          || [ '99995', '99996', '99997' ] | [ '99992', '99993', '99994' ]
        3          | 5          || [ '99995', '99996', '99997' ] | [ '99992', '99993', '99994' ]
        3          | 6          || [ '99995', '99996', '99997' ] | [ '99992', '99993', '99994' ]
        3          | 7          || [ '99995', '99996', '99997' ] | [ '99992', '99993', '99994' ]
        3          | 8          || [ '99995', '99996', '99997' ] | [ '99992', '99993', '99994' ]
        1          | 4096       || [ '99999' ]                   | [ '99998' ]
        2          | 4096       || [ '99997', '99998' ]          | [ '99995', '99996' ]
        3          | 4096       || [ '99995', '99996', '99997' ] | [ '99992', '99993', '99994' ]

    }

    @Unroll
    def "Can read the top of a long file, then move down using a large or small buffer"() {
        given: 'a file reader with a buffer of size #bufferSize'
        FileContentReader reader = new FileReader( file, windowSize, bufferSize )

        when: 'A file with 100,000 lines is created'
        file << ( 1..100_000 ).join( '\n' )

        then: 'The file top can be read'
        reader.top()
        def top = reader.refresh()
        top.isPresent()

        when: 'we move down a certain number of lines'
        def result = reader.moveDown( windowSize )

        then: 'we get the expected lines'
        result.isPresent()
        result.get() == expectedLines

        when: 'we move down again'
        result = reader.moveDown( windowSize )

        then: 'we get the expected lines'
        result.get() == expectedLines2

        where:
        windowSize | bufferSize || expectedLines     | expectedLines2
        1          | 2          || [ '2' ]           | [ '3' ]
        2          | 3          || [ '3', '4' ]      | [ '5', '6' ]
        3          | 4          || [ '4', '5', '6' ] | [ '7', '8', '9' ]
        3          | 5          || [ '4', '5', '6' ] | [ '7', '8', '9' ]
        3          | 6          || [ '4', '5', '6' ] | [ '7', '8', '9' ]
        3          | 7          || [ '4', '5', '6' ] | [ '7', '8', '9' ]
        3          | 8          || [ '4', '5', '6' ] | [ '7', '8', '9' ]
        1          | 4096       || [ '2' ]           | [ '3' ]
        2          | 4096       || [ '3', '4' ]      | [ '5', '6' ]
        3          | 4096       || [ '4', '5', '6' ] | [ '7', '8', '9' ]

    }

    @Unroll
    def "Refresh at top should cause the previously read lines to be read again"() {
        given: 'a file reader with a short byte buffer'
        FileContentReader reader = new FileReader( file, 3, 8 )

        and: 'A file with 10 lines is created'
        file << ( 0..9 ).collect { def s = it.toString(); s.padLeft( 3, s ) }.join( '\n' )

        when: 'the reader refreshes'
        def lines = reader.refresh()

        then: 'The top 3 lines are read'
        lines.isPresent()
        lines.get() == [ '000', '111', '222' ]

        when: 'the reader refreshes again'
        lines = reader.refresh()

        then: 'The top 3 lines are read'
        lines.isPresent()
        lines.get() == [ '000', '111', '222' ]

    }

    def "Refresh at tail should cause the previously read lines to be read again"() {
        given: 'a file reader with a short byte buffer'
        FileContentReader reader = new FileReader( file, 3, 8 )

        and: 'A file with 10 lines is created'
        file << ( 0..9 ).collect { def s = it.toString(); s.padLeft( 3, s ) }.join( '\n' )

        when: 'The reader moves to the tail'
        reader.tail()
        def lines = reader.refresh()

        then: 'The last 3 lines of the file are returned'
        lines.isPresent()
        lines.get() == [ '777', '888', '999' ]

        when: 'the reader refreshes'
        lines = reader.refresh()

        then: 'The tail 3 lines are read'
        lines.isPresent()
        lines.get() == [ '777', '888', '999' ]

        when: 'the reader refreshes again'
        lines = reader.refresh()

        then: 'The tail 3 lines are read'
        lines.isPresent()
        lines.get() == [ '777', '888', '999' ]
    }

    def "Moving up after file boundaries does not cause errors"() {
        given: 'a file reader with a short byte buffer'
        FileContentReader reader = new FileReader( file, 5, 8 )

        and: 'A file with 10 lines is created'
        file << ( 0..9 ).collect { def s = it.toString(); s.padLeft( 3, s ) }.join( '\n' )

        when: 'The file reader moves to the tail'
        reader.tail()
        def tail = reader.refresh()

        then: 'The last lines are returned'
        tail.isPresent()
        tail.get() == [ '555', '666', '777', '888', '999' ]

        when: 'The reader moves up nonsensical amounts'
        def lines3 = reader.moveUp( 3 )
        def lines2 = reader.moveUp( 2 )
        def lines10 = reader.moveUp( 10 )
        def lines200 = reader.moveUp( 200 )
        def lines1 = reader.moveUp( 1 )

        then: 'The previous lines are always returned, if any'
        lines3.isPresent()
        lines3.get() == [ '222', '333', '444' ]

        lines2.isPresent()
        lines2.get() == [ '000', '111' ]

        lines10.isPresent()
        lines10.get() == [ ]

        lines200.isPresent()
        lines200.get() == [ ]

        lines1.isPresent()
        lines1.get() == [ ]
    }

    def "Moving down after file boundaries does not cause errors"() {
        given: 'a file reader with a short byte buffer'
        FileContentReader reader = new FileReader( file, 5, 8 )

        and: 'A file with 10 lines is created'
        file << ( 0..9 ).collect { def s = it.toString(); s.padLeft( 3, s ) }.join( '\n' )

        when: 'The reader moves to the top line'
        reader.top()
        def top = reader.refresh()

        then: 'The top lines are returned'
        top.isPresent()
        top.get() == [ '000', '111', '222', '333', '444' ]

        when: 'The reader moves down nonsensical amounts'
        def lines3 = reader.moveDown( 3 )
        def lines2 = reader.moveDown( 2 )
        def lines10 = reader.moveDown( 10 )
        def lines200 = reader.moveDown( 200 )
        def lines1 = reader.moveDown( 1 )

        then: 'The lower lines are always returned'
        lines3.isPresent()
        lines3.get() == [ '555', '666', '777' ]

        lines2.isPresent()
        lines2.get() == [ '888', '999' ]

        lines10.isPresent()
        lines10.get() == [ ]

        lines200.isPresent()
        lines200.get() == [ ]

        lines1.isPresent()
        lines1.get() == [ ]
    }

    def "It is possible to refresh from the tail after a file change"() {
        given: 'a file reader with a short byte buffer'
        FileContentReader reader = new FileReader( file, 5, 8 )

        and: 'A file with 10 lines is created'
        file << ( 0..9 ).collect { def s = it.toString(); s.padLeft( 3, s ) }.join( '\n' )

        when: 'The file reader moves to the tail'
        reader.tail()
        def tail = reader.refresh()

        then: 'The last lines are returned'
        tail.isPresent()
        tail.get() == [ '555', '666', '777', '888', '999' ]

        when: 'The file gets more 2 lines written to it'
        file << '\na new line\nlast line'

        and: 'The reader refreshes'
        def lines = reader.refresh()

        then: 'The previously read lines are returned again '
        lines.isPresent()
        lines.get() == [ '555', '666', '777', '888', '999' ]

        when: 'The reader moves down 3 lines'
        lines = reader.moveDown( 3 )

        then: 'The 2 new lines are returned'
        lines.isPresent()
        lines.get() == [ 'a new line', 'last line' ]

        when: 'The reader moves down another few lines'
        lines = reader.moveDown( 6 )

        then: 'No more lines are available'
        lines.isPresent()
        lines.get() == [ ]

        when: 'The reader moves up 2 lines'
        lines = reader.moveUp( 2 )

        then: 'The 2 lines immediately before the current file window are returned'
        lines.isPresent()
        lines.get() == [ '555', '666' ]
    }

    def "It is possible to refresh from the top after a file change"() {
        given: 'a file reader with a short byte buffer'
        FileContentReader reader = new FileReader( file, 5, 8 )

        and: 'A file with 10 lines is created'
        file << ( 0..9 ).collect { def s = it.toString(); s.padLeft( 3, s ) }.join( '\n' )

        when: 'The file reader moves to the top'
        reader.top()
        def top = reader.refresh()

        then: 'The first lines are returned'
        top.isPresent()
        top.get() == [ '000', '111', '222', '333', '444' ]

        when: 'The file gets more 2 lines written to it at the top'
        file.write( 'top-line\nsecond-line\n' + file.text )

        and: 'The reader refreshes'
        def lines = reader.refresh()

        then: 'The new top lines are returned'
        lines.isPresent()
        lines.get() == [ 'top-line', 'second-line', '000', '111', '222' ]

        when: 'The reader moves down 3 lines'
        lines = reader.moveDown( 3 )

        then: 'The 3 next lines are returned'
        lines.isPresent()
        lines.get() == [ '333', '444', '555' ]

        when: 'The reader moves up 6 lines'
        lines = reader.moveUp( 6 )

        then: 'The top 3 lines are returned again'
        lines.isPresent()
        lines.get() == [ 'top-line', 'second-line', '000' ]
    }

}