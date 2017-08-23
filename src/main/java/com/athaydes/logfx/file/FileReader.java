package com.athaydes.logfx.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @Override
    public Optional<Stream<String>> moveUp( int lines ) {
        return loadFromBottom( lines );
    }

    @Override
    public Optional<Stream<String>> moveDown( int lines ) {
        return loadFromTop( lines );
    }

    @Override
    public Optional<Stream<String>> toTop( int lines ) {
        position = 0L;
        return loadFromTop( lines );
    }

    @Override
    public Optional<Stream<String>> toTail( int lines ) {
        position = file.length();
        return loadFromBottom( lines );
    }

    @Override
    public Optional<Stream<String>> refresh( int lines ) {
        position -= bufferSize;
        return loadFromTop( lines );
    }

    @Override
    public void setChangeListener( Runnable onChange ) {
        this.onChange = onChange;
    }

    @Override
    public File getFile() {
        return file;
    }

    private Optional<Stream<String>> loadFromTop( int lines ) {
        if ( !file.isFile() ) {
            return Optional.empty();
        }
        byte[] buffer = new byte[ bufferSize ];

        LinkedList<String> result = new LinkedList<>();
        byte[] topBytes = new byte[ 0 ];

        try ( RandomAccessFile reader = new RandomAccessFile( file, "r" ) ) {

            readerMainLoop:
            while ( true ) {
                log.debug( "Seeking position {}", position );
                reader.seek( position );

                final long startIndex = reader.getFilePointer();

                log.trace( "Reading chunk {}:{}",
                        startIndex, startIndex + bufferSize );

                final int bytesRead = reader.read( buffer );
                int lineStartIndex = 0;

                for ( int i = 0; i < bytesRead; i++ ) {
                    byte b = buffer[ i ];
                    boolean isNewLine = ( b == '\n' );
                    if ( isNewLine || i + 1 == bytesRead ) {

                        // if the byte is a new line, don't include it in the result
                        int lineEndIndex = isNewLine ? i - 1 : i;
                        int lineLength = lineEndIndex - lineStartIndex + 1;

                        byte[] lineBytes = new byte[ lineLength + topBytes.length ];
                        log.trace( "Found line, copying [{}:{}] bytes from buffer + {} from top",
                                lineStartIndex, lineLength, topBytes.length );
                        System.arraycopy( buffer, lineStartIndex, lineBytes, 0, lineLength );
                        System.arraycopy( topBytes, 0, lineBytes, lineLength, topBytes.length );
                        result.addLast( new String( lineBytes, StandardCharsets.UTF_8 ) );
                        log.debug( "Added line: {}", result.getLast() );

                        if ( result.size() >= lines ) {
                            log.trace( "Got enough lines, breaking out" );

                            // set the position to the index where we stopped reading
                            position += isNewLine ? i + 1 : i + 2;
                            log.trace( "Leaving file position at {}, i = {}", position, i );

                            break readerMainLoop;
                        }

                        topBytes = new byte[ 0 ];
                        lineStartIndex = isNewLine ? i + 1 : i;
                    }
                }

                // read the chunk below in the next iteration
                position += bufferSize;

                if ( position >= reader.length() ) {
                    log.trace( "Already reached file end, breaking out of loop" );
                    break;
                }

                // remember the current buffer bytes as the next top bytes
                int bytesToCopy = bufferSize - lineStartIndex;
                byte[] newTop = new byte[ topBytes.length + bytesToCopy ];
                System.arraycopy( buffer, lineStartIndex, newTop, 0, bytesToCopy );
                System.arraycopy( topBytes, 0, newTop, bytesToCopy, topBytes.length );
                log.trace( "Updated top bytes, now top has {} bytes", newTop.length );
                topBytes = newTop;
            }

            log.debug( "Loaded {} lines from file {}", result.size(), file );
            return Optional.of( result.stream() );
        } catch ( IOException e ) {
            log.warn( "Error reading file [{}]: {}", file, e );
            return Optional.empty();
        }
    }

    private Optional<Stream<String>> loadFromBottom( int lines ) {
        if ( !file.isFile() ) {
            return Optional.empty();
        }
        byte[] buffer = new byte[ bufferSize ];

        LinkedList<String> result = new LinkedList<>();
        byte[] tailBytes = new byte[ 0 ];

        // start reading from the bottom section of the file above the previous position that fits into the buffer
        position = Math.max( 0, position - bufferSize );
        long previousStartIndex = -1;

        try ( RandomAccessFile reader = new RandomAccessFile( file, "r" ) ) {

            readerMainLoop:
            while ( true ) {
                log.debug( "Seeking position {}", position );
                reader.seek( position );

                final long startIndex = reader.getFilePointer();

                log.trace( "Reading chunk {}:{}, previous start: {}",
                        startIndex, startIndex + bufferSize, previousStartIndex );

                final int bytesRead = startIndex == 0L && previousStartIndex >= 0 ?
                        reader.read( buffer, 0, ( int ) previousStartIndex ) :
                        reader.read( buffer );

                int lastByteIndex = bytesRead - 1;
                log.trace( "Last byte index: {}", lastByteIndex );

                for ( int i = lastByteIndex; i >= 0; i-- ) {
                    byte b = buffer[ i ];
                    boolean isNewLine = ( b == '\n' );
                    if ( isNewLine ||
                            // is this the beginning of the file
                            ( startIndex == 0 && i == 0 ) ) {

                        // if the byte is a new line, don't include it in the result
                        int lineStartIndex = isNewLine ? i + 1 : i;
                        int bufferBytesToAdd = lastByteIndex - lineStartIndex + 1;

                        byte[] lineBytes = new byte[ bufferBytesToAdd + tailBytes.length ];
                        log.trace( "Found line, copying {} bytes from buffer + {} from tail", bufferBytesToAdd, tailBytes.length );
                        System.arraycopy( buffer, lineStartIndex, lineBytes, 0, bufferBytesToAdd );
                        System.arraycopy( tailBytes, 0, lineBytes, bufferBytesToAdd, tailBytes.length );
                        result.addFirst( new String( lineBytes, StandardCharsets.UTF_8 ) );
                        log.debug( "Added line: {}", result.getFirst() );

                        tailBytes = new byte[ 0 ];

                        if ( result.size() >= lines ) {
                            log.trace( "Got enough lines, breaking out" );

                            // set the position to the index where we stopped reading
                            position += i;
                            log.trace( "Leaving file position at {}", position );

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

                // remember the previous chunk start so that we can avoid re-reading it
                previousStartIndex = position;

                // read the above chunk in the next iteration
                position = Math.max( 0, position - bufferSize );

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
}
