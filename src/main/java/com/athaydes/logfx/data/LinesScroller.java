package com.athaydes.logfx.data;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class LinesScroller {
    private final int listSize;
    private final Function<Integer, String> getLineAt;
    private final BiConsumer<Integer, String> lineSetter;

    public LinesScroller( int listSize,
                          Function<Integer, String> getLineAt,
                          BiConsumer<Integer, String> lineSetter ) {
        this.listSize = listSize;
        this.getLineAt = getLineAt;
        this.lineSetter = lineSetter;
    }

    public void setTopLines( List<String> lines ) {
        int shift = Math.min( listSize, lines.size() );
        if ( shift < listSize ) {
            shiftLinesDownBy( shift );
        }
        for ( int i = 0; i < shift; i++ ) {
            lineSetter.accept( i, lines.get( i ) );
        }
    }

    public void setBottomLines( List<String> lines ) {
        int shift = Math.min( listSize, lines.size() );
        int linesOffset;
        if ( shift < listSize ) {
            shiftLinesUpBy( shift );
            linesOffset = 0;
        } else {
            linesOffset = lines.size() - listSize;
        }
        int initialIndex = listSize - shift;
        for ( int i = 0; i < shift; i++ ) {
            lineSetter.accept( i + initialIndex, lines.get( i + linesOffset ) );
        }
    }

    private void shiftLinesDownBy( int shift ) {
        for ( int i = listSize - 1; i >= shift; i-- ) {
            String contents = getLineAt.apply( i - shift );
            lineSetter.accept( i, contents );
        }
    }

    private void shiftLinesUpBy( int shift ) {
        int lastIndex = listSize - shift;
        for ( int i = 0; i < lastIndex; i++ ) {
            String contents = getLineAt.apply( i + shift );
            lineSetter.accept( i, contents );
        }
    }
}
