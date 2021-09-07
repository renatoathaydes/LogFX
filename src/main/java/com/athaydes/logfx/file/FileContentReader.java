package com.athaydes.logfx.file;

import java.io.File;
import java.util.List;
import java.util.Optional;
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
     * @return the file window size used by this reader.
     */
    int fileWindowSize();

    /**
     * Move one file window up and return that.
     *
     * @return next file window up
     */
    default Optional<? extends List<String>> movePageUp() {
        return moveUp( fileWindowSize() );
    }

    /**
     * Move one file window down and return that.
     *
     * @return next file window down
     */
    default Optional<? extends List<String>> movePageDown() {
        return moveDown( fileWindowSize() );
    }

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
     * @return an exact copy of this reader.
     */
    FileContentReader makeCopy();

    void copyState( FileContentReader searchReader );
}
