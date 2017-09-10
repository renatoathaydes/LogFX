package com.athaydes.logfx.log;

import com.athaydes.logfx.LogFX;
import com.athaydes.logfx.log.LogFXLogger.LogLevel;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.nio.file.Path;

public enum LogFXLogFactory implements ILoggerFactory {

    INSTANCE;

    private volatile LogLevel currentLevel = LogLevel.INFO;

    private final Path logFilePath = LogFX.LOGFX_DIR.resolve( "logfx.log" );

    @Override
    public Logger getLogger( String name ) {
        return new LogFXLogger( name );
    }

    boolean isLogLevelEnabled( LogLevel logLevel ) {
        return logLevel.ordinal() >= currentLevel.ordinal();
    }

    public void setLogLevel( LogLevel logLevel ) {
        currentLevel = logLevel;
    }

    public Path getLogFilePath() {
        return logFilePath;
    }
}
