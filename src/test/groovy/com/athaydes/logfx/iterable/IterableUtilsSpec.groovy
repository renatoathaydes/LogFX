package com.athaydes.logfx.iterable

import spock.lang.Specification

class IterableUtilsSpec extends Specification {

    def 'Can get first item of collection'() {
        when: 'getFirst is used'
        def result = IterableUtils.getFirst( collection )

        then: 'the first element is returned, if any'
        result == expected

        where:
        collection   || expected
        [ ]          || Optional.empty()
        [ 'a' ]      || Optional.of( 'a' )
        [ 'b', 'c' ] || Optional.of( 'b' )
        1..10        || Optional.of( 1 )
    }

    def 'Can get last item of collection'() {
        when: 'getLast is used'
        def result = IterableUtils.getLast( collection )

        then: 'the last element is returned, if any'
        result == expected

        where:
        collection   || expected
        [ ]          || Optional.empty()
        [ 'a' ]      || Optional.of( 'a' )
        [ 'b', 'c' ] || Optional.of( 'c' )
        1..10        || Optional.of( 10 )
    }

    @SuppressWarnings( 'GroovyAssignabilityCheck' )
    def 'Can append item to collection'() {
        when: 'append is used'
        def result = IterableUtils.append( item, collection )

        then: 'the element is appended to the collection'
        result == expected

        where:
        collection     | item || expected
        [ ]            | 'a'  || [ 'a' ]
        [ 'a' ]        | 'b'  || [ 'b', 'a' ]
        [ 0, 1, 2, 3 ] | 4    || [ 4, 0, 1, 2, 3 ]
    }

    def 'Can find mid point between two items in a collection'() {
        when: 'we attempt to find the mid-point between two items in a collection'
        def result = IterableUtils.midPoint( collection.collect { it as Double }, index )

        then: 'the result is the precise mid-point'
        doublesEquivalent( result, expected )

        where:
        collection        | index || expected
        [ ]               | 0     || 0.5
        [ ]               | 1     || 0.5
        [ 0.5 ]           | 0     || 0.25
        [ 0.5 ]           | 1     || 0.75
        [ 0.5 ]           | 2     || 0.75
        [ 0.1 ]           | 0     || 0.05
        [ 0.1 ]           | 1     || 0.55
        [ 0.2, 0.7 ]      | 0     || 0.1
        [ 0.2, 0.7 ]      | 1     || 0.45
        [ 0.2, 0.7 ]      | 2     || 0.85
        [ 0.2, 0.7 ]      | 3     || 0.85
        [ 0.3, 0.5, 0.8 ] | 0     || 0.15
        [ 0.3, 0.5, 0.8 ] | 1     || 0.4
        [ 0.3, 0.5, 0.8 ] | 2     || 0.65
        [ 0.3, 0.5, 0.8 ] | 3     || 0.9
        [ 0.3, 0.5, 0.8 ] | 4     || 0.9
    }

    private static boolean doublesEquivalent( double d1, double d2 ) {
        Math.abs( d1 - d2 ) < 0.001
    }

}
