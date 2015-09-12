package com.athaydes.logfx.file

import spock.lang.AutoCleanup
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

public class FileReaderTest extends Specification {

    @AutoCleanup( 'delete' )
    File file

    @Subject
    FileReader fileReader

    @Unroll
    def "Knows how to split lines properly, unlike String.split"() {
        when:
        'Breaking up some text into lines using the FileReader'
        def result = FileReader.linesOf( text )

        then:
        'We get the lines as expected, not like Java String.split which does not return the last empty lines'
        result == expectedLines

        where:
        text              | expectedLines
        ''                | [ '' ]
        'a'               | [ 'a' ]
        '\n'              | [ '', '' ]
        'a\n'             | [ '', 'a' ]
        '\na'             | [ 'a', '' ]
        '\n\n'            | [ '', '', '' ]
        '\n\n\n'          | [ '', '', '', '' ]
        '\na\nb\n'        | [ '', 'b', 'a', '' ]
        'logfx\nis\ncool' | [ 'cool', 'is', 'logfx' ]
    }

    def "Can read a few lines from a simple file using the default configuration"() {
        given: 'A simple file with a few lines of text'
        file = new File( "build/simple-file" )
        file.delete()
        file << """first line
                |second line
                |third line
                |fourth line""".stripMargin()

        and: 'A FileReader with a callback that saves all lines read in a list'
        def result = [ ]
        def reader = new FileReader( file, { lines -> result.addAll lines } )

        when: 'The FileReader is started and we wait until the done callback is called'
        def latch = new CountDownLatch( 1 )

        reader.start { success ->
            assert success
            latch.countDown()
        }

        assert latch.await( 5, TimeUnit.SECONDS )

        then: 'All lines have been added to the list in reverse order'
        result == [ 'first line', 'second line', 'third line', 'fourth line' ]
    }

    @Unroll
    def "Can read several lines with a buffer much smaller than the whole file"() {
        given: "A long file (100 lines with 100 characters each)"
        file = new File( "build/long-file" )
        file.delete()
        file.withWriter { writer ->
            ( 1..100 ).each {
                writer << ( it == 1 ? '' : '\n' ) +
                        ( it.toString()[ -1 ] * 99 )
            }
        }

        and: "A FileReader using a buffer of size #bufferSize and maximum bytes #maxBytes"
        final List<List> fileLines = [ ]
        fileReader = new FileReader( file,
                { lines -> fileLines.addAll lines },
                maxBytes, bufferSize )

        when: "The FileReader is started and we block until the the done callback is called"
        def latch = new CountDownLatch( 1 )

        fileReader.start { done ->
            assert done
            latch.countDown()
        }

        assert latch.await( 5, TimeUnit.SECONDS )

        then: "Only the lines that fit into maxBytes were read"
        fileLines.size() == expectedLinesRead

        and: "All lines except the last are all 100 characters long (99 + newline)"
        if ( fileLines.size() > 1 ) {
            fileLines[ 1..-1 ].collect { it.size() }.every { size -> size == 99 }
        }

        and: "The last line is as long as needed for the last partition to fill the bytes required"
        fileLines[ 0 ].size() == bytesInLastLine

        where:
        maxBytes | bufferSize | expectedLinesRead | bytesInLastLine
        510      | 40         | 6                 | 20
        750      | 100        | 9                 | 0
        3        | 1          | 1                 | 3
    }

    @Unroll
    def "Can join lines which are broken up in partitions"() {
        given: "A file with known contents"
        println "STARTING NEW TEST"
        file = new File( "build/long-file" )
        file.delete()
        file << fileContents

        and: "A FileReader using a buffer of size #bufferSize"
        final List fileLines = [ ]
        fileReader = new FileReader( file,
                { lines -> fileLines.addAll lines }, 9999999, bufferSize )

        when: "The FileReader is started and we block until the the done callback is called"
        def latch = new CountDownLatch( 1 )

        fileReader.start { done ->
            assert done
            latch.countDown()
        }

        assert latch.await( 5, TimeUnit.SECONDS )

        then: "All lines are correctly found"
        fileLines == expectedLines

        where:
        fileContents      | bufferSize | expectedLines
        'abc'             | 3          | [ 'abc' ]
        'ab\nc'           | 3          | [ 'ab', 'c' ]
        'abc\n'           | 3          | [ 'abc', '' ]
        'abc\nd'          | 3          | [ 'abc', 'd' ]
        'abc\n\n'         | 3          | [ 'abc', '', '' ]
        'abc\ndef\nghi'   | 3          | [ 'abc', 'def', 'ghi' ]
        'a\nbc\ndef\nghi' | 2          | [ 'a', 'bc', 'def', 'ghi' ]
    }

}
