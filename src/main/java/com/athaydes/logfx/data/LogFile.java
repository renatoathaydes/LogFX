package com.athaydes.logfx.data;

import com.athaydes.logfx.binding.BindableValue;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;

import java.io.File;
import java.util.Objects;

public final class LogFile {
    public static final long DEFAULT_MIN_TIME_GAP = 1_000L;

    public final File file;
    public final LongProperty minTimeGap = new SimpleLongProperty();
    private final BindableValue<String> highlightGroup = new BindableValue<>( "" );

    public LogFile( File file ) {
        this( file, "", DEFAULT_MIN_TIME_GAP );
    }

    public LogFile( File file, String highlightGroupName ) {
        this( file, highlightGroupName, DEFAULT_MIN_TIME_GAP );
    }

    public LogFile( File file, String highlightGroupName, long minTimeGap ) {
        this.file = file;
        this.minTimeGap.set( minTimeGap );
        highlightGroup.setValue( highlightGroupName );
    }

    public BindableValue<String> highlightGroupProperty() {
        return highlightGroup;
    }

    public String getHighlightGroup() {
        return highlightGroup.getValue();
    }

    @Override
    public boolean equals( Object other ) {
        if ( this == other ) return true;
        if ( !( other instanceof LogFile ) ) return false;
        LogFile logFile = ( LogFile ) other;
        return Objects.equals( file, logFile.file );
    }

    @Override
    public int hashCode() {
        return Objects.hash( file );
    }

}
