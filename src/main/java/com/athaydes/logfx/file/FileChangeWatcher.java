package com.athaydes.logfx.file;

import com.athaydes.logfx.concurrency.Cancellable;
import com.athaydes.logfx.concurrency.TaskRunner;
import com.sun.nio.file.SensitivityWatchEventModifier;
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
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Watches a file for any change that might happen to it.
 */
public class FileChangeWatcher {

    private static final Logger log = LoggerFactory.getLogger( FileChangeWatcher.class );

    private static final AtomicInteger FILE_WATCHER_THREAD_COUNTER = new AtomicInteger( 0 );

    private final AtomicBoolean closed = new AtomicBoolean( false );
    private final AtomicBoolean watching = new AtomicBoolean( false );
    private final File file;
    private final Cancellable watcherTask;
    private final Runnable onChange;
    private volatile Thread watcherThread;

    public FileChangeWatcher( File file,
                              TaskRunner taskRunner,
                              Runnable onChange ) {
        this.file = file.getAbsoluteFile();
        this.onChange = onChange;

        if ( this.file.getParentFile() == null ) {
            log.warn( "Unable to watch file at the root path: {}", file );
            this.watcherTask = () -> log.debug( "Cancelling no-op FileWatcher for {}", file );
        } else {
            this.watcherTask = taskRunner.scheduleRepeatingTask(
                    Duration.ofSeconds( 2 ),
                    this::startIfNotWatching );
        }

    }

    private synchronized void startIfNotWatching() {
        if ( !isClosed() && !isWatching() ) {
            log.debug( "FileWatcher is not watching the file yet, trying to start it up for file {}", file );
            if ( file.getParentFile().isDirectory() ) {
                final Thread previousThread = watcherThread;

                if ( previousThread != null ) {
                    previousThread.interrupt();
                }

                log.debug( "Creating a new watcher Thread" );

                Thread thread = watchFile( file.toPath() );
                thread.setDaemon( true );
                this.watcherThread = thread;

                log.debug( "Will start Thread {}", thread.getName() );
                thread.start();
            } else {
                log.debug( "Cannot start watching file as its parent directory does not exist: {}", file );
            }
        }
    }

    private Thread watchFile( Path path ) {
        return new Thread( () -> {
            log.debug( "File watcher Thread started: {}", path );

            WatchKey watchKey = null;
            try ( WatchService watchService = FileSystems.getDefault().newWatchService() ) {
                watchKey = path.getParent().register( watchService, new WatchEvent.Kind[]{
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.OVERFLOW,
                        StandardWatchEventKinds.ENTRY_DELETE
                }, SensitivityWatchEventModifier.HIGH );

                log.info( "Watching file {}", path );
                watching.set( true );
                notifyWatcher( "started_watching" );

                while ( !isClosed() && isWatching() ) {
                    WatchKey wk = watchService.take();
                    log.trace( "Watch key: {}", wk );
                    for ( WatchEvent<?> event : wk.pollEvents() ) {
                        // we only register event kinds for which the context is always a Path.
                        Path changed = ( Path ) event.context();
                        if ( !closed.get() && path.getFileName().equals( changed.getFileName() ) ) {
                            notifyWatcher( event.kind().name() );
                        }
                    }
                    // reset the key
                    boolean valid = wk.reset();
                    if ( !valid ) {
                        log.info( "Key has been unregistered! File watcher cannot watch file " +
                                "until it is created again: {}", file );
                        notifyWatcher( "unregistered" );
                        watching.set( false );
                    }
                }
            } catch ( InterruptedException e ) {
                log.info( "Interrupted watching file {}", file );
                closed.set( true );
            } catch ( IOException e ) {
                log.warn( "Problem watching file [{}]: {}", file, e );
            } finally {
                watching.set( false );
                if ( watchKey != null ) {
                    watchKey.cancel();
                }
            }
        }, "file-change-watcher-" + FILE_WATCHER_THREAD_COUNTER.incrementAndGet() );
    }

    private void notifyWatcher( String eventKind ) {
        log.debug( "Notifying listener of change event {} on file {}",
                eventKind, file );
        try {
            onChange.run();
        } catch ( Exception e ) {
            log.warn( "Error handling file change event", e );
        }
    }

    public boolean isWatching() {
        return watching.get();
    }

    public boolean isClosed() {
        return closed.get();
    }

    public synchronized void close() {
        if ( !closed.getAndSet( true ) ) {
            log.info( "Closing FileChangeWatcher for file {}", file );

            final Thread thread = watcherThread;
            watcherThread = null;
            watcherTask.cancel();

            if ( thread != null ) {
                thread.interrupt();
            }
        }
    }

}
