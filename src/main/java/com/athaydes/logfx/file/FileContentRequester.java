package com.athaydes.logfx.file;

import java.io.File;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * API for the {@link com.athaydes.logfx.ui.LogView} to request file contents.
 */
public interface FileContentRequester {

    /**
     * Request the file contents up from the current position.
     *
     * @param lines how many lines should be read
     * @return the lines above the current position, or nothing if the file does not exist
     */
    Optional<Stream<String>> moveUp( int lines );

    /**
     * Request the file contents down from the current position.
     *
     * @param lines how many lines should be read
     * @return the lines below the current position, or nothing if the file does not exist
     */
    Optional<Stream<String>> moveDown( int lines );

    /**
     * Request the file contents at the top.
     *
     * @param lines how many lines should be read
     * @return the lines at the top of the file, or nothing if the file does not exist
     */
    Optional<Stream<String>> toTop( int lines );

    /**
     * Request the file contents at the tail.
     *
     * @param lines how many lines should be read
     * @return the lines at the tail of the file, or nothing if the file does not exist
     */
    Optional<Stream<String>> toTail( int lines );

    /**
     * Refresh the current contents in case the file changed.
     *
     * @param lines how many lines should be read
     * @return the refreshed lines at the current position, or nothing if the file does not exist
     */
    Optional<Stream<String>> refresh( int lines );

    /**
     * Register a change listener.
     * <p>
     * The provided listener will be notified every time the file changes. The listener should
     * then decide whether to refresh its own contents, for example, by using the
     * {@link #refresh(int)} method.
     *
     * @param onChange listener for file changes
     */
    void setChangeListener( Runnable onChange );

    /**
     * @return the file associated with this instance.
     */
    File getFile();

    /**
     * Close this instance.
     */
    void close();
}
