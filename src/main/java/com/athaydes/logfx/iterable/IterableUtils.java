package com.athaydes.logfx.iterable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class IterableUtils {
    public static <N> Optional<N> getFirst( Iterable<N> iterable ) {
        N item = null;
        var iter = iterable.iterator();
        if ( iter.hasNext() ) {
            item = iter.next();
        }
        return Optional.ofNullable( item );
    }

    public static <N> Optional<N> getLast( Iterable<N> iterable ) {
        N item = null;
        for ( N n : iterable ) {
            item = n;
        }
        return Optional.ofNullable( item );
    }

    public static <T> Collection<T> append( T item, Collection<T> collection ) {
        var result = new ArrayList<T>( collection.size() + 1 );
        result.add( item );
        result.addAll( collection );
        return result;
    }

    public static <T> T getAt( int index, List<T> collection, T defaultValue ) {
        if ( index >= 0 && !collection.isEmpty() && index < collection.size() ) {
            return collection.get( index );
        }
        return defaultValue;
    }

    public static double midPoint( List<Double> collection, int index ) {
        if ( collection.isEmpty() ) return 0.5;
        var before = getAt( Math.min( index, collection.size() ) - 1, collection, 0.0 );
        var after = getAt( index, collection, 1.0 );
        return before + ( ( after - before ) / 2.0 );
    }
}
