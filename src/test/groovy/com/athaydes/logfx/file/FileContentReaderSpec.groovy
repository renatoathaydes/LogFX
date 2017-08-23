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
        tail.get().iterator().toList() == [ 'Z' * 100 ]

    }

    def "Can read the tail of a multi-line file with some lines spanning multiple buffers"() {
        given: 'a file reader with a short byte buffer'
        FileContentReader reader = new FileReader( file, 8 )

        when: 'A file with some long and short lines is created'
        file << ( 'Z' * 100 ) << '\n' << 'abc' << '\n\n' << ( 'X' * 10 ) << '\n'

        then: 'The file tail can be read'
        def tail = reader.toTail( 5 )
        tail.isPresent()
        tail.get().iterator().toList() == [ 'Z' * 100, 'abc', '', ( 'X' * 10 ), '' ]

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
        tail.get().iterator().toList() == expectedLines

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
        tail.get().iterator().toList() == expectedLines

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
        tail.get().iterator().toList() == expectedLines

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
        top.get().iterator().toList() == expectedLines

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
        given: 'a file reader with a default buffer'
        FileContentReader reader = new FileReader( file )

        when: 'A file with 100,000 lines is created'
        file << ( 1..100_000 ).join( '\n' )

        then: 'The file tail can be read'
        def tail = reader.toTail( lines )
        tail.isPresent()

        when: 'we move up a certain number of lines'
        def result = reader.moveUp( lines )

        then: 'we get the expected lines'
        result.isPresent()
        result.get().iterator().toList() == expectedLines

        when: 'we move up again'
        result = reader.moveUp( lines )

        then: 'we get the expected lines'
        result.get().iterator().toList() == expectedLines2

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

}
