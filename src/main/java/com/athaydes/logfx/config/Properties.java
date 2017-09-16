package com.athaydes.logfx.config;

import com.athaydes.logfx.log.LogLevel;
import com.athaydes.logfx.log.LogTarget;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

/**
 * LogFX System Properties.
 */
public class Properties {

    public static final Path LOGFX_DIR;

    private static volatile LogLevel logLevel = null;
    private static volatile LogTarget logTarget = null;
    private static final boolean refreshStylesheet;
    private static final String customStylesheet;

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
                System.err.println( "Invalid value for 'logfx.log.level' system property: " + logLevelValue );
                System.err.println( "Valid values for 'logfx.log.level' are: TRACE, DEBUG, INFO (default), WARN, ERROR" );
            }
        }

        String logTargetValue = System.getProperty( "logfx.log.target" );

        if ( logTargetValue != null && !logTargetValue.trim().isEmpty() ) {
            switch ( logTargetValue.trim().toUpperCase() ) {
                case "SYSOUT":
                    logTarget = new LogTarget.PrintStreamLogTarget( System.out );
                    break;
                case "SYSERR":
                    logTarget = new LogTarget.PrintStreamLogTarget( System.err );
                    break;
                case "FILE":
                    logTarget = new LogTarget.FileLogTarget();
                    break;
                default:
                    System.err.println( "Invalid value for 'logfx.log.target' system property: " + logTargetValue );
                    System.err.println( "Valid values for 'logfx.log.target' are: SYSOUT, SYSERR, FILE (default)" );
            }
        }

        customStylesheet = System.getProperty( "logfx.stylesheet.file" );
        refreshStylesheet = System.getProperty( "logfx.stylesheet.norefresh" ) == null;
    }

    public static Optional<LogLevel> getLogLevel() {
        return Optional.ofNullable( logLevel );
    }

    public static Optional<LogTarget> getLogTarget() {
        return Optional.ofNullable( logTarget );
    }

    public static Optional<File> getCustomStylesheet() {
        return Optional.ofNullable( customStylesheet ).map( File::new );
    }

    public static boolean isRefreshStylesheet() {
        return refreshStylesheet;
    }
}
