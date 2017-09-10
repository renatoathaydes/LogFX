package com.athaydes.logfx.text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
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

        // FIXME more patterns, more formatters

        // date followed by at least one space, then anything.
        patterns.add( Pattern.compile( "\\s*([a-zA-Z0-9:+\\-.]{5,50})\\s+.*" ) );

        // TODO pattern for custom formatters
        patterns.add( Pattern.compile( "\\s*[A-z]+\\s+([0-9:+\\-.]+)\\s+.*" ) );

        formatters.addAll( Arrays.asList(
                DateTimeFormatter.ISO_DATE_TIME,
                DateTimeFormatter.RFC_1123_DATE_TIME,
                DateTimeFormatter.ofPattern( "EE LL dd HH:mm:ss.S" ),
                DateTimeFormatter.ofPattern( "LL dd HH:mm:ss.S" )
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

        for ( String line : lines ) {
            findGuessForLine:
            for ( Pattern pattern : logLinePatterns ) {
                Matcher matcher = pattern.matcher( line );
                if ( matcher.matches() ) {
                    for ( DateTimeFormatter formatter : logLineDateFormatters ) {
                        SingleDateTimeFormatGuess guess = new SingleDateTimeFormatGuess( pattern, formatter );
                        if ( guess.convert( line ).isPresent() ) {
                            log.debug( "Found guess: {} - for line: {}", guess, line );
                            result.add( guess );
                            break findGuessForLine; // one success per line is all that should be possible
                        }
                    }
                }
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
        public Optional<LocalDateTime> convert( String line ) {
            for ( SingleDateTimeFormatGuess guess : guesses ) {
                Optional<LocalDateTime> result = guess.convert( line );
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
        public Optional<LocalDateTime> convert( String line ) {
            Matcher matcher = pattern.matcher( line );
            if ( matcher.matches() ) {
                try {
                    return Optional.of( LocalDateTime.parse( matcher.group( 1 ), formatter ) );
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
