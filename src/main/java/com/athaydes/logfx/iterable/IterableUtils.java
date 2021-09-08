package com.athaydes.logfx.iterable;

import java.util.ArrayList;
import java.util.Collection;
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
}
