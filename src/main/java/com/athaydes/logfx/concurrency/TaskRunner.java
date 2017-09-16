package com.athaydes.logfx.concurrency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A Runner of tasks that can throttle execution.
 */
public class TaskRunner {

    private static final Logger log = LoggerFactory.getLogger( TaskRunner.class );

    private static final TaskRunner globalInstance = new TaskRunner( true ) {
        @Override
        public void shutdown() {
            // cannot shutdown the global task runner (it's a daemon anyway)
        }
    };

    private static final ThreadGroup threadGroup = new ThreadGroup( "logfx-global-task-runner" );

    private final ScheduledExecutorService executor;

    private enum TaskState {
        RAN_WITHIN_LIMIT, WAITING_TO_RUN
    }

    private final Map<Runnable, TaskState> scheduledTasks = new ConcurrentHashMap<>( 2 );

    public TaskRunner() {
        this( true );
    }

    public TaskRunner( boolean daemon ) {
        this.executor = Executors.newScheduledThreadPool( 2, ( runnable ) -> {
            Thread thread = new Thread( threadGroup, runnable );
            thread.setDaemon( daemon );
            return thread;
        } );
    }

    /**
     * @return the global instance of {@link TaskRunner}.
     * This class is thread-safe and can be shared safely between the whole application,
     * but it cannot be closed.
     */
    public static TaskRunner getGlobalInstance() {
        return globalInstance;
    }

    /**
     * Run a task asynchronously.
     *
     * @param task to run
     */
    public void runAsync( Runnable task ) {
        executor.submit( task );
    }

    /**
     * Run the given runnable task using the following algorithm:
     * <p>
     * * If the task has not run before or
     * the last time it ran was before {@code currentTime - maxFrequencyInMs}:
     * <p>
     * Run the task immediately.
     * <p>
     * * If the task last ran between {@code currentTime - maxFrequencyInMs} and {@code currentTime}:
     * <p>
     * Run the task at the instant {@code previousExecutionTime + maxFrequencyInMs}.
     *
     * @param runnable         to run later
     * @param maxFrequencyInMs maximum frequency this runnable may run
     */
    public void runWithMaxFrequency( Runnable runnable,
                                     long maxFrequencyInMs ) {
        if ( maxFrequencyInMs < 1L ) {
            throw new IllegalArgumentException( "maxFrequencyInMs must be larger than 0" );
        }

        boolean shouldRunImmediately;

        synchronized ( scheduledTasks ) {
            TaskState state = scheduledTasks.get( runnable );

            if ( state == null ) {
                // The task will run immediately, set its state so the next scheduled run will see this status
                // as RAN_WITHIN_LIMIT unless another request is made.
                scheduledTasks.put( runnable, TaskState.RAN_WITHIN_LIMIT );

                shouldRunImmediately = true;
            } else {
                // the task is in the Map, so it is already scheduled to check if it needs to run again
                switch ( state ) {
                    case WAITING_TO_RUN:
                        // nothing to do, the task will already run within the maximum frequency possible
                        break;
                    case RAN_WITHIN_LIMIT:
                        // change the state to WAITING_TO_RUN, so the task runs again on the scheduled run
                        scheduledTasks.put( runnable, TaskState.WAITING_TO_RUN );
                        break;
                }

                shouldRunImmediately = false;
            }
        }

        if ( shouldRunImmediately ) {
            log.trace( "Running task immediately: {}", runnable );
            executor.execute( () -> {
                try {
                    log.debug( "Running {}", runnable );
                    runnable.run();
                } catch ( Exception e ) {
                    log.warn( "Error running runnable", e );
                }
            } );

            // schedule to check again after a while if the task needs to run again
            executor.schedule( () -> {
                boolean runAgain = false;

                synchronized ( scheduledTasks ) {
                    TaskState currentState = scheduledTasks.remove( runnable );
                    switch ( currentState ) {
                        case WAITING_TO_RUN:
                            runAgain = true;
                            break;
                    }

                    log.trace( "{} scheduled tasks after removing {}", scheduledTasks.size(), runnable );
                }

                if ( runAgain ) {
                    // run asynchronously, do not block the executor's Thread
                    executor.execute( () -> runWithMaxFrequency( runnable, maxFrequencyInMs ) );
                }
            }, maxFrequencyInMs, TimeUnit.MILLISECONDS );
        } else {
            log.trace( "Task was already scheduled to check if it needs to run, request to run ignored" );
        }
    }

    public Cancellable repeat( int count, Duration delayBetweenRepetitions, Runnable task ) {
        AtomicReference<Cancellable> cancellable = new AtomicReference<>();
        AtomicInteger counter = new AtomicInteger( 0 );
        Runnable taskWrapper = () -> {
            try {
                task.run();
            } finally {
                if ( counter.incrementAndGet() >= count ) {
                    cancellable.get().cancel();
                }
            }
        };

        cancellable.set( scheduleRepeatingTask( delayBetweenRepetitions, taskWrapper ) );

        return cancellable.get();
    }

    public Cancellable scheduleRepeatingTask( Duration period, Runnable task ) {
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(
                task, 0L, period.toMillis(), TimeUnit.MILLISECONDS );

        return () -> future.cancel( false );
    }

    public void shutdown() {
        log.debug( "Shutting down TaskRunner" );
        executor.shutdown();
    }

}
