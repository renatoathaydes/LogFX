package com.athaydes.logfx.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Watches a file for any change that might happen to it.
 */
public class FileChangeWatcher {

    private static final Logger log = LoggerFactory.getLogger( FileChangeWatcher.class );

    private final AtomicBoolean closed = new AtomicBoolean( false );
    private final AtomicBoolean watching = new AtomicBoolean( false );
    private final File file;
    private final Thread watcherThread;
    private volatile Runnable onChange;

    public FileChangeWatcher( File file ) {
        this.file = file;
        this.watcherThread = watchFile( file.toPath() );
        this.watcherThread.start();
    }

    public void setOnChange( Runnable onChange ) {
        this.onChange = onChange;
    }

    private Thread watchFile( Path path ) {
        return new Thread( () -> {
            WatchKey watchKey = null;
            try ( WatchService watchService = FileSystems.getDefault().newWatchService() ) {
                watchKey = path.getParent().register( watchService,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.OVERFLOW,
                        StandardWatchEventKinds.ENTRY_DELETE );

                log.info( "Watching file " + path );
                watching.set( true );

                while ( !closed.get() ) {
                    WatchKey wk = watchService.take();
                    for ( WatchEvent<?> event : wk.pollEvents() ) {
                        //we only register "ENTRY_MODIFY" so the context is always a Path.
                        Path changed = ( Path ) event.context();
                        if ( !closed.get() && path.getFileName().equals( changed.getFileName() ) ) {
                            final Runnable toRun = onChange;
                            if ( toRun != null ) {
                                log.debug( "Notifying listener of change event {} on file {}",
                                        event.kind(), file );
                                toRun.run();
                            } else {
                                log.info( "File change detected ({}), but no listener has been registered " +
                                        "for file: {}", event.kind(), file );
                            }
                        }
                    }
                    // reset the key
                    boolean valid = wk.reset();
                    if ( !valid ) {
                        log.warn( "Key has been unregistered!!!! Closing file watcher of file {}", file );
                        closed.set( true );
                    }
                }
            } catch ( InterruptedException e ) {
                log.info( "Interrupted watching file {}", file );
                closed.set( true );
            } catch ( IOException e ) {
                log.warn( "Problem with file watcher [{}]: {}", file, e );
            } finally {
                watching.set( false );
                if ( watchKey != null ) {
                    watchKey.cancel();
                }
            }
        } );
    }

    public boolean isWatching() {
        return !isClosed() && watching.get();
    }

    public boolean isClosed() {
        return closed.get();
    }

    public void close() {
        if ( !closed.getAndSet( true ) ) {
            watcherThread.interrupt();
        }
    }

}
