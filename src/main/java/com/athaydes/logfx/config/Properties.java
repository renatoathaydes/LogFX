package com.athaydes.logfx.config;

import com.athaydes.logfx.log.LogLevel;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

/**
 * LogFX System Properties.
 */
public class Properties {

    public static final Path LOGFX_DIR;

    private static volatile LogLevel logLevel = null;

    static {
        String customHome = System.getProperty( "logfx.home" );

        File homeDir;
        if ( customHome == null ) {
            homeDir = new File( System.getProperty( "user.home" ), ".logfx" );
        } else {
            homeDir = new File( customHome );
        }

        if ( homeDir.isFile() ) {
            throw new IllegalStateException( "LogFX home directory is set to a file: " +
                    homeDir + ", use the 'logfx.home' system property to change LogFX's home." );
        }

        if ( !homeDir.exists() ) {
            boolean ok = homeDir.mkdirs();
            if ( !ok ) {
                throw new IllegalStateException( "Unable to create LogFX home directory at " +
                        homeDir + ", use the 'logfx.home' system property to change LogFX's home." );
            }
        }

        LOGFX_DIR = homeDir.toPath();

        String logLevelValue = System.getProperty( "logfx.log.level" );

        if ( logLevelValue != null && !logLevelValue.trim().isEmpty() ) {
            try {
                logLevel = LogLevel.valueOf( logLevelValue.trim().toUpperCase() );
            } catch ( IllegalArgumentException e ) {
                e.printStackTrace();
            }
        }
    }

    public static Optional<LogLevel> getLogLevel() {
        return Optional.ofNullable( logLevel );
    }

}
