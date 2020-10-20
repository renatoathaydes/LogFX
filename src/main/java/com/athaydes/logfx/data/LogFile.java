package com.athaydes.logfx.data;

import com.athaydes.logfx.binding.BindableValue;

import java.io.File;
import java.util.Objects;

public final class LogFile {
    public final File file;
    private final BindableValue<String> highlightGroup = new BindableValue<>( "" );

    public LogFile( File file ) {
        this.file = file;
    }

    public LogFile( File file, String highlightGroupName ) {
        this.file = file;
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
