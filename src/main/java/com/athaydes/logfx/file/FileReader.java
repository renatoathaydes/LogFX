package com.athaydes.logfx.file;

import com.athaydes.logfx.ui.Dialog;
import com.athaydes.logfx.ui.LogView;
import org.apache.commons.io.input.ReversedLinesFileReader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 *
 */
public class FileReader {

    private static final int BUFFER_SIZE = 1024;
    private static final int MAX_BYTES = BUFFER_SIZE * 1_000;

    private final File file;
    private final Consumer<String[]> lineFeed;
    private final AtomicBoolean closed = new AtomicBoolean( false );
    private final Thread watcherThread;
    private final ExecutorService readerThread = Executors.newSingleThreadExecutor();

    public FileReader( File file, Consumer<String[]> lineFeed ) {
        this.file = file;
        this.lineFeed = lineFeed;
        this.watcherThread = watchFile( file.toPath() );
        watcherThread.setDaemon( true );
    }

    /**
     * @param onDone called when done, if all good, true is passed to the function, otherwise, false.
     */
    public void start( Consumer<Boolean> onDone ) {
        readerThread.execute( () -> {
            boolean fileReadOk = onChange2();
            if ( fileReadOk ) {
                watcherThread.start();
            }
            onDone.accept( fileReadOk );
        } );
    }

    private Thread watchFile( Path path ) {
        return new Thread( () -> {
            WatchKey watchKey = null;
            try ( WatchService watchService = FileSystems.getDefault().newWatchService() ) {
                watchKey = path.getParent().register( watchService, StandardWatchEventKinds.ENTRY_MODIFY );

                System.out.println( "Watching path " + path.getFileName() );

                while ( !closed.get() ) {
                    WatchKey wk = watchService.take();
                    for ( WatchEvent<?> event : wk.pollEvents() ) {
                        //we only register "ENTRY_MODIFY" so the context is always a Path.
                        Path changed = ( Path ) event.context();
                        if ( !closed.get() && path.getFileName().equals( changed.getFileName() ) ) {
                            readerThread.execute( this::onChange2 );
                        }
                    }
                    // reset the key
                    boolean valid = wk.reset();
                    if ( !valid ) {
                        System.out.println( "Key has been unregistered!!!!" );
                    }
                }
            } catch ( InterruptedException e ) {
                System.out.println( "Interrupted watching file " + file );
            } catch ( IOException e ) {
                e.printStackTrace();
            } finally {
                if ( watchKey != null ) {
                    watchKey.cancel();
                }
            }
        } );
    }

    private void onChange() {
        try ( ReversedLinesFileReader reader = new ReversedLinesFileReader( file ) ) {
            String line;
            List<String> lines = new ArrayList<>();
            while ( ( line = reader.readLine() ) != null ) {
                lines.add( line );
            }
            Collections.reverse( lines );
            lineFeed.accept( lines.toArray( new String[ 0 ] ) );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    private boolean onChange2() {
        //TODO implement reading file from any position
        try ( RandomAccessFile reader = new RandomAccessFile( file, "r" ) ) {
            FileChannel channel = reader.getChannel();
            final long length = channel.size();
            if ( length == 0 ) {
                lineFeed.accept( new String[ 0 ] );
                return true;
            }

            lineFeed.accept( joinContiguous( partitions( channel, length ) ) );

            return true;
        } catch ( MalformedInputException e ) {
            Dialog.showConfirmDialog( "Bad encoding." );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return false;
    }

    private List<List<String>> partitions( FileChannel channel, long length )
            throws IOException {
        LinkedList<List<String>> partitions = new LinkedList<>();

        long startPosition = length - 1;
        long size = BUFFER_SIZE;
        final int maxIterations = MAX_BYTES / BUFFER_SIZE;
        int currentIterations = 0;
        long totalLinesRead = 0;

        partitionBuilding:
        do {
            startPosition -= size;

            // compensate for the case where startPosition is negative
            size = Math.min( BUFFER_SIZE, BUFFER_SIZE + startPosition );

            System.out.println( "Mapping file partition from " + startPosition + " to " + ( startPosition + size - 1 ) );

            MappedByteBuffer mapBuffer = channel.map( FileChannel.MapMode.READ_ONLY,
                    Math.max( 0, startPosition ), size );

            List<String> partition = linesFrom( mapBuffer );
            partitions.addFirst( partition );
            totalLinesRead += partition.size();
            currentIterations += 1;

            // get out if already have enough lines (+ 1 so we can complete the last line if necessary)
            if ( totalLinesRead > LogView.getMaxLines() + 1 ) {
                System.out.println( "Got enough lines already: " + totalLinesRead );
                break partitionBuilding;
            }
        } while ( startPosition > 0 && currentIterations < maxIterations );

        System.out.println( "Done mapping file in " + currentIterations + " of " + maxIterations + " iterations" );
        return partitions;
    }

    private static List<String> linesFrom( MappedByteBuffer buffer )
            throws CharacterCodingException {
        String text = StandardCharsets.US_ASCII.newDecoder().decode( buffer ).toString();
        return Arrays.asList( text.split( "\n" ) );
    }

    private static String[] joinContiguous( List<List<String>> partitions ) {
        List<String> result = new ArrayList<>( partitions.stream()
                .mapToInt( List::size ).sum() );

        boolean joinPrevious = false;
        for ( List<String> partition : partitions ) {
            Iterator<String> partitionIterator = partition.iterator();
            if ( joinPrevious && partitionIterator.hasNext() ) {
                appendToLastElementOf( result, partitionIterator.next() );
            }
            while ( partitionIterator.hasNext() ) {
                result.add( partitionIterator.next() );
            }
            joinPrevious = !result.get( result.size() - 1 ).endsWith( "\n" );
        }

        // we need the last lines first
        Collections.reverse( result );

        return result.toArray( new String[ 0 ] );
    }

    private static void appendToLastElementOf( List<String> list, String toAppend ) {
        if ( list.isEmpty() ) {
            list.add( toAppend );
        } else {
            int lastIndex = list.size() - 1;
            list.set( lastIndex, list.get( lastIndex ) + toAppend );
        }
    }

    public void stop() {
        closed.set( true );
        watcherThread.interrupt();
        readerThread.shutdownNow();
    }

}
