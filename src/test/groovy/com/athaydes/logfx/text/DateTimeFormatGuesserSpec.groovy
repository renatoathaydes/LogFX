package com.athaydes.logfx.text

import spock.lang.Specification
import spock.lang.Unroll

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class DateTimeFormatGuesserSpec extends Specification {

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
            def parsedDate = result.get().convert( line as String )
            assert line && parsedDate.isPresent()
            assert line && parsedDate.get().toInstant() == instant
        }

        where: 'valid log lines and their expected date-times'
        lines << [
                [ '2017-09-11T18:13:57.483+02:00 TRACE {worker-1} com.acme.Log event' ],
                [ '2017-09-11T18:13:57:484+0400 TRACE {worker-1} com.acme.Log event' ],
                [ '2017-9-14T19:23:53.499+02:00 [pool-6-thread-1] DEBUG com' ],
                [ '2013-2-23T5:6:7 [pool-6-thread-1] DEBUG com' ],
                [ 'INFO 2017-09-11T18:13:57:485+0400 {worker-1} com.acme.Log event' ],
                [ 'INFO Fri Sep 01 22:02:55 CEST 2017 - 22',
                  'Sun Nov 03 14:22:00 CEST 2019 msg' ],
                [ '[Fri Sep 09 10:42:29.902 ART 2011] [core:error]' ],
                [ '[10/Oct/2000:13:55:36 -0700] INFO hello' ],
                [ 'Sun Sep 10 03:30:20.271 <kernel> wl0',
                  '2015-12-21 10:48:38.037 INFO  [0] [Licensing]  --> This' ],
        ]

        expectedDateTimes << [
                [ dateTime( '2017-09-11T18:13:57.483+02:00' ) ],
                [ dateTime( '2017-09-11T18:13:57.484+04:00' ) ],
                [ dateTime( '2017-09-14T19:23:53.499+02:00' ) ],
                [ dateTime( '2013-02-23T05:06:07.000+00:00' ) ],
                [ dateTime( '2017-09-11T18:13:57.485+04:00' ) ],
                [ dateTime( '2017-09-01T22:02:55+02:00' ),
                  dateTime( '2019-11-03T14:22:00+01:00' ) ],
                [ dateTime( '2011-09-09T10:42:29.902-03:00' ) ],
                [ dateTime( '2000-10-10T13:55:36.000-07:00' ) ],
                [ dateTime( thisYear() + '-09-10T03:30:20.271+00:00' ),
                  dateTime( '2015-12-21T10:48:38.037+00:00' ) ],
        ]
    }

    private static thisYear() {
        LocalDateTime.now().getYear()
    }

    private static Instant dateTime( String value ) {
        try {
            return ZonedDateTime.parse( value, DateTimeFormatter.ISO_DATE_TIME ).toInstant()
        } catch ( DateTimeParseException ignore ) {
            // without timezone
            return LocalDateTime.parse( value, DateTimeFormatter.ISO_DATE_TIME )
                    .toInstant( ZoneOffset.UTC )
        }
    }

}
