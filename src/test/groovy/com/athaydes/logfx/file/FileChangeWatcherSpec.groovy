package com.athaydes.logfx.file

import com.athaydes.logfx.concurrency.TaskRunner
import groovy.transform.CompileStatic
import org.junit.jupiter.api.condition.OS
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.time.Duration
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

import static com.google.code.tempusfugit.temporal.Duration.millis
import static com.google.code.tempusfugit.temporal.Timeout.timeout
import static com.google.code.tempusfugit.temporal.WaitFor.waitOrTimeout

class FileChangeWatcherSpec extends Specification {

    @Shared
    @AutoCleanup( "shutdown" )
    TaskRunner taskRunner = new TaskRunner()

    LinkedBlockingQueue eventQueue = new LinkedBlockingQueue()

    @CompileStatic
    private FileChangeWatcher createFileChangeWatcher( File file ) {
        new FileChangeWatcher( file, taskRunner, {
            eventQueue.offer( true )
        } )
    }

    def setupSpec() {
        // warmup taskRunner and FileWatcher

        def file = Files.createTempFile( 'FileChangeWatcherSpec', '.log' ).toFile()
        assert file.isFile()
        def fileWatcher = createFileChangeWatcher( file )
        file << 'hello'
        sleep 100
        fileWatcher.close()
    }

    def setup() {
        eventQueue.clear()
    }

    private void assertSingleEventWithin( Duration timeout,
                                          Duration timeToWaitForNoFurtherEvents,
                                          String description ) {
        assertEventWithin timeout, description
        if ( OS.WINDOWS.currentOs ) {
            // allow double events on Windows
            skipEventsWithin( Duration.ofMillis( 500 ) )
        }
        assertNoEventsFor timeToWaitForNoFurtherEvents, description
    }

    private void assertOneOrTwoEventsWithin( Duration timeout1, Duration timeout2, String description ) {
        assert eventQueue.poll( timeout1.toMillis(), TimeUnit.MILLISECONDS ):
                "First Event '$description' did not occur within timeout"
        // just throw away possible second event
        eventQueue.poll( timeout2.toMillis(), TimeUnit.MILLISECONDS )
    }

    private void skipEventsWithin( Duration timeout ) {
        def max = System.currentTimeMillis() + timeout.toMillis()
        while ( System.currentTimeMillis() < max ) {
            eventQueue.poll( 10, TimeUnit.MILLISECONDS )
        }
    }

    private void assertEventWithin( Duration timeout, String description ) {
        assert eventQueue.poll( timeout.toMillis(), TimeUnit.MILLISECONDS ):
                "Event '$description' did not occur within timeout"
    }

    private void assertNoEventsFor( Duration duration, String description ) {
        long time = System.currentTimeMillis()
        def waitTime = { System.currentTimeMillis() - time }
        assert eventQueue.poll( duration.toMillis(), TimeUnit.MILLISECONDS ) == null:
                "Unexpected Event occurred within ${waitTime()} ms: '$description'"
    }

    def "FileWatcher can watch existing file"() {
        given: 'An existing file'
        def file = Files.createTempFile( 'FileChangeWatcherSpec', '.log' ).toFile()
        assert file.isFile()

        and: 'A FileChangeWatcher watches over it, informing the test when it changes'
        def fileWatcher = createFileChangeWatcher( file )

        and: 'We wait for the watcher to start up'
        waitOrTimeout( { fileWatcher.isWatching() }, timeout( millis( 400L ) ) )
        assertSingleEventWithin Duration.ofSeconds( 1 ), Duration.ofMillis( 500 ), 'initial start_watching'

        when: 'The file changes'
        file << 'hi'

        then: 'One file change is observed'
        def time = System.currentTimeMillis()
        assertSingleEventWithin Duration.ofSeconds( 4 ), Duration.ofMillis( 500 ), 'first file change'
        println "Got first file change notification in ${System.currentTimeMillis() - time} ms"

        when: 'The file changes again'
        file << 'bye'
        time = System.currentTimeMillis()

        then: 'Two file changes have been observed'
        assertSingleEventWithin Duration.ofSeconds( 4 ), Duration.ofMillis( 500 ), 'second file change'
        println "Got second file change notification in ${System.currentTimeMillis() - time} ms"
    }

    def "FileWatcher can see when existing file is deleted"() {
        given: 'An existing file'
        def file = Files.createTempFile( 'FileChangeWatcherSpec', '.log' ).toFile()
        assert file.isFile()

        and: 'A FileChangeWatcher watches over it, informing the test when it changes'
        def fileWatcher = createFileChangeWatcher( file )

        and: 'We wait for the watcher to start up'
        waitOrTimeout( { fileWatcher.isWatching() }, timeout( millis( 400L ) ) )
        assertSingleEventWithin Duration.ofSeconds( 1 ), Duration.ofMillis( 500 ), 'initial start_watching'

        when: 'The file changes'
        file << 'hi'

        then: 'One file change is observed'
        def time = System.currentTimeMillis()
        assertSingleEventWithin Duration.ofSeconds( 4 ), Duration.ofMillis( 500 ), 'first file change'
        println "Got first file change notification in ${System.currentTimeMillis() - time} ms"

        when: 'The file is deleted'
        assert file.delete()
        time = System.currentTimeMillis()

        then: 'Two file changes have been observed'
        assertSingleEventWithin Duration.ofSeconds( 4 ), Duration.ofMillis( 500 ), 'file deletion'
        println "Got delete file change notification in ${System.currentTimeMillis() - time} ms"
    }

    def "FileWatcher can start watching non-existing file and later see when it's created"() {
        given: 'A non-existing file'
        def tempDir = Files.createTempDirectory( 'FileChangeWatcherSpec' ).toFile()
        tempDir.deleteOnExit()

        def file = new File( tempDir, 'test-file.log' )
        assert !file.exists()

        and: 'A FileChangeWatcher watches over it, informing the test when it gets created'
        def fileWatcher = createFileChangeWatcher( file )

        and: 'We wait for the watcher to start up'
        waitOrTimeout( { fileWatcher.isWatching() }, timeout( millis( 400L ) ) )
        assertSingleEventWithin Duration.ofSeconds( 1 ), Duration.ofMillis( 500 ), 'initial start_watching event'

        when: 'The file is created'
        file << 'hi'

        then: 'Two file changes may be observed (create, write)'
        def time = System.currentTimeMillis()

        // depending on the OS, we get one or two events in quick succession... all we care here is that we capture one event at least
        assertOneOrTwoEventsWithin Duration.ofSeconds( 4 ), Duration.ofMillis( 500 ), 'file is created'
        println "Got first file change notification in ${System.currentTimeMillis() - time} ms"

        when: 'The file changes again'
        file << 'bye'
        time = System.currentTimeMillis()

        then: 'One more file change has been observed'
        assertSingleEventWithin Duration.ofSeconds( 4 ), Duration.ofMillis( 500 ), 'second file change'
        println "Got second file change notification in ${System.currentTimeMillis() - time} ms"
    }

    def "FileWatcher can start watching non-existing file under non-existing dir, and later see when it's created"() {
        given: 'A non-existing file under a non-existing directory'
        def tempDir = Files.createTempDirectory( 'FileChangeWatcherSpec' ).toFile()
        tempDir.deleteOnExit()

        def nonExistingDir = new File( tempDir, 'dir' )

        def file = new File( nonExistingDir, 'test-file.log' )
        assert !nonExistingDir.exists() && !file.exists()

        when: 'A FileChangeWatcher watches over it, informing the test when it gets created'
        def fileWatcher = createFileChangeWatcher( file )

        then: 'We watcher does not start watching within a second as there is nothing to watch'
        assertNoEventsFor Duration.ofSeconds( 1 ), 'no file, no dir'
        !fileWatcher.isWatching()

        when: 'The parent directory is created'
        assert nonExistingDir.mkdir()

        then: 'The File watcher finally starts watching'
        waitOrTimeout( { fileWatcher.isWatching() }, timeout( millis( 2500L ) ) )
        assertSingleEventWithin Duration.ofSeconds( 1 ), Duration.ofMillis( 500 ), 'initial start_watching event'

        when: 'The file is created'
        file << 'hi'

        then: 'The first change is observed'
        def time = System.currentTimeMillis()

        // depending on the OS, we get one or two events in quick succession... all we care here is that we capture one event at least
        assertOneOrTwoEventsWithin Duration.ofSeconds( 4 ), Duration.ofMillis( 500 ), 'file is created'
        println "Got first file change notification in ${System.currentTimeMillis() - time} ms"

        when: 'The file changes again'
        file << 'hi'
        time = System.currentTimeMillis()

        then: 'Two file changes have been observed'
        assertSingleEventWithin Duration.ofSeconds( 4 ), Duration.ofMillis( 500 ), 'second file change'
        println "Got second file change notification in ${System.currentTimeMillis() - time} ms"
    }

    def "FileWatcher can start watching existing file, then restart after it gets deleted and re-created again"() {
        given: 'An existing file under a removable directory'
        def tempDir = Files.createTempDirectory( 'FileChangeWatcherSpec' ).toFile()
        tempDir.deleteOnExit()

        def removableDir = new File( tempDir, 'dir' )
        assert removableDir.mkdir()

        def file = new File( removableDir, 'test-file.log' )
        assert file.createNewFile()

        when: 'A FileChangeWatcher watches over it, informing the test when it gets changed'
        def fileWatcher = createFileChangeWatcher( file )

        and: 'We wait for the watcher to start up'
        waitOrTimeout( { fileWatcher.isWatching() }, timeout( millis( 400L ) ) )
        assertSingleEventWithin Duration.ofSeconds( 1 ), Duration.ofMillis( 500 ), 'initial start_watching event'

        and: 'The file is modified'
        println "Writing to file"
        file << 'hi'

        then: 'One file change is observed'
        def time = System.currentTimeMillis()
        assertSingleEventWithin Duration.ofSeconds( 4 ), Duration.ofMillis( 500 ), 'first file change'
        println "Got first file change notification in ${System.currentTimeMillis() - time} ms"

        when: 'The file and its parent directory are deleted'
        time = System.currentTimeMillis()
        println "Deleting file"
        assert file.delete()
        println "Deleting dir"
        assert removableDir.delete()

        then: 'Two file changes have been observed'
        assertEventWithin Duration.ofSeconds( 4 ), 'file deleted'
        println "Got second file change notification in ${System.currentTimeMillis() - time} ms"

        and: 'The File watcher is no longer watching'
        waitOrTimeout( { !fileWatcher.isWatching() }, timeout( millis( 400L ) ) )

        when: 'The file is re-created'
        assert removableDir.mkdir()
        println "Creating file again"
        file.createNewFile()
        time = System.currentTimeMillis()

        then: 'A third file change is observed'
        assertSingleEventWithin Duration.ofSeconds( 4 ), Duration.ofMillis( 500 ), 'file re-created'
        println "Got first file change notification in ${System.currentTimeMillis() - time} ms"
    }

    def "FileWatcher can STOP watching file when it is closed"() {
        given: 'An existing file'
        def file = Files.createTempFile( 'FileChangeWatcherSpec', '.log' ).toFile()
        assert file.isFile()

        and: 'A FileChangeWatcher watches over it, informing the test when it changes'
        def fileWatcher = createFileChangeWatcher( file )

        and: 'We wait for the watcher to start up'
        waitOrTimeout( { fileWatcher.isWatching() }, timeout( millis( 400L ) ) )
        assertSingleEventWithin Duration.ofSeconds( 1 ), Duration.ofMillis( 500 ), 'initial start_watching event'

        when: 'The file changes'
        file << 'hi'

        then: 'One file change is observed'
        def time = System.currentTimeMillis()
        assertEventWithin Duration.ofSeconds( 4 ), 'first file change'
        println "Got first file change notification in ${System.currentTimeMillis() - time} ms"

        when: 'The file watcher is closed'
        fileWatcher.close()

        then: 'It stops watching within a short time'
        waitOrTimeout( { !fileWatcher.isWatching() }, timeout( millis( 400L ) ) )

        when: 'The file is modified again'
        file << 'bye'

        then: 'The second file change is not observed'
        assertNoEventsFor Duration.ofSeconds( 2 ), 'fileWatcher is closed, no more events'
    }

}
