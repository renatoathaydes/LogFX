package com.athaydes.logfx.file

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.nio.file.Files

class FileContentReaderSpec extends Specification {

    @Shared
    File file = Files.createTempFile( 'file-content-reader', '.log' ).toFile()

    @Subject
    FileContentReader reader = new NewFileReader( file )

    def setup() {
        file.delete()
        file.deleteOnExit()
    }

    @Unroll
    def "Can read the tail of a file"() {
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
    def "Can read the top of a file"() {
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

}
