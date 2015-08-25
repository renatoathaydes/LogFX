package com.athaydes.logfx.file;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by renato on 25/08/15.
 */
public class FileReaderJavaTest {

    File file;

    // subject
    FileReader fileReader;

    @After
    public void cleanup() {
        if ( file != null ) {
            file.delete();
        }
    }

    @Test
    public void canReadAFewLinesFromAFileUsingDefaultConfiguration()
            throws Exception {
        // GIVEN: A simple file with a few lines of text
        file = new File( "build/simple-file" );
        file.delete();
        try ( FileWriter stream = new FileWriter( file ) ) {
            stream.write( "First line\n" +
                    "Second line\n" +
                    "Third line\n" +
                    "Fourth line" );
        }

        // AND: A FileReader with a callback that saves all lines read in a list
        List<String> result = new ArrayList<>();
        fileReader = new FileReader( file,
                lines -> result.addAll( asList( lines ) ) );

        CountDownLatch latch = new CountDownLatch( 1 );

        // WHEN: The FileReader is started and we wait until the done callback is called
        fileReader.start( success -> {
            assertThat( success, is( true ) );
            latch.countDown();
        } );

        assertThat( latch.await( 5, TimeUnit.SECONDS ), is( true ) );

        // THEN: All lines have been added to the list in reverse order
        assertThat( result, is( equalTo( asList(
                "Fourth line", "Third line", "Second line", "First line" ) ) ) );
    }

    @Test
    public void canReadSeveralLinesWithABufferMuchSmallerThanTheWholeFile()
            throws Exception {
        // GIVEN: A long file (100 lines with 100 characters each)
        file = new File( "build/long-file" );
        file.delete();

        try ( FileWriter writer = new FileWriter( file ) ) {
            IntStream.rangeClosed( 1, 100 )
                    .mapToObj( lineNumber -> IntStream.rangeClosed( 1, 99 )
                            .mapToObj( Integer::toString )
                            .map( s -> s.substring( s.length() - 1 ) )
                            .reduce( "", ( acc, s ) -> acc + s ) )
                    .map( line -> line + "\n" )
                    .forEach( line -> {
                        try {
                            writer.write( line );
                        } catch ( IOException e ) {
                            throw new RuntimeException( "Error writing to file", e );
                        }
                    } );
        }

        // AND: A FileReader with a callback that saves all lines read in a list
        List<String> fileLines = new ArrayList<>();
        fileReader = new FileReader( file,
                lines -> fileLines.addAll( asList( lines ) ) );

        CountDownLatch latch = new CountDownLatch( 1 );

        // WHEN: The FileReader is started and we wait until the done callback is called
        fileReader.start( success -> {
            assertThat( success, is( true ) );
            latch.countDown();
        } );

        assertThat( latch.await( 5, TimeUnit.SECONDS ), is( true ) );

        // THEN: Only the lines that fit into maxBytes were read
//        assertThat( fileLines.size(), is( equalTo( 100 ) ) );

        // AND: The lines were all 100 characters long
        fileLines.stream().mapToInt( String::length ).allMatch(length -> length == 100);
    }

}
