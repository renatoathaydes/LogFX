package com.athaydes.logfx.data;

import java.io.File;
import java.util.Objects;
import java.util.function.Function;

public abstract class LogFile {
    public final File file;

    private LogFile( File file ) {
        this.file = file;
    }

    public abstract <T> T use( Function<SimpleLogFile, T> useFile,
                               Function<LogFileWithHighlightGroup, T> useFileWithGroup );

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

    public static final class SimpleLogFile extends LogFile {
        public SimpleLogFile( File file ) {
            super( file );
        }

        @Override
        public <T> T use( Function<SimpleLogFile, T> useFile,
                          Function<LogFileWithHighlightGroup, T> useFileWithGroup ) {
            return useFile.apply( this );
        }
    }

    public static final class LogFileWithHighlightGroup extends LogFile {
        public final String highlighGroupName;

        public LogFileWithHighlightGroup( File file, String highlighGroupName ) {
            super( file );
            this.highlighGroupName = highlighGroupName;
        }

        @Override
        public <T> T use( Function<SimpleLogFile, T> useFile,
                          Function<LogFileWithHighlightGroup, T> useFileWithGroup ) {
            return useFileWithGroup.apply( this );
        }
    }
}
