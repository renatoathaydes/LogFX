package com.athaydes.logfx.text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility class that can be used to guess the format of dates within a log file
 * by analysing a sample of lines from the file.
 */
public class DateTimeFormatGuesser {

    private static final Logger log = LoggerFactory.getLogger( DateTimeFormatGuesser.class );

    private final Set<Pattern> logLinePatterns;
    private final Set<DateTimeFormatter> logLineDateFormatters;

    public static DateTimeFormatGuesser standard() {
        Set<Pattern> patterns = new LinkedHashSet<>();
        Set<DateTimeFormatter> formatters = new LinkedHashSet<>();

        String time = "\\d{1,2}[:.]\\d{1,2}[:.]\\d{1,2}([:.]\\d{1,9})?(\\s{0,2}[+-]\\d{1,2}(:)?\\d{1,2})?";

        // 2017-09-11T18:13:57.483+02:00
        String isoDateTime = "\\d{1,4}-\\d{1,4}-\\d{1,4}(T|\\s{1,2})" + time;

        // (Tue,) 3 Jun 2008 11:05:30 GMT
        String rfc1123DateTime = "\\w{1,3}\\s+\\w{1,10}\\s+\\d{2,4}\\s+" + time + "(\\s+\\w+)?";

        // (Fri) Sep 01 22:02:57 CEST 2017
        String longDateTime = "\\w{3,10}\\s+\\d{1,2}\\s+" + time + "\\s+(\\w+\\s+)?\\d{2,4}";

        // 10/Oct/2000:13:55:36 -0700
        String ncsaCommonLogFormat = "\\w{1,10}[/\\-.]\\w{1,10}[/\\-.]\\d{2,4}([T\\s:]|\\s{1,2})" + time;

        String[] formats = { isoDateTime, rfc1123DateTime, longDateTime, ncsaCommonLogFormat };

        // wrap formats into a main group (maybe more complex wrappers in the future?)
        for ( String format : formats ) {
            patterns.add( Pattern.compile( "(" + format + ")" ) );
        }

        formatters.addAll( Arrays.asList(
                DateTimeFormatter.ISO_DATE_TIME,
                DateTimeFormatter.RFC_1123_DATE_TIME,
                DateTimeFormatter.ofPattern( "yyyy-M-d'T'HH:mm:ss.SSSz" ),
                DateTimeFormatter.ofPattern( "yyyy-M-d'T'HH:mm:ss:SSSZ" ),
                DateTimeFormatter.ofPattern( "MMM dd HH:mm:ss[.SSS] z yyyy" ),
                DateTimeFormatter.ofPattern( "LL dd HH:mm:ss.SSS" )
        ) );

        return new DateTimeFormatGuesser( patterns, formatters );
    }

    public DateTimeFormatGuesser( Set<Pattern> logLinePatterns,
                                  Set<DateTimeFormatter> logLineDateFormatters ) {
        this.logLinePatterns = logLinePatterns;
        this.logLineDateFormatters = logLineDateFormatters;
    }

    public Optional<DateTimeFormatGuess> guessDateTimeFormats( Iterable<String> lines ) {
        Set<SingleDateTimeFormatGuess> result = new HashSet<>( 3 );

        log.trace( "Trying to guess date-time formats in log using {} patterns and {} date-time formatters",
                logLinePatterns.size(), logLineDateFormatters.size() );

        for ( String line : lines ) {
            boolean patternFound = false;

            findGuessForLine:
            for ( Pattern pattern : logLinePatterns ) {
                Matcher matcher = pattern.matcher( line );

                if ( matcher.find() ) {
                    patternFound = true;
                    String dateTimePart = matcher.group( 1 );
                    log.trace( "Line matched pattern (extracted date-time: '{}'): {}", dateTimePart, pattern );
                    for ( DateTimeFormatter formatter : logLineDateFormatters ) {
                        SingleDateTimeFormatGuess guess = new SingleDateTimeFormatGuess( pattern, formatter );
                        if ( guess.convert( line ).isPresent() ) {
                            log.debug( "Found guess: {} - for line: {}", guess, line );
                            result.add( guess );
                            break findGuessForLine; // one success per line is all that should be possible
                        }
                    }

                    log.trace( "Found pattern, but not DateTimeFormatter for line: {}", line );
                }
            }

            if ( !patternFound ) {
                log.trace( "No matching pattern for line: {}", line );
            }
        }

        log.debug( "Found {} date-time-formatter guesses in provided sample", result.size() );

        if ( result.isEmpty() ) {
            return Optional.empty();
        }

        return Optional.of( new MultiDateTimeFormatGuess( result ) );
    }


    private static final class MultiDateTimeFormatGuess implements DateTimeFormatGuess {
        private final Set<SingleDateTimeFormatGuess> guesses;

        private MultiDateTimeFormatGuess( Set<SingleDateTimeFormatGuess> guesses ) {
            this.guesses = guesses;
        }

        @Override
        public Optional<ZonedDateTime> convert( String line ) {
            for ( SingleDateTimeFormatGuess guess : guesses ) {
                Optional<ZonedDateTime> result = guess.convert( line );
                if ( result.isPresent() ) {
                    return result;
                }
            }
            return Optional.empty();
        }
    }

    private static final class SingleDateTimeFormatGuess implements DateTimeFormatGuess {
        private final Pattern pattern;
        private final DateTimeFormatter formatter;

        private SingleDateTimeFormatGuess( Pattern pattern, DateTimeFormatter formatter ) {
            this.pattern = pattern;
            this.formatter = formatter;
        }

        @Override
        public Optional<ZonedDateTime> convert( String line ) {
            Matcher matcher = pattern.matcher( line );
            if ( matcher.find() ) {
                try {
                    return Optional.of( ZonedDateTime.parse( matcher.group( 1 ), formatter ) );
                } catch ( Exception e ) {
                    log.debug( "Failed to extract date from line: {}", line );
                }
            }

            return Optional.empty();
        }

        @Override
        public boolean equals( Object other ) {
            if ( this == other ) {
                return true;
            }
            if ( other == null || getClass() != other.getClass() ) {
                return false;
            }

            SingleDateTimeFormatGuess that = ( SingleDateTimeFormatGuess ) other;

            return pattern.equals( that.pattern ) && formatter.equals( that.formatter );
        }

        @Override
        public int hashCode() {
            int result = pattern.hashCode();
            result = 31 * result + formatter.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "SingleDateTimeFormatGuess{" +
                    "pattern=" + pattern +
                    ", formatter=" + formatter +
                    '}';
        }
    }


}
