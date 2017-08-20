package com.athaydes.logfx.file;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
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
public class NewFileReader implements FileContentRequester {

    private static final Logger log = LoggerFactory.getLogger( NewFileReader.class );

    private final File file;
    private final AtomicBoolean closed = new AtomicBoolean( false );
    private final Thread watcherThread;
    private volatile Runnable onChange;

    /**
     * The current position we should start reading in the file.
     * This should be invalidated when the file changes.
     */
    private long position = 0L;

    public NewFileReader( File file ) {
        this.file = file;
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
        try {
            ReversedLinesFileReader reader = new ReversedLinesFileReader( file, 4096, StandardCharsets.UTF_8 );
            LinkedList<String> result = new LinkedList<>();
            String line;
            while ( result.size() < lines && ( line = reader.readLine() ) != null ) {
                result.addFirst( line );
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
        return loadTail( lines );
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
