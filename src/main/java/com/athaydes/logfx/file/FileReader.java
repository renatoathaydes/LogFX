package com.athaydes.logfx.file;

import com.athaydes.logfx.ui.Dialog;
import com.athaydes.logfx.ui.LogView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 *
 */
public class FileReader {

    private static final Logger log = LoggerFactory.getLogger( FileReader.class );

    private static final int DEFAULT_BUFFER_SIZE = 1024;
    private static final int DEFAULT_MAX_BYTES = DEFAULT_BUFFER_SIZE * 1_000;

    private final long bufferSize;
    private final long maxBytes;

    private final File file;
    private final Consumer<List<String>> lineFeed;
    private final long minMillisBetweenUpdates;
    private final AtomicBoolean closed = new AtomicBoolean( false );
    private final Thread watcherThread;
    private final ScheduledExecutorService updateThread = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean updateScheduled = new AtomicBoolean( false );

    public FileReader( File file, Consumer<List<String>> lineFeed,
                       long minMillisBetweenUpdates, long maxBytes, long bufferSize ) {
        if ( bufferSize > maxBytes ) {
            throw new IllegalArgumentException( "bufferSize > maxBytes" );
        }
        if ( bufferSize <= 0 ) {
            throw new IllegalArgumentException( "bufferSize <= 0" );
        }

        this.minMillisBetweenUpdates = Math.max( 0, minMillisBetweenUpdates );
        this.file = file;
        this.lineFeed = lineFeed;
        this.maxBytes = maxBytes;
        this.bufferSize = bufferSize;
        this.watcherThread = watchFile( file.toPath() );
        watcherThread.setDaemon( true );
    }

    public FileReader( File file, Consumer<List<String>> lineFeed, long minMillisBetweenUpdates ) {
        this( file, lineFeed, minMillisBetweenUpdates, DEFAULT_MAX_BYTES, DEFAULT_BUFFER_SIZE );
    }

    /**
     * @param onDone called when done, if all good, true is passed to the function, otherwise, false.
     */
    public void start( Consumer<Boolean> onDone ) {
        onChange( fileReadOk -> {
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

                log.debug( "Watching path " + path.getFileName() );

                while ( !closed.get() ) {
                    WatchKey wk = watchService.take();
                    for ( WatchEvent<?> event : wk.pollEvents() ) {
                        //we only register "ENTRY_MODIFY" so the context is always a Path.
                        Path changed = ( Path ) event.context();
                        if ( !closed.get() && path.getFileName().equals( changed.getFileName() ) ) {
                            onChange();
                        }
                    }
                    // reset the key
                    boolean valid = wk.reset();
                    if ( !valid ) {
                        log.debug( "Key has been unregistered!!!!" );
                    }
                }
            } catch ( InterruptedException e ) {
                log.debug( "Interrupted watching file " + file );
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
        onChange( ( ignored ) -> {
        } );
    }

    private void onChange( Consumer<Boolean> fileUpdatedCallback ) {
        if ( updateScheduled.compareAndSet( false, true ) ) {
            updateThread.schedule( () -> {
                updateScheduled.set( false );

                //TODO implement reading file from any position
                try ( RandomAccessFile reader = new RandomAccessFile( file, "r" ) ) {
                    FileChannel channel = reader.getChannel();
                    final long channelLength = channel.size();
                    if ( channelLength == 0 ) {
                        lineFeed.accept( Collections.emptyList() );
                        fileUpdatedCallback.accept( true );
                    } else {
                        lineFeed.accept( lines( channel, channelLength ) );
                    }
                    fileUpdatedCallback.accept( true );
                } catch ( MalformedInputException e ) {
                    fileUpdatedCallback.accept( false );
                    Dialog.showConfirmDialog( "Bad encoding." );
                } catch ( Exception e ) {
                    fileUpdatedCallback.accept( false );
                    e.printStackTrace();
                }
            }, minMillisBetweenUpdates, TimeUnit.MILLISECONDS );
        }
    }

    private List<String> lines( FileChannel channel, long channelLength )
            throws IOException {
        LinkedList<String> fileLines = new LinkedList<>();

        long startPosition = channelLength;
        long size = bufferSize;
        final double maxIterations = Math.ceil( ( double ) maxBytes / bufferSize );
        long currentIterations = 0;
        long totalLinesRead = 0;

        do {
            startPosition -= size;

            // compensate for the case where startPosition is negative
            size = Math.min( bufferSize, bufferSize + startPosition );
            long nonNegativeStartPosition = Math.max( 0, startPosition );

            log.debug( "Mapping file partition from " + nonNegativeStartPosition +
                    " to " + ( nonNegativeStartPosition + size ) );

            MappedByteBuffer mapBuffer = channel.map( FileChannel.MapMode.READ_ONLY,
                    nonNegativeStartPosition, size );

            String partition = readPartition( mapBuffer );
            boolean newLineAtEnd = partition.endsWith( "\n" );
            LinkedList<String> reversedLines = reversedLinesOf( partition );

            if ( newLineAtEnd && currentIterations > 0 ) {
                reversedLines.removeFirst(); // empty-line can be removed as partition already broke up the lines
            }

            log.debug( "Partition: " + partition.replace( "\n", "#" ) );
            log.debug( "Lines: " + reversedLines );
            if ( !newLineAtEnd && !fileLines.isEmpty() ) {
                log.debug( "Joining with first in lines: " + fileLines );
                fileLines.set( 0, reversedLines.removeFirst() + fileLines.get( 0 ) );
            }

            for ( String line : reversedLines ) {
                fileLines.addFirst( line );
            }

            totalLinesRead += reversedLines.size();
            currentIterations += 1;

            // get out if already have enough lines (+ 1 so we can complete the last line if necessary)
            if ( totalLinesRead > LogView.getMaxLines() + 1 ) {
                log.debug( "Got enough lines already: " + totalLinesRead );
                break;
            }

            //joinPrevious = !partition.startsWith( "\n" );

        } while ( startPosition > 0 && currentIterations < maxIterations );
        log.debug( "PARTITIONS: " + fileLines );
        log.debug( "Done mapping file in " + currentIterations + " of " + maxIterations + " iterations" );
        return fileLines;
    }

    protected static LinkedList<String> reversedLinesOf( String partition ) {
        LinkedList<String> result = new LinkedList<>();
        int index = 0;
        int endIndex;
        while ( ( endIndex = partition.indexOf( '\n', index ) ) >= 0 ) {
            result.addFirst( partition.substring( index, endIndex ) );
            index = endIndex + 1;
        }

        result.addFirst( partition.substring( index ) );

        return result;
    }

    private static String readPartition( MappedByteBuffer buffer )
            throws CharacterCodingException {
        return StandardCharsets.US_ASCII.newDecoder().decode( buffer ).toString();
    }

    public void stop() {
        closed.set( true );
        watcherThread.interrupt();
        updateThread.shutdownNow();
    }

    public String getName() {
        return file.getName();
    }
}
