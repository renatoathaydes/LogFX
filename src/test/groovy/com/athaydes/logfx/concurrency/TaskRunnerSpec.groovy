package com.athaydes.logfx.concurrency


import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import static com.google.code.tempusfugit.temporal.Duration.millis
import static com.google.code.tempusfugit.temporal.Timeout.timeout
import static com.google.code.tempusfugit.temporal.WaitFor.waitOrTimeout

class TaskRunnerSpec extends Specification {

    @Subject
    @Shared
    @AutoCleanup( "shutdown" )
    TaskRunner taskRunner = new TaskRunner()

    def setupSpec() {
        def task = { -> }
        Cancellable cancellable = null

        // warm up the task runner to minimize time fluctuations
        try {
            cancellable = taskRunner.scheduleRepeatingTask( Duration.ofMillis( 10 ), task )
        } catch ( e ) {
            e.printStackTrace()
        } finally {
            sleep 500
            cancellable?.cancel()
        }
    }

    def "Should be able to run tasks at most at a given frequency"() {
        given: 'A helper Integer mixin for assertions'
        Long.metaClass.between = { long start, long end ->
            long self = getDelegate() as long
            start <= self && self <= end
        }
        when: 'A task is requested to run 10 times within 100 ms, but with max frequency once every 40ms'
        int maxFrequencyInMs = 40
        int minTaskRunsToWaitFor = 3
        long startTime = System.currentTimeMillis()

        // TIME:       0  10  20  30  40  50  60  70  80  90  100 110 120
        // Schedule:   X  X   X   X   X   X   X   X   X   X   X
        // Run:        T              T...               T...                T...
        // Slow Run:       T...                         T...                      T...
        RunnableTask task =
                RunnableTask.runWith( taskRunner, 10, maxFrequencyInMs, 10, minTaskRunsToWaitFor, 200L )

        final List<Long> taskExecutionTimes = task.taskExecutionTimes.toList()
                .take( minTaskRunsToWaitFor )
                .collect { it - startTime }

        then: 'The task is run first nearly immediately after the first request'
        def initialTime = taskExecutionTimes.first()
        initialTime.between( 0L, maxFrequencyInMs - 1L )

        and: 'The next execution times occur within the max frequency expected'
        def time = initialTime
        for ( long executionTime in taskExecutionTimes.tail() ) {
            assert taskExecutionTimes &&
                    ( executionTime - time ).between( maxFrequencyInMs, maxFrequencyInMs * 3 )
            time = executionTime
        }

        when: 'We request the task to run again'
        sleep 60 // to avoid a delayed execution from the scheduled runs
        startTime = System.currentTimeMillis()
        def taskRuns = task.runCount.get()
        taskRunner.runWithMaxFrequency( task, maxFrequencyInMs, 0 )

        then: 'The task runs again almost immediately (within less than the maxPeriod)'
        waitOrTimeout( { task.runCount.get() == taskRuns + 1 },
                timeout( millis( maxFrequencyInMs - 1 ) ) )

    }

}

@TupleConstructor
@CompileStatic
class RunnableTask implements Runnable {
    AtomicInteger runCount
    CountDownLatch latch
    long[] taskExecutionTimes

    @Override
    void run() {
        taskExecutionTimes[ runCount.getAndIncrement() ] = System.currentTimeMillis()
        latch.countDown()
    }

    @CompileStatic
    static RunnableTask runWith( TaskRunner taskRunner,
                                 int taskSchedulingCallCount,
                                 int maxFrequencyInMs,
                                 long schedulePeriod,
                                 int minTaskRunsToWaitFor,
                                 long maxTimeToWaitForInMs ) {
        assert minTaskRunsToWaitFor < taskSchedulingCallCount

        def task = new RunnableTask(
                runCount: new AtomicInteger( 0 ),
                latch: new CountDownLatch( minTaskRunsToWaitFor ),
                taskExecutionTimes: new long[taskSchedulingCallCount] )

        taskSchedulingCallCount.times {
            def scheduleTime = System.currentTimeMillis()

            taskRunner.runWithMaxFrequency( task, maxFrequencyInMs, 0 )

            // it should not take any time to schedule tasks
            assert System.currentTimeMillis() - scheduleTime < 100L

            sleep schedulePeriod
        }

        // this wait guarantees we get at least 'minTaskRunsToWaitFor' executions
        assert task.latch.await( maxTimeToWaitForInMs, TimeUnit.MILLISECONDS )

        return task
    }

}
