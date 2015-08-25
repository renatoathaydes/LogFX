package com.athaydes.logfx.file

import spock.lang.AutoCleanup
import spock.lang.Specification
import spock.lang.Subject

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

public class FileReaderTest extends Specification {

    @AutoCleanup( 'delete' )
    File file

    @Subject
    FileReader fileReader


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
        result == ['fourth line', 'third line', 'second line', 'first line']
    }

    def "Can read several lines with a buffer much smaller than the whole file"() {
        given: "A long file (100 lines with 100 characters each)"
        file = new File( "build/long-file" )
        file.delete()
        file.withWriter { writer ->
            ( 1..100 ).each { writer << (it.toString()[ -1 ] * 99 + '\n') }
        }

        and: "A FileReader using a buffer of size #bufferSize and maximum bytes #maxBytes"
        final fileLines = [ ]
        fileReader = new FileReader( file,
                { lines -> println lines; fileLines.addAll lines },
                maxBytes, bufferSize )

        when: "The FileReader is started and we block until the the done callback is called"
        def latch = new CountDownLatch( 1 )

        fileReader.start { done ->
            assert done
            latch.countDown()
        }

        assert latch.await( 5, TimeUnit.SECONDS )

        then: "Only the lines that fit into maxBytes were read"
     //   fileLines.size() == expectedLinesRead

        and: "The lines are all 100 characters long"
        fileLines.collect { it.size() }.every { size -> size == 100 }
//        fileLines.every { line -> line.size() == 100 }

        where:
        maxBytes | bufferSize || expectedLinesRead
        510      | 40         || 5
      //  810      | 40         || 8
       // 2        | 1          || 0

    }

}
