package com.athaydes.logfx.data;

import java.util.List;
import java.util.function.Function;

public final class LinesScroller {
    private final int listSize;
    private final Function<Integer, String> getLineAt;
    private final LinesSetter lineSetter;

    public LinesScroller( int listSize,
                          Function<Integer, String> getLineAt,
                          LinesSetter lineSetter ) {

        this.listSize = listSize;
        this.getLineAt = getLineAt;
        this.lineSetter = lineSetter;
    }

    public void setTopLines( List<String> lines ) {
        if ( lines.isEmpty() ) return;
        int shift = Math.min( listSize, lines.size() );
        lineSetter.withSetter( setter -> {
            if ( shift < listSize ) {
                shiftLinesDownBy( shift, setter );
            }
            for ( int i = 0; i < shift; i++ ) {
                setter.set( i, lines.get( i ) );
            }
        } );

    }

    public void setBottomLines( List<String> lines ) {
        if ( lines.isEmpty() ) return;
        int shift = Math.min( listSize, lines.size() );
        lineSetter.withSetter( setter -> {
            int linesOffset;
            if ( shift < listSize ) {
                shiftLinesUpBy( shift, setter );
                linesOffset = 0;
            } else {
                linesOffset = lines.size() - listSize;
            }
            int initialIndex = listSize - shift;
            for ( int i = 0; i < shift; i++ ) {
                setter.set( i + initialIndex, lines.get( i + linesOffset ) );
            }
        } );
    }

    private void shiftLinesDownBy( int shift, LinesSetter.Setter setter ) {
        for ( int i = listSize - 1; i >= shift; i-- ) {
            String contents = getLineAt.apply( i - shift );
            setter.set( i, contents );
        }
    }

    private void shiftLinesUpBy( int shift, LinesSetter.Setter setter ) {
        int lastIndex = listSize - shift;
        for ( int i = 0; i < lastIndex; i++ ) {
            String contents = getLineAt.apply( i + shift );
            setter.set( i, contents );
        }
    }
}
