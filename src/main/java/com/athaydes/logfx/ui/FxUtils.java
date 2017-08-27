package com.athaydes.logfx.ui;

import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Paint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Utility functions for JavaFX-related functionality.
 */
public class FxUtils {

    private static final Logger log = LoggerFactory.getLogger( FxUtils.class );

    private static final Map<Paint, Background> bkgByPaint = new HashMap<>();

    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool( 2 );

    private static final Set<Runnable> scheduledTasks = ConcurrentHashMap.newKeySet( 2 );

    /**
     * Creates and caches a simple {@link Background} for the given {@link Paint}.
     *
     * @param paint of the background
     * @return simple background
     */
    public static Background simpleBackground( Paint paint ) {
        return bkgByPaint.computeIfAbsent( paint,
                ignored -> new Background(
                        new BackgroundFill( paint, CornerRadii.EMPTY, Insets.EMPTY ) ) );
    }

    /**
     * Run the given runnable after the maxFrequencyInMs delay if this is the first time
     * ths runnable is seen, or possibly ignore this call if a previous request to run it
     * has been performed within the maxFrequencyInMs time (in which case the runnable
     * is going to run soon, so it is ok to ignore this call).
     *
     * @param runnable         to run later
     * @param maxFrequencyInMs maximum frequency this runnable may run
     */
    public static void runWithMaxFrequency( Runnable runnable, long maxFrequencyInMs ) {
        if ( maxFrequencyInMs < 1L ) {
            throw new IllegalArgumentException( "maxFrequencyInMs must be larger than 0" );
        }

        // only add and run the task if it has not been scheduled yet,
        // otherwise the task will run soon and we don't need to do anything here
        boolean shouldScheduleTask;
        synchronized ( scheduledTasks ) {
            shouldScheduleTask = !scheduledTasks.contains( runnable );
            if ( shouldScheduleTask ) {
                scheduledTasks.add( runnable );
            }
        }

        if ( shouldScheduleTask ) {
            log.debug( "Scheduling new task: {}", runnable );
            executor.schedule( () -> {
                try {
                    log.debug( "Running {}", runnable );
                    runnable.run();
                } catch ( Exception e ) {
                    log.warn( "Error running runnable", e );
                } finally {
                    scheduledTasks.remove( runnable );
                    log.trace( "{} scheduled tasks after removing {}", scheduledTasks.size(), runnable );
                }
            }, maxFrequencyInMs, TimeUnit.MILLISECONDS );
        }
    }

    public static void shutdown() {
        log.debug( "Shutting down FxUtils" );
        executor.shutdown();
    }

}
