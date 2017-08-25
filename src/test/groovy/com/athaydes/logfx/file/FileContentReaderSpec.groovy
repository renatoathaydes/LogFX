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
        FileContentReader reader = new FileReader( file, 8 )

        when: 'A file with a very long line is created'
        file << ( 'Z' * 100 )

        then: 'The file tail can be read'
        def tail = reader.toTail( 5 )
        tail.isPresent()
        tail.get() == [ 'Z' * 100 ]

    }

    def "Can read the tail of a multi-line file with some lines spanning multiple buffers"() {
        given: 'a file reader with a short byte buffer'
        FileContentReader reader = new FileReader( file, 8 )

        when: 'A file with some long and short lines is created'
        file << ( 'Z' * 100 ) << '\n' << 'abc' << '\n\n' << ( 'X' * 10 ) << '\n'

        then: 'The file tail can be read'
        def tail = reader.toTail( 5 )
        tail.isPresent()
        tail.get() == [ 'Z' * 100, 'abc', '', ( 'X' * 10 ), '' ]

    }

    @Unroll
    def "Can read the tail of a short file with a short buffer"() {
        given: 'a file reader with a short byte buffer'
        FileContentReader reader = new FileReader( file, 8 )

        when: 'A file with 10 lines is created'
        file << ( 1..10 ).join( '\n' )

        then: 'The file tail can be read'
        def tail = reader.toTail( lines )
        tail.isPresent()
        tail.get() == expectedLines

        where:
        lines || expectedLines
        1     || [ '10' ]
        2     || [ '9', '10' ]
        3     || [ '8', '9', '10' ]
        10    || ( 1..10 ).collect { it.toString() }
        100   || ( 1..10 ).collect { it.toString() }

    }

    @Unroll
    def "Can read the tail of a short file with the default buffer"() {
        given: 'a file reader with a short byte buffer'
        FileContentReader reader = new FileReader( file )

        when: 'A file with 10 lines is created'
        file << ( 1..10 ).join( '\n' )

        then: 'The file tail can be read'
        def tail = reader.toTail( lines )
        tail.isPresent()
        tail.get() == expectedLines

        where:
        lines || expectedLines
        1     || [ '10' ]
        2     || [ '9', '10' ]
        3     || [ '8', '9', '10' ]
        10    || ( 1..10 ).collect { it.toString() }
        100   || ( 1..10 ).collect { it.toString() }

    }

    @Unroll
    def "Can read the tail of a long file"() {
        given: 'a file reader with a default buffer'
        FileContentReader reader = new FileReader( file )

        when: 'A file with 100,000 lines is created'
        file << ( 1..100_000 ).join( '\n' )

        then: 'The file tail can be read'
        def tail = reader.toTail( lines )
        tail.isPresent()
        tail.get() == expectedLines

        where:
        lines || expectedLines
        1     || [ '100000' ]
        2     || [ '99999', '100000' ]
        3     || [ '99998', '99999', '100000' ]
        10    || ( 99991..100000 ).collect { it.toString() }
        100   || ( 99901..100000 ).collect { it.toString() }

    }

    @Unroll
    def "Can read the top of a long file"() {
        given: 'a file reader with a default buffer'
        FileContentReader reader = new FileReader( file )

        when: 'A file with 100,000 lines is created'
        file << ( 1..100_000 ).join( '\n' )

        then: 'The file top can be read'
        def top = reader.toTop( lines )
        top.isPresent()
        top.get() == expectedLines

        where:
        lines || expectedLines
        1     || [ '1' ]
        2     || [ '1', '2' ]
        3     || [ '1', '2', '3' ]
        10    || ( 1..10 ).collect { it.toString() }
        100   || ( 1..100 ).collect { it.toString() }

    }

    @Unroll
    def "Can read the tail of a long file, then move up using a large or small buffer"() {
        given: 'a file reader with a buffer of size #bufferSize'
        FileContentReader reader = new FileReader( file, bufferSize )

        when: 'A file with 100,000 lines is created'
        file << ( 1..100_000 ).join( '\n' )

        then: 'The file tail can be read'
        def tail = reader.toTail( lines )
        tail.isPresent()

        when: 'we move up a certain number of lines'
        def result = reader.moveUp( lines )

        then: 'we get the expected lines'
        result.isPresent()
        result.get() == expectedLines

        when: 'we move up again'
        result = reader.moveUp( lines )

        then: 'we get the expected lines'
        result.get() == expectedLines2

        where:
        lines | bufferSize || expectedLines                 | expectedLines2
        1     | 2          || [ '99999' ]                   | [ '99998' ]
        2     | 3          || [ '99997', '99998' ]          | [ '99995', '99996' ]
        3     | 4          || [ '99995', '99996', '99997' ] | [ '99992', '99993', '99994' ]
        3     | 5          || [ '99995', '99996', '99997' ] | [ '99992', '99993', '99994' ]
        3     | 6          || [ '99995', '99996', '99997' ] | [ '99992', '99993', '99994' ]
        3     | 7          || [ '99995', '99996', '99997' ] | [ '99992', '99993', '99994' ]
        3     | 8          || [ '99995', '99996', '99997' ] | [ '99992', '99993', '99994' ]
        1     | 4096       || [ '99999' ]                   | [ '99998' ]
        2     | 4096       || [ '99997', '99998' ]          | [ '99995', '99996' ]
        3     | 4096       || [ '99995', '99996', '99997' ] | [ '99992', '99993', '99994' ]

    }

    @Unroll
    def "Can read the top of a long file, then move down using a large or small buffer"() {
        given: 'a file reader with a buffer of size #bufferSize'
        FileContentReader reader = new FileReader( file, bufferSize )

        when: 'A file with 100,000 lines is created'
        file << ( 1..100_000 ).join( '\n' )

        then: 'The file top can be read'
        def top = reader.toTop( lines )
        top.isPresent()

        when: 'we move down a certain number of lines'
        def result = reader.moveDown( lines )

        then: 'we get the expected lines'
        result.isPresent()
        result.get() == expectedLines

        when: 'we move down again'
        result = reader.moveDown( lines )

        then: 'we get the expected lines'
        result.get() == expectedLines2

        where:
        lines | bufferSize || expectedLines     | expectedLines2
        1     | 2          || [ '2' ]           | [ '3' ]
        2     | 3          || [ '3', '4' ]      | [ '5', '6' ]
        3     | 4          || [ '4', '5', '6' ] | [ '7', '8', '9' ]
        3     | 5          || [ '4', '5', '6' ] | [ '7', '8', '9' ]
        3     | 6          || [ '4', '5', '6' ] | [ '7', '8', '9' ]
        3     | 7          || [ '4', '5', '6' ] | [ '7', '8', '9' ]
        3     | 8          || [ '4', '5', '6' ] | [ '7', '8', '9' ]
        1     | 4096       || [ '2' ]           | [ '3' ]
        2     | 4096       || [ '3', '4' ]      | [ '5', '6' ]
        3     | 4096       || [ '4', '5', '6' ] | [ '7', '8', '9' ]

    }

    @Unroll
    def "Refresh at top should cause the previously read lines to be read again"() {
        given: 'a file reader with a short byte buffer'
        FileContentReader reader = new FileReader( file, 8 )

        and: 'A file with 10 lines is created'
        file << ( 0..9 ).collect { def s = it.toString(); s.padLeft( 3, s ) }.join( '\n' )

        when: 'the reader refreshes 3 lines'
        def lines = reader.refresh( 3 )

        then: 'The top 3 lines are read'
        lines.isPresent()
        lines.get() == [ '000', '111', '222' ]

        when: 'the reader refreshes 3 lines'
        lines = reader.refresh( 3 )

        then: 'The top 3 lines are read'
        lines.isPresent()
        lines.get() == [ '000', '111', '222' ]

        when: 'the reader refreshes 4 lines'
        lines = reader.refresh( 4 )

        then: 'The top 4 lines are read'
        lines.isPresent()
        lines.get() == [ '000', '111', '222', '333' ]

        when: 'the reader refreshes 5 lines'
        lines = reader.refresh( 5 )

        then: 'The top 5 lines are read'
        lines.isPresent()
        lines.get() == [ '000', '111', '222', '333', '444' ]

        when: 'the reader refreshes 2 lines'
        lines = reader.refresh( 2 )

        then: 'The top 2 lines are read'
        lines.isPresent()
        lines.get() == [ '000', '111' ]

    }

    @Unroll
    def "Refresh at tail should cause the previously read lines to be read again"() {
        given: 'a file reader with a short byte buffer'
        FileContentReader reader = new FileReader( file, 8 )

        and: 'A file with 10 lines is created'
        file << ( 0..9 ).collect { def s = it.toString(); s.padLeft( 3, s ) }.join( '\n' )

        when: 'The reader moves to the 3 tail lines'
        def lines = reader.toTail( 3 )

        then: 'The last 3 lines of the file are returned'
        lines.isPresent()
        lines.get() == [ '777', '888', '999' ]

        when: 'the reader refreshes 3 lines'
        lines = reader.refresh( 3 )

        then: 'The tail 3 lines are read'
        lines.isPresent()
        lines.get() == [ '777', '888', '999' ]

        when: 'the reader refreshes 3 lines'
        lines = reader.refresh( 3 )

        then: 'The tail 3 lines are read'
        lines.isPresent()
        lines.get() == [ '777', '888', '999' ]

        when: 'the reader refreshes 2 lines'
        lines = reader.refresh( 2 )

        then: 'The 2 lines at the top of the previous read are read'
        lines.isPresent()
        lines.get() == [ '777', '888' ]

        when: 'the reader refreshes 5 lines'
        lines = reader.refresh( 5 )

        then: 'The 5 lines ending at the place where the previous read stopped are read'
        lines.isPresent()
        lines.get() == [ '444', '555', '666', '777', '888' ]

    }

}