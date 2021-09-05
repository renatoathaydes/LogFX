package com.athaydes.logfx.text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToIntBiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.time.temporal.TemporalQueries.offset;
import static java.time.temporal.TemporalQueries.zoneId;
import static java.util.stream.Collectors.joining;

/**
 * A utility class that can be used to guess the format of dates within a log file
 * by analysing a sample of lines from the file.
 */
public final class DateTimeFormatGuesser {

    private static final Logger log = LoggerFactory.getLogger( DateTimeFormatGuesser.class );
    public static final int MAX_CHARS_TO_LOOK_FOR_DATE = 250;
    private static final String DATE_TIME_GROUP = "datetime";

    private final Collection<SingleDateTimeFormatGuess> dateTimeFormatGuesses;
    private final MultiDateTimeFormatGuess multiGuess;

    private static volatile DateTimeFormatGuesser STANDARD_INSTANCE;

    static DateTimeFormatGuesser createStandard() {
        String time = "\\d{1,2}[:.]\\d{1,2}[:.]\\d{1,2}([:.]\\d{1,9})?(\\s{0,2}[+-]\\d{1,2}(:)?\\d{1,2})?";

        // 2017-09-11T18:13:57.483+02:00
        String isoDateTime = "\\d{1,4}-\\d{1,4}-\\d{1,4}T" + time;

        // 2015-12-21 10:48:38.037
        String isoDateTime1 = "\\d{1,4}-\\d{1,4}-\\d{1,4}\\s+" + time;

        // Tue, 3 Jun 2008 11:05:30 GMT
        String rfc1123DateTime = "\\w{1,3},\\s+\\d{1,2}\\s+\\w{1,10}\\s+\\d{2,4}\\s+" + time + "(\\s+\\w{1,10})?";

        // Tue Aug 11 21:55:22 CEST 2020
        String commonDateTime = "(\\w{3}\\s+)?\\w{3}\\s+\\d{1,2}\\s+" + time + "(\\s+\\w{1,10}\\s+\\d{2,4})?";

        // 10/Oct/2000:13:55:36 -0700
        String ncsaCommonLogFormat = "\\w{1,10}[/\\-.]\\w{1,10}[/\\-.]\\d{2,4}([T\\s:]|\\s{1,2})" + time;

        List<String> patterns = Arrays.asList(
                isoDateTime, // 1
                rfc1123DateTime, // 2
                isoDateTime, // 3
                isoDateTime, // 4
                isoDateTime1, // 5
                commonDateTime, // 6
                ncsaCommonLogFormat // 7
        );

        List<DateTimeFormatter> formatters = Arrays.asList(
                DateTimeFormatter.ISO_DATE_TIME, // 1
                DateTimeFormatter.RFC_1123_DATE_TIME, // 2
                DateTimeFormatter.ofPattern( "yyyy-M-d'T'H:m:s[:SSS][Z]" ), // 3
                DateTimeFormatter.ofPattern( "yyyy-M-d'T'H:m:s[.SSS][z]" ), // 4
                DateTimeFormatter.ofPattern( "yyyy-M-d H:m:s[.SSS][Z]" ), // 5
                DateTimeFormatter.ofPattern( "[EE ]MMM dd HH:mm:ss[.SSS][ zzz][ yyyy]" ), // 6
                DateTimeFormatter.ofPattern( "d/MMM/yyyy:H:m:s[:SSS][ Z]" ) // 7
        );

        return new DateTimeFormatGuesser( patterns, formatters );
    }

    public static DateTimeFormatGuesser standard() {
        if ( STANDARD_INSTANCE == null ) {
            synchronized ( DateTimeFormatGuesser.class ) {
                if ( STANDARD_INSTANCE == null ) {
                    STANDARD_INSTANCE = createStandard();
                }
            }
        }
        return STANDARD_INSTANCE;
    }

    DateTimeFormatGuesser( List<String> patterns, List<DateTimeFormatter> formatters ) {

        List<SingleDateTimeFormatGuess> guesses = new ArrayList<>( patterns.size() );

        Iterator<Pattern> patternsIter = patterns.stream()
                .map( p -> Pattern.compile( "(?<" + DATE_TIME_GROUP + ">" + p + ")" ) )
                .iterator();
        Iterator<DateTimeFormatter> formattersIter = formatters.iterator();

        while ( patternsIter.hasNext() && formattersIter.hasNext() ) {
            guesses.add( new SingleDateTimeFormatGuess( patternsIter.next(), formattersIter.next() ) );
        }

        assert !patternsIter.hasNext() && !formattersIter.hasNext() :
                "number of patterns and formatters is not the same";

        this.dateTimeFormatGuesses = guesses;
        this.multiGuess = new MultiDateTimeFormatGuess( guesses );

        if ( log.isTraceEnabled() ) {
            log.trace( "New instance with patterns:\n  - {}", guesses.stream()
                    .map( Object::toString )
                    .collect( joining( "\n  - " ) ) );
        }
    }

    /**
     * @return this guesser as a {@link DateTimeFormatGuess} so that it can be used to guess Date-Time in any
     * of its formats.
     */
    public DateTimeFormatGuess asGuess() {
        return multiGuess;
    }

    /**
     * Find out which date-time format a logger uses based on the given sample lines.
     * <p>
     * All formats that can match something in the logger will be included in the returned guesser. If more than one
     * format is found, the different formats will be sorted by the frequency of matches they had, so that the most
     * likely format is used first.
     *
     * @param lines sample log lines
     * @return guesser if possible
     */
    public Optional<DateTimeFormatGuess> guessDateTimeFormats( Iterable<String> lines ) {
        Map<SingleDateTimeFormatGuess, Integer> countByGuess = new HashMap<>( 3 );

        log.trace( "Trying to guess date-time formats in log using {} guesses", dateTimeFormatGuesses.size() );

        long start = System.currentTimeMillis();

        for ( String line : lines ) {
            // only look for dates within the first 250 characters
            line = line.substring( 0, Math.min( MAX_CHARS_TO_LOOK_FOR_DATE, line.length() ) );
            boolean patternFound = false;

            for ( SingleDateTimeFormatGuess guess : dateTimeFormatGuesses ) {
                if ( guess.guessDateTime( line ).isPresent() ) {
                    log.trace( "Guess {} matched line: {}", guess, line );
                    patternFound = true;
                    countByGuess.merge( guess, 1, Integer::sum );
                    break; // one success per line is all that should be possible
                }
            }

            if ( !patternFound ) {
                log.trace( "No matching pattern for line: {}", line );
            }
        }

        if ( log.isInfoEnabled() ) {
            log.info( "Found {} date-time-formatter guesses in provided file sample (search took {} ms)",
                    countByGuess.size(), System.currentTimeMillis() - start );
        }

        log.debug( "Occurrences of date-time patterns in log file lines: {}", countByGuess );

        if ( countByGuess.isEmpty() ) {
            return Optional.empty();
        }

        if ( countByGuess.size() == 1 ) {
            return Optional.of( countByGuess.keySet().iterator().next() );
        }

        return Optional.of( new MultiDateTimeFormatGuess( sortByOccurrences( countByGuess ) ) );
    }

    private static Collection<SingleDateTimeFormatGuess> sortByOccurrences(
            Map<SingleDateTimeFormatGuess, Integer> countByGuess ) {
        List<SingleDateTimeFormatGuess> guesses = new ArrayList<>( countByGuess.keySet() );
        guesses.sort( ( a, b ) -> {
            Integer aGuess = countByGuess.get( a );
            Integer bGuess = countByGuess.get( b );
            return bGuess.compareTo( aGuess );
        } );
        log.trace( "Date-time guesses sorted by occurrences: {}", guesses );
        return guesses;
    }

    static final class MultiDateTimeFormatGuess implements DateTimeFormatGuess {
        private final Collection<SingleDateTimeFormatGuess> guesses;

        private MultiDateTimeFormatGuess( Collection<SingleDateTimeFormatGuess> guesses ) {
            this.guesses = guesses;
        }

        @Override
        public Optional<ZonedDateTime> guessDateTime( String line ) {
            line = line.substring( 0, Math.min( MAX_CHARS_TO_LOOK_FOR_DATE, line.length() ) );

            for ( SingleDateTimeFormatGuess guess : guesses ) {
                Optional<ZonedDateTime> result = guess.guessDateTime( line );
                if ( result.isPresent() ) {
                    return result;
                }
            }
            return Optional.empty();
        }

        Collection<SingleDateTimeFormatGuess> getGuesses() {
            return guesses;
        }
    }

    private static final class SingleDateTimeFormatGuess implements DateTimeFormatGuess {

        private static final int THIS_YEAR = ZonedDateTime.now().getYear();

        private final Pattern pattern;
        private final DateTimeFormatter formatter;

        private SingleDateTimeFormatGuess( Pattern pattern, DateTimeFormatter formatter ) {
            this.pattern = pattern;
            this.formatter = formatter;
        }

        @Override
        public Optional<ZonedDateTime> guessDateTime( String line ) {
            Matcher matcher = pattern.matcher( line );
            if ( matcher.find() ) {
                try {
                    return dateTimeFromTemporal( formatter.parse(
                            matcher.group( DateTimeFormatGuesser.DATE_TIME_GROUP ) ) );
                } catch ( Exception e ) {
                    log.debug( "Failed to extract date from line due to {}: {}", e, line );
                }
            }

            return Optional.empty();
        }

        private static Optional<ZonedDateTime> dateTimeFromTemporal( TemporalAccessor ta ) {
            ToIntBiFunction<ChronoField, Integer> getField = ( field, orElse ) ->
                    ta.isSupported( field ) ? ta.get( field ) : orElse;

            ZoneId zone = Optional.<ZoneId>ofNullable( ta.query( offset() ) )
                    .orElse( Optional.ofNullable( ta.query( zoneId() ) )
                            .orElse( ZoneOffset.UTC ) );

            int year = getField.applyAsInt( ChronoField.YEAR, THIS_YEAR );
            int month = getField.applyAsInt( ChronoField.MONTH_OF_YEAR, 1 );
            int day = getField.applyAsInt( ChronoField.DAY_OF_MONTH, 1 );
            int hour = getField.applyAsInt( ChronoField.HOUR_OF_DAY, 0 );
            int minute = getField.applyAsInt( ChronoField.MINUTE_OF_HOUR, 0 );
            int second = getField.applyAsInt( ChronoField.SECOND_OF_MINUTE, 0 );
            int nanos = getField.applyAsInt( ChronoField.NANO_OF_SECOND, 0 );

            ZonedDateTime dateTime = ZonedDateTime.of( year, month, day, hour, minute, second, nanos, zone );

            return Optional.of( dateTime );
        }

        @Override
        public boolean equals( Object other ) {
            if ( this == other ) {
                return true;
            }
            if ( other == null || getClass() != other.getClass() ) {
                return false;
            }

            var that = ( SingleDateTimeFormatGuess ) other;

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
