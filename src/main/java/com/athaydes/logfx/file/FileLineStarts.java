package com.athaydes.logfx.file;

import java.util.TreeSet;

/**
 * Representation of a file line start indexes.
 */
class FileLineStarts {

    private final int size;
    private final TreeSet<Long> indexes = new TreeSet<>();

    FileLineStarts( int size ) {
        if ( size < 1L ) {
            throw new IllegalArgumentException( "Size must be positive" );
        }
        this.size = size;
    }

    void addFirst( long index ) {
        indexes.add( index );
        trim( false );
    }

    void addLast( long index ) {
        indexes.add( index );
        trim( true );
    }

    void clear() {
        indexes.clear();
    }

    long getFirst() {
        if ( indexes.isEmpty() ) {
            return 0L;
        } else {
            return indexes.first();
        }
    }

    long getLast() {
        if ( indexes.isEmpty() ) {
            return 0L;
        } else {
            return indexes.last();
        }
    }

    private void trim( boolean fromBeginning ) {
        Runnable remove = fromBeginning ?
                () -> indexes.remove( getFirst() ) :
                () -> indexes.remove( getLast() );

        while ( indexes.size() > size ) {
            remove.run();
        }
    }

    @Override
    public String toString() {
        return "FileLineStarts{" +
                "indexes=" + indexes +
                '}';
    }
}
