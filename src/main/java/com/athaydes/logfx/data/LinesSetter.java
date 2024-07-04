package com.athaydes.logfx.data;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class LinesSetter {

    public record LineChange( int index, String text ) {
    }

    public interface Setter {
        void set( int index, String text );
    }

    private final Consumer<List<LineChange>> updater;

    public LinesSetter( Consumer<List<LineChange>> updater ) {
        this.updater = updater;
    }

    void withSetter( Consumer<Setter> useSetter ) {
        var setter = new SetterImpl();
        useSetter.accept( setter );
        updater.accept( setter.changes );
    }

    private static final class SetterImpl implements Setter {
        final List<LineChange> changes = new ArrayList<>();

        @Override
        public void set( int index, String text ) {
            changes.add( new LineChange( index, text ) );
        }
    }
}
