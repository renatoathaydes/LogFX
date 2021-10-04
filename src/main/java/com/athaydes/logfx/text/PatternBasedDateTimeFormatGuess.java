package com.athaydes.logfx.text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * PatternBasedDateTimeFormatGuess groups together a line pattern that can extract a data-time from a log line,
 * and a formatter which can then parse the date-time.
 * <p>
 * The pattern must use the given group to extract the date-time.
 */
public record PatternBasedDateTimeFormatGuess(
        String name,
        Pattern linePattern,
        DateTimeFormatter formatter
) implements DateTimeFormatGuess {
    public static final String DATE_TIME_GROUP = "dt";
    public static final String TIMEZONE_GROUP = "tz";

    private static final Logger log = LoggerFactory.getLogger( PatternBasedDateTimeFormatGuess.class );

    public static String namedGroup( String name, String regex ) {
        return "(?<" + name + ">" + regex + ")";
    }

    public PatternBasedDateTimeFormatGuess {
        if ( !linePattern.toString().contains( "(?<" + DATE_TIME_GROUP + ">" ) ) {
            throw new IllegalArgumentException( "Pattern does not contain group '" + DATE_TIME_GROUP + "': " + linePattern );
        }
    }

    @Override
    public Optional<ZonedDateTime> guessDateTime( String line ) {
        var match = linePattern.matcher( line );
        if ( match.matches() ) {
            log.trace( "Pattern '{}' matched line '{}'", linePattern, line );
            var dateTimeString = match.group( DATE_TIME_GROUP );
            var hasTimeZone = match.group( TIMEZONE_GROUP ) != null;
            try {
                if ( hasTimeZone ) {
                    return Optional.of( ZonedDateTime.parse( dateTimeString, formatter ) );
                }
                return Optional.of( LocalDateTime.parse( dateTimeString, formatter ).atZone( ZoneId.systemDefault() ) );
            } catch ( DateTimeParseException e ) {
                log.warn( "Unable to parse date-time '{}' due to {}", dateTimeString, e.toString() );
            }
        }
        return Optional.empty();
    }

}
