package com.athaydes.logfx.text

import groovy.transform.CompileStatic
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.regex.Pattern

import static com.athaydes.logfx.text.PatternBasedDateTimeFormatGuess.*

class DateTimeFormatGuesserSpec extends Specification {

    static final Locale systemDefaultLocale = Locale.getDefault()

    def setupSpec() {
        Locale.setDefault( Locale.ENGLISH )
    }

    def cleanupSpec() {
        Locale.setDefault( systemDefaultLocale )
    }

    @Unroll
    def "Should be able to guess the format of most logs"() {
        given: 'The standard date-time format guesser'
        def guesser = DateTimeFormatGuesser.standard()

        when: 'The guesser tries to guess the formats for a few log lines'
        def result = guesser.guessDateTimeFormats( lines )

        then: 'The guesser can find a result'
        result.isPresent()

        and: 'All lines can be correctly parsed to date-times'
        [ lines, expectedDateTimes ].transpose().each { line, Instant instant ->
            def parsedDate = result.get().guessDateTime( line as String )
            assert line && parsedDate.isPresent()
            assert line && parsedDate.get().toInstant() == instant
        }

        where: 'valid log lines and their expected date-times'
        lines << [
                [ '2021-9-3T20:54:54.207+02:00 [file-change-watcher-1] INFO com.athaydes.logfx.file.FileChangeWatcher - Watching file ' ],
                [ '2021-9-3T20:54:51.97+02:00' ],
                [ '2024-07-02T14:20:14:009+0000 DEBUG {req-359} server' ],
                [ '2017-09-11T18:13:57.483+02:00 TRACE {worker-1} com.acme.Log event' ],
                [ '2017-9-14T19:23:53.499+02:00 [pool-6-thread-1] DEBUG com' ],
                [ '2013-2-23T5:6:7 [pool-6-thread-1] DEBUG com' ],
                [ 'INFO 2017-09-11T18:13:57.485+04:00 {worker-1} com.acme.Log event' ],
                [ 'INFO Fri Sep 01 22:02:55 CEST 2017 - 22',
                  'Sun Nov 03 14:22:00 CEST 2019 msg' ],
                [ '[Fri, 09 Sep 2011 10:42:29 GMT] [core:error]' ],
                [ '[Fri, 09 Sep 2011 10:42:29 +0200] [core:error]' ],
                [ '[Fri, 09 Sep 2011 10:42:29 -0530] [core:error]' ],
                [ '[10/Oct/2000:13:55:36 -0700] INFO hello' ],
                [ '2020-06-21 18:32:45-07 INFO hello' ],
        ]

        expectedDateTimes << [
                [ dateTime( '2021-09-03T20:54:54.207+02:00' ) ],
                [ dateTime( '2021-09-03T20:54:51.97+02:00' ) ],
                [ dateTime( '2024-07-02T14:20:14.009+00:00' ) ],
                [ dateTime( '2017-09-11T18:13:57.483+02:00' ) ],
                [ dateTime( '2017-09-14T19:23:53.499+02:00' ) ],
                [ dateTime( '2013-02-23T05:06:07.000' ) ],
                [ dateTime( '2017-09-11T18:13:57.485+04:00' ) ],
                [ dateTime( '2017-09-01T22:02:55+02:00' ),
                  dateTime( '2019-11-03T14:22:00+01:00' ) ],
                [ dateTime( '2011-09-09T10:42:29+00:00' ) ],
                [ dateTime( '2011-09-09T10:42:29+02:00' ) ],
                [ dateTime( '2011-09-09T10:42:29-05:30' ) ],
                [ dateTime( '2000-10-10T13:55:36-07:00' ) ],
                [ dateTime( '2020-06-21T18:32:45-07:00' ) ],
        ]
    }

    def "If log file has more than one date format, should guess the most common ones first"() {
        given: 'Log lines with 2 different date-time styles, including lines without any date-time'
        def lines = [
                '2017-09-11T18:13:57.483+02:00 TRACE {worker-1} com.acme.Log event',
                'This line has no date-time',
                '2017-09-14T19:23:53.499+02:00 [pool-6-thread-1] DEBUG com',
                '2013-02-23T05:06:07 [pool-6-thread-1] DEBUG com',
                '2017-09-11T18:13:57.485+04:00 {worker-1} com.acme.Log event',
                'INFO [Fri, 01 Sep 20017 22:02:55 GMT] - 22',
                'Sun, 03 Nov 2019 14:22:00 GMT: msg',
                'This line has no date-time',
                'This line has no date-time',
                'Sun, 10 Sep 2017 03:30:20 GMT <kernel> wl0',
                '2015-12-21T10:48:38.037 INFO  [0] [Licensing]  --> This'
        ]

        and: 'A date-time format guesser with 3 formats, including the 2 present in the log'
        String tz = namedGroup( TIMEZONE_GROUP, '\\s{0,2}[+-]\\d{1,2}(:)?\\d{1,2}' )
        String time = "\\d{1,2}[:.]\\d{1,2}[:.]\\d{1,2}([:.]\\d{1,9})?"

        def patterns = [
                // 2017-09-11T18:13:57.483+02:00
                namedGroup( DATE_TIME_GROUP, "\\d{1,4}-\\d{1,4}-\\d{1,4}(T|\\s{1,2})$time$tz?" ),

                // Tue, 3 Jun 2008 11:05:30 GMT
                namedGroup( DATE_TIME_GROUP, "\\w{1,3},\\s+\\d{1,2}\\s+\\w{1,10}\\s+\\d{2,4}\\s+$time" +
                        namedGroup( TIMEZONE_GROUP, '\\sGMT' ) ),

                // INFO [Tue, 3 Jun 2008 11:05:30 GMT]
                "\\w+\\s+\\[" + namedGroup( DATE_TIME_GROUP, "\\w{1,3},\\s+\\d{1,2}\\s+\\w{1,10}\\s+\\d{2,4}\\s+$time" +
                        namedGroup( TIMEZONE_GROUP, '\\sGMT' ) ) + "]",

                // 10/Oct/2000:13:55:36 -0700
                namedGroup( DATE_TIME_GROUP, "\\w{1,10}[/\\-.]\\w{1,10}[/\\-.]\\d{2,4}([T\\s:]|\\s{1,2})$time$tz" ),
        ].collect { Pattern.compile( "${it}.*" ) }

        def guesser = new DateTimeFormatGuesser( [
                new PatternBasedDateTimeFormatGuess( 'iso', patterns[ 0 ], DateTimeFormatter.ISO_DATE_TIME ),
                new PatternBasedDateTimeFormatGuess( 'rfc1', patterns[ 1 ], DateTimeFormatter.RFC_1123_DATE_TIME ),
                new PatternBasedDateTimeFormatGuess( 'rfc2', patterns[ 2 ], DateTimeFormatter.RFC_1123_DATE_TIME ),
                new PatternBasedDateTimeFormatGuess( 'ncsa', patterns[ 3 ],
                        DateTimeFormatter.ofPattern( "d/MMM/yyyy:H:m:s[:SSS][ Z]", Locale.ENGLISH ) ),
        ] )

        when: 'The guesser tries to guess the formats for a few log lines'
        def result = guesser.guessDateTimeFormats( lines )

        then: 'The guesser can find multiple results'
        result.isPresent()
        def guess = result.get()
        assert guess instanceof DateTimeFormatGuesser.MultiDateTimeFormatGuess

        and: 'The results are sorted by order of occurrences from more to less frequent'
        def guessedFormatters = guess.guesses.toList()
        guessedFormatters.size() == 2

        // verify the guesses can parse the expected formats, first ISO, second RFC-1123
        formatterName( guessedFormatters[ 0 ] ) == 'iso'
        formatterName( guessedFormatters[ 1 ] ) == 'rfc-1123'
    }

    def "A single line with a valid date-time is enough for the guesser to guess a formatter correctly"() {
        given: 'The standard date-time format guesser'
        def guesser = DateTimeFormatGuesser.standard()

        and: 'Lots of lines, only one of which has a valid date-time'
        def lines = generateRandomLines( 100 )
        lines.set( 67, 'INFO [Fri, 1 Sep 2017 22:02:55 GMT] - 22' )

        when: 'The guesser tries to guess the formats'
        def result = guesser.guessDateTimeFormats( lines )

        then: 'The guesser can find the correct result'
        result.isPresent()
        formatterName( result.get() ) == 'rfc-1123'

    }

    @CompileStatic
    private static List<String> generateRandomLines( int count ) {
        Random rand = new Random()
        char[] chars = ' '..'~' as char[]
        def result = new ArrayList<String>( count )
        for ( int line = 1; line <= count; line++ ) {
            int lineWords = 5 + rand.nextInt( 1000 )
            def words = new ArrayList<CharSequence>( lineWords )
            for ( int w = 1; w <= lineWords; w++ ) {
                def wordLength = 2 + rand.nextInt( 50 )
                def sb = new StringBuilder( wordLength )
                for ( int i = 1; i <= wordLength; i++ ) {
                    sb.append( chars[ rand.nextInt( chars.size() ) ] )
                }
                words.add( sb )
            }
            result.add( words.join( ' ' ) )
        }
        return result
    }

    private static String formatterName( DateTimeFormatGuess guess ) {
        if ( guess.guessDateTime( '2017-09-11T18:13:57.485+04:00' ).isPresent() ) return 'iso'
        if ( guess.guessDateTime( 'Sun, 10 Sep 2017 03:30:20 GMT' ).isPresent() ) return 'rfc-1123'
        return 'unknown'
    }

    private static Instant dateTime( String value ) {
        try {
            return ZonedDateTime.parse( value, DateTimeFormatter.ISO_DATE_TIME ).toInstant()
        } catch ( DateTimeParseException ignore ) {
            // without timezone
            def local = LocalDateTime.parse( value, DateTimeFormatter.ISO_DATE_TIME )
            return local.atZone( ZoneId.systemDefault() ).toInstant()
        }
    }

}
