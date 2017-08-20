package com.athaydes.logfx.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 *
 */
public class FileReader implements FileContentReader {

    private static final Logger log = LoggerFactory.getLogger( FileReader.class );

    private final File file;
    private final AtomicBoolean closed = new AtomicBoolean( false );
    private final int bufferSize;
    private final Thread watcherThread;
    private volatile Runnable onChange;

    /**
     * The current position we should start reading in the file.
     * This should be invalidated when the file changes.
     */
    private long position = 0L;

    /**
     * Size of the data "window" that has been read.
     */
    private long windowSize = 0L;

    public FileReader( File file ) {
        this( file, 4096 );
    }

    FileReader( File file, int bufferSize ) {
        this.file = file;
        this.bufferSize = bufferSize;
        watcherThread = watchFile( file.toPath() );
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
                            onChange.run();
                        }
                    }
                    // reset the key
                    boolean valid = wk.reset();
                    if ( !valid ) {
                        log.warn( "Key has been unregistered!!!!" );
                    }
                }
            } catch ( InterruptedException e ) {
                log.info( "Interrupted watching file {}", file );
            } catch ( IOException e ) {
                log.warn( "Problem with file watcher [{}]: {}", file, e );
            } finally {
                if ( watchKey != null ) {
                    watchKey.cancel();
                }
            }
        } );
    }

    @Override
    public void close() {
        log.debug( "Closing file reader for file {}", file );
        closed.set( true );
        watcherThread.interrupt();
    }

    private Optional<Stream<String>> loadTail( int lines ) {
        if ( !file.isFile() ) {
            return Optional.empty();
        }
        byte[] buffer = new byte[ bufferSize ];

        LinkedList<String> result = new LinkedList<>();
        byte[] tailBytes = new byte[ 0 ];

        try ( RandomAccessFile reader = new RandomAccessFile( file, "r" ) ) {
            reader.seek( file.length() + buffer.length );

            readerMainLoop:
            while ( true ) {
                long previousStart = reader.getFilePointer() - bufferSize;
                reader.seek( Math.max( 0L, reader.getFilePointer() - ( 2 * bufferSize ) ) );
                long startIndex = reader.getFilePointer();
                log.trace( "Starting to read at {}", startIndex );
                int bytesRead = startIndex == 0L ?
                        reader.read( buffer, 0, ( int ) previousStart ) :
                        reader.read( buffer );

                int lastByteIndex = bytesRead - 1;

                for ( int i = bytesRead - 1; i >= 0; i-- ) {
                    byte b = buffer[ i ];
                    boolean isNewLine = ( b == '\n' );
                    if ( isNewLine ||
                            // is this the beginning of the file
                            ( startIndex == 0 && i == 0 ) ) {

                        int bufferBytesToAdd = isNewLine ?
                                lastByteIndex - i :
                                lastByteIndex - i + 1;

                        byte[] lineBytes = new byte[ bufferBytesToAdd + tailBytes.length ];
                        log.trace( "Found line, copying {} bytes from buffer + {} from tail", bufferBytesToAdd, tailBytes.length );
                        System.arraycopy( buffer, isNewLine ? i + 1 : i, lineBytes, 0, bufferBytesToAdd );
                        System.arraycopy( tailBytes, 0, lineBytes, bufferBytesToAdd, tailBytes.length );
                        result.addFirst( new String( lineBytes, StandardCharsets.UTF_8 ) );
                        log.trace( "Added line: {}", result.getFirst() );

                        tailBytes = new byte[ 0 ];

                        if ( result.size() >= lines ) {
                            log.trace( "Got enough lines, breaking out" );
                            break readerMainLoop;
                        }
                        lastByteIndex = i - 1;
                        log.trace( "Last byte index is now {}", lastByteIndex );
                    }
                }

                if ( startIndex == 0 ) {
                    log.trace( "Already reached file start, breaking out of loop" );
                    break;
                }

                // remember the current buffer bytes as the next tail bytes
                byte[] newTail = new byte[ tailBytes.length + lastByteIndex + 1 ];
                System.arraycopy( buffer, 0, newTail, 0, lastByteIndex + 1 );
                System.arraycopy( tailBytes, 0, newTail, lastByteIndex + 1, tailBytes.length );
                log.trace( "Updated tail, now tail has {} bytes", newTail.length );
                tailBytes = newTail;
            }

            log.debug( "Loaded {} lines from file {}", result.size(), file );
            return Optional.of( result.stream() );
        } catch ( IOException e ) {
            log.warn( "Error reading file [{}]: {}", file, e );
            return Optional.empty();
        }
    }

    @Override
    public Optional<Stream<String>> moveUp( int lines ) {
        return loadTail( lines );
    }

    @Override
    public Optional<Stream<String>> moveDown( int lines ) {
        return loadTail( lines );
    }

    @Override
    public Optional<Stream<String>> toTop( int lines ) {
        position = 0L;
        windowSize = 0L;

        if ( !file.isFile() ) {
            return Optional.empty();
        }
        try ( BufferedReader reader = new BufferedReader( new java.io.FileReader( file ) ) ) {
            LinkedList<String> result = new LinkedList<>();
            String line;
            while ( result.size() < lines && ( line = reader.readLine() ) != null ) {
                result.addLast( line );
            }

            log.debug( "Loaded {} lines from file {}", result.size(), file );
            return Optional.of( result.stream() );
        } catch ( IOException e ) {
            log.warn( "Error reading file [{}]: {}", file, e );
            return Optional.empty();
        }
    }

    @Override
    public Optional<Stream<String>> toTail( int lines ) {
        return loadTail( lines );
    }

    @Override
    public Optional<Stream<String>> refresh( int lines ) {
        return loadTail( lines );
    }

    @Override
    public void setChangeListener( Runnable onChange ) {
        this.onChange = onChange;
    }

    @Override
    public File getFile() {
        return file;
    }
}
