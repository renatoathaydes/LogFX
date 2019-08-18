package com.athaydes.logfx.file

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files

@Unroll
class FileContentReaderWithFilterSpec extends Specification {

    @Shared
    File file = Files.createTempFile( 'file-content-reader-filter', '.log' ).toFile()

    def setup() {
        file.delete()
        file.deleteOnExit()
    }

    def "FileReader should be able to filter lines from the top"() {
        given: 'A FileReader with a line filter'
        FileContentReader reader = new FileReader( file, 5, 16 ).with {
            lineFilter = { String line -> line.contains( mustContainText ) }
            it
        }

        and: 'the File contains 100 lines'
        ( 1..100 ).each { line ->
            file << "line $line" << '\n'
        }

        when: 'we read the top of the file'
        reader.top()
        def top = reader.refresh()

        then: 'it should contain the expected lines'
        top.isPresent()
        top.get() == expectedLines

        where:
        mustContainText | expectedLines
        '1'             | [ 'line 1', 'line 10', 'line 11', 'line 12', 'line 13' ]
        'line 2'        | [ 'line 2', 'line 20', 'line 21', 'line 22', 'line 23' ]
        '10'            | [ 'line 10', 'line 100' ]
        '30'            | [ 'line 30' ]
    }

    def "FileReader should be able to filter lines from the top (after moving down)"() {
        given: 'A FileReader with a line filter'
        FileContentReader reader = new FileReader( file, 5, 16 ).with {
            lineFilter = { String line -> line.contains( mustContainText ) }
            it
        }

        and: 'the File contains 100 lines'
        ( 1..100 ).each { line ->
            file << "line $line" << '\n'
        }

        when: 'we read the top of the file, then down 3 lines'
        reader.top()
        reader.refresh()
        def next3Lines = reader.moveDown( 3 )
        def newFileWindow = reader.refresh()

        then: 'it should contain the expected lines'
        next3Lines.isPresent()
        next3Lines.get() == expectedNext3Lines
        newFileWindow.isPresent()
        newFileWindow.get() == expectedNewFileWindow

        where:
        mustContainText | expectedNext3Lines                  | expectedNewFileWindow
        '1'             | [ 'line 14', 'line 15', 'line 16' ] | [ 'line 12', 'line 13', 'line 14', 'line 15', 'line 16' ]
        'line 2'        | [ 'line 24', 'line 25', 'line 26' ] | [ 'line 22', 'line 23', 'line 24', 'line 25', 'line 26' ]
        '10'            | [ ]                                 | [ 'line 10', 'line 100' ]
        '30'            | [ ]                                 | [ 'line 30' ]
    }

    def "FileReader should be able to filter lines from the tail (after moving up)"() {
        given: 'A FileReader with a line filter'
        FileContentReader reader = new FileReader( file, 5, 16 ).with {
            lineFilter = { String line -> line.contains( mustContainText ) }
            it
        }

        and: 'the File contains 100 lines'
        ( 1..100 ).each { line ->
            file << "line $line" << '\n'
        }

        when: 'we read the tail of the file, then up 7 lines'
        reader.tail()
        reader.refresh()
        def up7 = reader.moveUp( 7 )
        def newFileWindow = reader.refresh()

        then: 'it should contain the expected lines'
        up7.isPresent()
        up7.get() == expectedUp7Lines
        newFileWindow.isPresent()
        newFileWindow.get() == expectedNewFileWindow

        where:
        mustContainText | expectedUp7Lines                                                                 | expectedNewFileWindow
        '1'             | [ 'line 17', 'line 18', 'line 19', 'line 21', 'line 31', 'line 41', 'line 51', ] |
                [ 'line 17', 'line 18', 'line 19', 'line 21', 'line 31' ]
        'line 2'        | [ 'line 2', 'line 20', 'line 21', 'line 22', 'line 23', 'line 24' ]              |
                [ 'line 2', 'line 20', 'line 21', 'line 22', 'line 23' ]
        '10'            | [ ]                                                                              |
                [ 'line 10', 'line 100' ]
        '30'            | [ ]                                                                              |
                [ 'line 30' ]
    }

    def "FileReader should be able to filter lines from the tail"() {
        given: 'A FileReader with a line filter'
        FileContentReader reader = new FileReader( file, 5, 16 ).with {
            lineFilter = { String line -> line.contains( mustContainText ) }
            it
        }

        and: 'the File contains 100 lines'
        ( 1..100 ).each { line ->
            file << "line $line" << '\n'
        }

        when: 'we read the tail of the file'
        reader.tail()
        def top = reader.refresh()

        then: 'it should contain the expected lines'
        top.isPresent()
        top.get() == expectedLines

        where:
        mustContainText | expectedLines
        '1'             | [ 'line 61', 'line 71', 'line 81', 'line 91', 'line 100' ]
        'line 2'        | [ 'line 25', 'line 26', 'line 27', 'line 28', 'line 29' ]
        '10'            | [ 'line 10', 'line 100' ]
        '30'            | [ 'line 30' ]
    }

}
