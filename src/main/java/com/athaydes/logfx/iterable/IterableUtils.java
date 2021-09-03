package com.athaydes.logfx.iterable;

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
}
