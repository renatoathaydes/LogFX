package com.athaydes.logfx.iterable;

import javafx.collections.ObservableList;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ObservableListView<T, N> {

    private final Class<? extends T> elementType;
    private final ObservableList<? extends N> delegate;

    public ObservableListView( Class<? extends T> elementType, ObservableList<N> delegate ) {
        this.elementType = elementType;
        this.delegate = delegate;
    }

    public ObservableList<? extends N> getList() {
        return delegate;
    }

    public Iterable<T> getIterable() {
        return () -> new Iter( delegate.iterator() );
    }

    private final class Iter implements Iterator<T> {
        private final Iterator<?> iterator;
        private T nextItem;

        public Iter( Iterator<?> iterator ) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            nextItem = null;
            while ( iterator.hasNext() ) {
                var next = iterator.next();
                if ( elementType.isInstance( next ) ) {
                    nextItem = elementType.cast( next );
                    break;
                }
            }
            return nextItem != null;
        }

        @Override
        public T next() {
            if ( nextItem == null ) {
                throw new NoSuchElementException();
            }
            return nextItem;
        }
    }
}
