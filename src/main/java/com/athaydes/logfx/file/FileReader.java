package com.athaydes.logfx.file;

import com.athaydes.logfx.ui.Dialog;
import org.apache.commons.io.input.ReversedLinesFileReader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
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

    private final ByteBuffer buffer = ByteBuffer.allocate( BUFFER_SIZE );
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

    public void start() {
        onChange2();
        watcherThread.start();
    }

    private Thread watchFile( Path path ) {
        System.out.println( "Watching path " + path.getFileName() );
        return new Thread( () -> {
            WatchKey watchKey = null;
            try ( WatchService watchService = FileSystems.getDefault().newWatchService() ) {
                watchKey = path.getParent().register( watchService, StandardWatchEventKinds.ENTRY_MODIFY );
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
                        System.out.println( "Key has been unregistrated!!!!" );
                    }
                }
            } catch ( InterruptedException e ) {
                System.out.println( "Interrupted watching file " + file );
            } catch ( IOException e ) {
                e.printStackTrace();
            } finally {
                watchKey.cancel();
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

    private void onChange2() {
        //TODO implement reading file from any position
        try ( RandomAccessFile reader = new RandomAccessFile( file, "r" ) ) {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
            StringBuilder builder = new StringBuilder( 10 * BUFFER_SIZE );
            long totalBytes = 0;
            int bytesRead = 0;
            while ( bytesRead != -1 && totalBytes < MAX_BYTES ) {
                buffer.position( 0 );
                FileChannel channel = reader.getChannel();
                channel.position( Math.max( 0, channel.size() - 1 - BUFFER_SIZE ) );
                do {
                    bytesRead = channel.read( buffer );
                    totalBytes += bytesRead;
                } while ( bytesRead != -1 && buffer.hasRemaining() );

                buffer.flip();
                builder.append( decoder.decode( buffer ) );
            }
            lineFeed.accept( builder.toString().split( "\n" ) );
        } catch ( MalformedInputException e ) {
            Dialog.showConfirmDialog( "Bad encoding." );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    public void stop() {
        closed.set( true );
        watcherThread.interrupt();
        readerThread.shutdownNow();
    }

}
