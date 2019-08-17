package com.athaydes.logfx.file;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

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
     * Set the line filter being used by this reader.
     * <p>
     * If the given filter is null, any filter that had been previously set will be unset.
     *
     * @param lineFilter a filter that may or may not accept lines from the file. The result only included lines
     *                   that were accepted by the filter.
     */
    void setLineFilter( Predicate<String> lineFilter );

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
     * If the date-time was not within the file first/last date-times, the result will still
     * be successful.
     *
     * @param dateTime      to move the file window to
     * @param dateExtractor function from a log line to the date the log line contains
     * @return a successful result if the dates in the log file could be found,
     * or an unsuccessful result otherwise.
     * Returning success implies that the file window was successfully moved. If this method
     * returns a unsuccessful result, the file window is left intact.
     */
    FileQueryResult moveTo( ZonedDateTime dateTime,
                            Function<String, Optional<ZonedDateTime>> dateExtractor );

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

    /**
     * Result of performing a query into the file contents.
     */
    interface FileQueryResult {

        /**
         * @return true if this is a successful query, false if it was not successful for any
         * reason.
         */
        boolean isSuccess();

        /**
         * If this query is successful, the line number of the first match within
         * the file window.
         * <p>
         * The result is between 1 and the file-window size for successful queries,
         * and undefined for unsuccessful ones or results before or after the current range.
         *
         * @return the line number of the first match in the current file-window if this
         * is a successful query.
         */
        int fileLineNumber();

        /**
         * @return true if the result is before the current range being looked at.
         * If this is the case, the line number is undefined.
         */
        boolean isBeforeRange();

        /**
         * @return true if the result is after the current range being looked at.
         * If this is the case, the line number is undefined.
         */
        boolean isAfterRange();
    }
}
