package com.athaydes.logfx.file;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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
     * Moves the file window so that the first line is the first line immediately at or before
     * the given dateTime, so that the next line must be after the given dateTime.
     * <p>
     * This method will only work if the date in the log file can be identified
     * using the given function.
     * <p>
     * The return value indicates whether it was possible to find dates in the file.
     *
     * @param dateTime      to move the file window to
     * @param dateExtractor function from a log line to the date the log line contains
     * @return true if the dates in the log file could be found, false otherwise.
     * Returning true implies that the file window was successfully moved. If this method
     * returns false, the file window is left intact.
     */
    boolean moveTo( LocalDateTime dateTime,
                    Function<String, Optional<LocalDateTime>> dateExtractor );

    /**
     * Move the file window to the top of the file.
     */
    void top();

    /**
     * Move the file window to the tail of the file.
     */
    void tail();

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
}
