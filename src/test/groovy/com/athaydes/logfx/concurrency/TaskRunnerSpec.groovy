package com.athaydes.logfx.concurrency

import spock.lang.AutoCleanup
import spock.lang.Specification
import spock.lang.Subject

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TaskRunnerSpec extends Specification {

    @Subject
    @AutoCleanup( "shutdown" )
    TaskRunner taskRunner = new TaskRunner()

    def "Should be able to run tasks at most at a given frequency"() {
        given: 'A helper Integer mixin for assertions'
        Long.metaClass.between = { long start, long end ->
            long self = getDelegate() as long
            start <= self && self <= end
        }
        when: 'A task is requested to run 10 times within 100 ms, but with max frequency once every 40ms'
        def taskSchedulingCallCount = 10
        List<Long> taskExecutionTimes = [ ].asSynchronized()
        def latch = new CountDownLatch( taskSchedulingCallCount )
        def startTime = System.currentTimeMillis()
        def task = { taskExecutionTimes << ( System.currentTimeMillis() - startTime ) }
        def maxFrequencyInMs = 40
        10.times {
            taskRunner.runWithMaxFrequency( task, maxFrequencyInMs )
            latch.countDown()
            sleep 10
        }

        then: 'All requests are performed within a timeout of 150ms'
        latch.await( 150, TimeUnit.MILLISECONDS )

        and: 'The task is run first nearly immediately after the first request'
        !taskExecutionTimes.isEmpty() && taskExecutionTimes.first().between( 0L, 30L )

        and: 'The second and third executions occur within 30ms of the expected times'
        taskExecutionTimes.size() >= 3

        def delta2 = taskExecutionTimes[ 1 ] - taskExecutionTimes[ 0 ]
        def delta3 = taskExecutionTimes[ 2 ] - taskExecutionTimes[ 1 ]

        delta2.between( 40L, 70L ) && delta3.between( 40L, 70L )

        and: 'After another 50 ms max, one more executions occur'
        for ( i in 1..5 ) {
            if ( taskExecutionTimes.size() == 4 ) {
                break
            }
            sleep 10 // wait a little and try again
        }

        taskExecutionTimes.size() == 4

        and: 'We wait 50 ms, no more executions occur'
        sleep 50
        taskExecutionTimes.size() == 4

        when: 'We request the task to run again'
        startTime = System.currentTimeMillis()
        taskRunner.runWithMaxFrequency( task, maxFrequencyInMs )

        then: 'The task runs again almost immediately (within 20 ms)'
        sleep 20
        taskExecutionTimes.size() == 5
    }

}
