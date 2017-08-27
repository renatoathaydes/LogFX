package com.athaydes.logfx.file;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * API for the {@link com.athaydes.logfx.ui.LogView} to request file contents.
 * <p>
 * A {@code FileContentReader} works with a file window, a view of a file that spans a
 * certain fixed number of lines.
 * <p>
 * All move operations work by moving that file window up or down a certain number of lines.
 * <p>
 * Absolute operations, such as {@link #tail()} and {@link #top()} set the file window
 * directly.
 */
public interface FileContentReader {

    /**
     * Request the given number of lines above the current file window, moving
     * the file window accordingly.
     *
     * @param lines how many lines should be read
     * @return the lines above the current file window, or nothing if the file does not exist
     */
    Optional<? extends List<String>> moveUp( int lines );

    /**
     * Request the given number of lines below the current file window, moving
     * the file window accordingly.
     *
     * @param lines how many lines should be read
     * @return the lines below the current file window, or nothing if the file does not exist
     */
    Optional<? extends List<String>> moveDown( int lines );

    /**
     * Request the top file window.
     *
     * @return the top file window, or nothing if the file does not exist
     */
    Optional<? extends List<String>> top();

    /**
     * Request the tail file window.
     *
     * @return the tail file window, or nothing if the file does not exist
     */
    Optional<? extends List<String>> tail();

    /**
     * Refresh the current file window.
     * <p>
     * A file window starts from the top, so the first line of the file window
     * is used as a point of reference on refreshes.
     * <p>
     * If the first line of the file window is no longer the beginning of a line
     * after refreshing, the file reader should start from the first new line character
     * found immediately before the current position. If after doing that, the
     * number of lines found is smaller than the size of the file window,
     * then the file reader should read lines before the first line that was read,
     * until it can fill the file window, as long as more lines are available.
     *
     * @return the current file window, or nothing if the file does not exist
     */
    Optional<? extends List<String>> refresh();

    /**
     * @return the file associated with this instance.
     */
    File getFile();

    /**
     * Close this instance.
     */
    void close();
}
