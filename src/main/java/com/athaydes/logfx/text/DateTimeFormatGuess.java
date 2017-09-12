package com.athaydes.logfx.text;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * A date-time formatter guess which can be used to try to extract the date-time from a log line.
 */
public interface DateTimeFormatGuess {

    /**
     * Convert the given line to a date-time if possible.
     *
     * @param line log line that might contain a date-time
     * @return the date-time if the line contained it in a format this guess could understand,
     * empty otherwise.
     */
    Optional<ZonedDateTime> convert( String line );

}
