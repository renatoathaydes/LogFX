package com.athaydes.logfx.text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.athaydes.logfx.text.PatternBasedDateTimeFormatGuess.DATE_TIME_GROUP;
import static com.athaydes.logfx.text.PatternBasedDateTimeFormatGuess.TIMEZONE_GROUP;
import static com.athaydes.logfx.text.PatternBasedDateTimeFormatGuess.namedGroup;

/**
 * A utility class that can be used to guess the format of dates within a log file
 * by analysing a sample of lines from the file.
 */
public final class DateTimeFormatGuesser {

    private static final Logger log = LoggerFactory.getLogger( DateTimeFormatGuesser.class );

    public static final int MAX_CHARS_TO_LOOK_FOR_DATE = 500;

    private final MultiDateTimeFormatGuess multiGuess;

    private static volatile DateTimeFormatGuesser STANDARD_INSTANCE;

    static DateTimeFormatGuesser createStandard() {
        var prefixRegex = "[a-zA-Z\\[\\]_ -]{0,20}";
        var time = "\\d{1,2}:\\d{1,2}:\\d{1,2}";
        var timeMs = time + "(\\.\\d{1,6})?";

        // 2017-09-11T18:13:57.483+02:00
        var isoDateTime = prefixRegex + namedGroup( DATE_TIME_GROUP,
                "\\d{1,4}-\\d{1,4}-\\d{1,4}T" + timeMs +
                        namedGroup( TIMEZONE_GROUP, "\\s{0,2}[+-]\\d{1,2}(:)?\\d{1,2}" ) + "?" );

        // Tue, 3 Jun 2008 11:05:30 GMT
        var rfc1123DateTime = prefixRegex + namedGroup( DATE_TIME_GROUP,
                "\\w{3},\\s+\\d{1,2}\\s+\\w{1,10}\\s+\\d{2,4}\\s+" + time +
                        namedGroup( TIMEZONE_GROUP, "\\s+(GMT|[+-]\\d{4})" ) );

        // Tue Aug 11 21:55:22 CEST 2020
        String commonDateTime = prefixRegex + namedGroup( DATE_TIME_GROUP,
                "(\\w{3}\\s+)?\\w{3}\\s+\\d{1,2}\\s+" + time +
                        namedGroup( TIMEZONE_GROUP, "\\s+\\w{1,10}\\s+\\d{2,4}" ) );

        // 10/Oct/2000:13:55:36 -0700
        String ncsaCommonLogFormat = prefixRegex + namedGroup( DATE_TIME_GROUP,
                "\\w{1,10}[/\\-.]\\w{1,10}[/\\-.]\\d{2,4}([T\\s:]|\\s{1,2})" + time +
                        namedGroup( TIMEZONE_GROUP, "\\s{0,2}[+-]\\d{1,2}(:)?\\d{1,2}" ) + "?" );

        return new DateTimeFormatGuesser( List.of(
                new PatternBasedDateTimeFormatGuess( Pattern.compile( isoDateTime + ".*" ),
                        DateTimeFormatter.ofPattern( "yyyy-M-d'T'H:m:s[.SSS][z]" ) ),
                new PatternBasedDateTimeFormatGuess( Pattern.compile( commonDateTime + ".*" ),
                        DateTimeFormatter.ofPattern( "[EE ]MMM dd HH:mm:ss[.SSS][ zzz][ yyyy]" ) ),
                new PatternBasedDateTimeFormatGuess( Pattern.compile( ncsaCommonLogFormat + ".*" ),
                        DateTimeFormatter.ofPattern( "d/MMM/yyyy:H:m:s[:SSS][ Z]" ) ),
                new PatternBasedDateTimeFormatGuess( Pattern.compile( rfc1123DateTime + ".*" ),
                        DateTimeFormatter.RFC_1123_DATE_TIME ) )
        );
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

    DateTimeFormatGuesser( List<? extends DateTimeFormatGuess> guessers ) {
        this.multiGuess = new MultiDateTimeFormatGuess( guessers );
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
        Map<DateTimeFormatGuess, Integer> countByGuess = new HashMap<>( 3 );

        var guessers = multiGuess.getGuesses();

        log.trace( "Trying to guess date-time formats in log using {} guesses", guessers.size() );

        long start = System.currentTimeMillis();

        for ( String line : lines ) {
            // only look for dates within the first 250 characters
            line = line.substring( 0, Math.min( MAX_CHARS_TO_LOOK_FOR_DATE, line.length() ) );
            boolean patternFound = false;

            for ( var guess : guessers ) {
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

    private static Collection<DateTimeFormatGuess> sortByOccurrences(
            Map<DateTimeFormatGuess, Integer> countByGuess ) {
        var guesses = new ArrayList<>( countByGuess.keySet() );
        guesses.sort( ( a, b ) -> {
            Integer aGuess = countByGuess.get( a );
            Integer bGuess = countByGuess.get( b );
            return bGuess.compareTo( aGuess );
        } );
        log.trace( "Date-time guesses sorted by occurrences: {}", guesses );
        return guesses;
    }

    static final class MultiDateTimeFormatGuess implements DateTimeFormatGuess {
        private final Collection<? extends DateTimeFormatGuess> guesses;

        private MultiDateTimeFormatGuess( Collection<? extends DateTimeFormatGuess> guesses ) {
            this.guesses = guesses;
        }

        @Override
        public Optional<ZonedDateTime> guessDateTime( String line ) {
            var effectiveLine = line.substring( 0, Math.min( MAX_CHARS_TO_LOOK_FOR_DATE, line.length() ) );

            return guesses.stream()
                    .map( guesser -> guesser.guessDateTime( effectiveLine ) )
                    .filter( Optional::isPresent )
                    .map( Optional::get )
                    .findFirst();
        }

        Collection<? extends DateTimeFormatGuess> getGuesses() {
            return guesses;
        }
    }

}
