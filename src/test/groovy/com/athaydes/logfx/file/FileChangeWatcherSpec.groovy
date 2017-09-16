package com.athaydes.logfx.file

import com.athaydes.logfx.concurrency.TaskRunner
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import static com.google.code.tempusfugit.temporal.Duration.millis
import static com.google.code.tempusfugit.temporal.Timeout.timeout
import static com.google.code.tempusfugit.temporal.WaitFor.waitOrTimeout

class FileChangeWatcherSpec extends Specification {

    @Shared
    TaskRunner taskRunner = new TaskRunner()

    def "FileWatcher can watch existing file"() {
        given: 'An existing file'
        def file = Files.createTempFile( 'FileChangeWatcherSpec', 'log' ).toFile()
        assert file.isFile()

        and: 'A FileChangeWatcher watches over it, informing the test when it changes'
        def fileWatcher = new FileChangeWatcher( file, taskRunner )
        final changeLatch1 = new CountDownLatch( 1 )
        final changeLatch2 = new CountDownLatch( 2 )

        fileWatcher.onChange = {
            changeLatch1.countDown()
            changeLatch2.countDown()
        }

        and: 'We wait for the watcher to start up'
        waitOrTimeout( { fileWatcher.isWatching() }, timeout( millis( 400L ) ) )
        sleep 1000L

        when: 'The file changes'
        file << 'hi'

        then: 'One file change is observed'
        def time = System.currentTimeMillis()
        changeLatch1.await( 4, TimeUnit.SECONDS )
        println "Got first file change notification in ${System.currentTimeMillis() - time} ms"

        when: 'The file changes again'
        file << 'bye'
        time = System.currentTimeMillis()

        then: 'Two file changes have been observed'
        changeLatch2.await( 4, TimeUnit.SECONDS )
        println "Got second file change notification in ${System.currentTimeMillis() - time} ms"
    }

    def "FileWatcher can see when existing file is deleted"() {
        given: 'An existing file'
        def file = Files.createTempFile( 'FileChangeWatcherSpec', 'log' ).toFile()
        assert file.isFile()

        and: 'A FileChangeWatcher watches over it, informing the test when it changes'
        def fileWatcher = new FileChangeWatcher( file, taskRunner )
        final changeLatch1 = new CountDownLatch( 1 )
        final changeLatch2 = new CountDownLatch( 2 )

        fileWatcher.onChange = {
            changeLatch1.countDown()
            changeLatch2.countDown()
        }

        and: 'We wait for the watcher to start up'
        waitOrTimeout( { fileWatcher.isWatching() }, timeout( millis( 400L ) ) )
        sleep 1000L

        when: 'The file changes'
        file << 'hi'

        then: 'One file change is observed'
        def time = System.currentTimeMillis()
        changeLatch1.await( 4, TimeUnit.SECONDS )
        println "Got first file change notification in ${System.currentTimeMillis() - time} ms"

        when: 'The file is deleted'
        assert file.delete()
        time = System.currentTimeMillis()

        then: 'Two file changes have been observed'
        changeLatch2.await( 4, TimeUnit.SECONDS )
        println "Got delete file change notification in ${System.currentTimeMillis() - time} ms"
    }

    def "FileWatcher can start watching non-existing file and later see when it's created"() {
        given: 'A non-existing file'
        def tempDir = Files.createTempDirectory( 'FileChangeWatcherSpec' ).toFile()
        tempDir.deleteOnExit()

        def file = new File( tempDir, 'test-file.log' )
        assert !file.exists()

        and: 'A FileChangeWatcher watches over it, informing the test when it gets created'
        def fileWatcher = new FileChangeWatcher( file, taskRunner )
        final changeLatch1 = new CountDownLatch( 1 )
        final changeLatch2 = new CountDownLatch( 2 )

        fileWatcher.onChange = {
            changeLatch1.countDown()
            changeLatch2.countDown()
        }

        and: 'We wait for the watcher to start up'
        waitOrTimeout( { fileWatcher.isWatching() }, timeout( millis( 400L ) ) )
        sleep 1000L

        when: 'The file is created'
        file << 'hi'

        then: 'One file change is observed'
        def time = System.currentTimeMillis()
        changeLatch1.await( 4, TimeUnit.SECONDS )
        println "Got first file change notification in ${System.currentTimeMillis() - time} ms"

        when: 'The file changes again'
        file << 'bye'
        time = System.currentTimeMillis()

        then: 'Two file changes have been observed'
        changeLatch2.await( 4, TimeUnit.SECONDS )
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
        def fileWatcher = new FileChangeWatcher( file, taskRunner )
        final changeLatch1 = new CountDownLatch( 1 )
        final changeLatch2 = new CountDownLatch( 2 )

        fileWatcher.onChange = {
            changeLatch1.countDown()
            changeLatch2.countDown()
        }

        then: 'We watcher does not start watching within a second as there is nothing to watch'
        sleep 1000L
        !fileWatcher.isWatching()

        when: 'The parent directory is created'
        assert nonExistingDir.mkdir()

        then: 'The File watcher finally starts watching'
        waitOrTimeout( { fileWatcher.isWatching() }, timeout( millis( 2500L ) ) )

        when: 'The file is created'
        file << 'hi'

        then: 'The first change is observed'
        def time = System.currentTimeMillis()
        changeLatch1.await( 4, TimeUnit.SECONDS )
        println "Got first file change notification in ${System.currentTimeMillis() - time} ms"

        when: 'The file changes again'
        file << 'hi'
        time = System.currentTimeMillis()

        then: 'Two file changes have been observed'
        changeLatch2.await( 4, TimeUnit.SECONDS )
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
        def fileWatcher = new FileChangeWatcher( file, taskRunner )
        final changeLatch1 = new CountDownLatch( 1 )
        final changeLatch2 = new CountDownLatch( 2 )
        final changeLatch3 = new CountDownLatch( 3 )

        fileWatcher.onChange = {
            changeLatch1.countDown()
            changeLatch2.countDown()
            changeLatch3.countDown()
        }

        and: 'We wait for the watcher to start up'
        waitOrTimeout( { fileWatcher.isWatching() }, timeout( millis( 400L ) ) )
        sleep 1000L

        and: 'The file is modified'
        file << 'hi'

        then: 'One file change is observed'
        def time = System.currentTimeMillis()
        changeLatch1.await( 4, TimeUnit.SECONDS )
        changeLatch2.count > 0
        changeLatch3.count > 0
        println "Got first file change notification in ${System.currentTimeMillis() - time} ms"

        when: 'The file and its parent directory are deleted'
        assert file.delete()
        assert removableDir.delete()

        then: 'Two file changes have been observed'
        changeLatch2.await( 4, TimeUnit.SECONDS )
        changeLatch3.count > 0
        println "Got second file change notification in ${System.currentTimeMillis() - time} ms"

        and: 'The File watcher is no longer watching'
        waitOrTimeout( { !fileWatcher.isWatching() }, timeout( millis( 400L ) ) )

        when: 'The file is re-created'
        assert removableDir.mkdir()
        file.createNewFile()
        time = System.currentTimeMillis()

        then: 'A third file change is observed'
        changeLatch3.await( 4, TimeUnit.SECONDS )
        println "Got first file change notification in ${System.currentTimeMillis() - time} ms"
    }


    def "FileWatcher can STOP watching file when it is closed"() {
        given: 'An existing file'
        def file = Files.createTempFile( 'FileChangeWatcherSpec', 'log' ).toFile()
        assert file.isFile()

        and: 'A FileChangeWatcher watches over it, informing the test when it changes'
        def fileWatcher = new FileChangeWatcher( file, taskRunner )
        final changeLatch1 = new CountDownLatch( 1 )
        final changeLatch2 = new CountDownLatch( 2 )

        fileWatcher.onChange = {
            changeLatch1.countDown()
            changeLatch2.countDown()
        }

        and: 'We wait for the watcher to start up'
        waitOrTimeout( { fileWatcher.isWatching() }, timeout( millis( 400L ) ) )
        sleep 1000L

        when: 'The file changes'
        file << 'hi'

        then: 'One file change is observed'
        def time = System.currentTimeMillis()
        changeLatch1.await( 4, TimeUnit.SECONDS )
        println "Got first file change notification in ${System.currentTimeMillis() - time} ms"

        when: 'The file watcher is closed'
        fileWatcher.close()
        sleep 250L

        and: 'The file is modified again'
        file << 'bye'

        then: 'The second file change is not observed'
        !changeLatch2.await( 2, TimeUnit.SECONDS )
    }


}
