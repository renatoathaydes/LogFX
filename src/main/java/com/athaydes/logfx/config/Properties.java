package com.athaydes.logfx.config;

import com.athaydes.logfx.log.LogLevel;
import com.athaydes.logfx.log.LogTarget;
import com.athaydes.logfx.ui.Dialog;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.athaydes.logfx.iterable.IterableUtils.append;

/**
 * LogFX System Properties.
 */
public class Properties {

    public static final Path LOGFX_DIR;
    public static final Path DEFAULT_LOGFX_CONFIG;
    public static final long UPDATE_CHECK_PERIOD_SECONDS;
    public static final String DEFAULT_PROJECT_NAME = "Default";

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
        DEFAULT_LOGFX_CONFIG = LOGFX_DIR.resolve( "config" );

        String logLevelValue = System.getProperty( "logfx.log.level" );

        if ( logLevelValue != null && !logLevelValue.trim().isEmpty() ) {
            try {
                logLevel = LogLevel.valueOf( logLevelValue.trim().toUpperCase() );
            } catch ( IllegalArgumentException e ) {
                System.err.println( "Invalid value for 'logfx.log.level' system property: " + logLevelValue );
                System.err.println( "Valid values for 'logfx.log.level' are: TRACE, DEBUG, INFO (default), WARN, " +
                        "ERROR" );
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

        String autoUpdatePeriod = System.getProperty( "logfx.auto_update.check_period" );
        Long autoUpdatePeriodSecs = null;
        if ( autoUpdatePeriod != null ) {
            try {
                autoUpdatePeriodSecs = Long.parseLong( autoUpdatePeriod );
            } catch ( NumberFormatException e ) {
                System.err.printf( "Invalid value for system property logfx.auto_update.check_period: %s (%s)\n",
                        autoUpdatePeriod, e );
            }
        }
        UPDATE_CHECK_PERIOD_SECONDS = autoUpdatePeriodSecs == null ? 24 * 60 * 60 : autoUpdatePeriodSecs;
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

    public static Collection<String> listProjects() {
        var projectsPath = LOGFX_DIR.resolve( Paths.get( "projects" ) );
        Collection<String> projects = List.of();
        if ( projectsPath.toFile().isDirectory() ) {
            var files = projectsPath.toFile().listFiles();
            if ( files != null && files.length != 0 ) {
                projects = Stream.of( files )
                        .map( File::getName )
                        .sorted()
                        .collect( Collectors.toList() );
            }
        }

        return append( DEFAULT_PROJECT_NAME, projects );
    }

    public static Optional<Path> getProjectFile( String projectName ) {
        if ( DEFAULT_PROJECT_NAME.equals( projectName ) ) {
            return Optional.of( LOGFX_DIR.resolve( "config" ) );
        }
        var projectPath = LOGFX_DIR.resolve( Paths.get( "projects", projectName ) );
        File file = projectPath.toFile();
        if ( !file.getParentFile().isDirectory() ) {
            var ok = file.getParentFile().mkdirs();
            if ( !ok ) {
                Dialog.showMessage( "Unable to create projects directory at: " + file.getParentFile(),
                        Dialog.MessageLevel.ERROR );
                return Optional.empty();
            }
        }
        if ( !file.isFile() ) {
            boolean ok;
            try {
                ok = file.createNewFile();
            } catch ( IOException e ) {
                Dialog.showMessage( "Cannot create project with given name: " + e, Dialog.MessageLevel.ERROR );
                return Optional.empty();
            }
            if ( !ok ) {
                Dialog.showMessage( "Unable to create project with given name", Dialog.MessageLevel.ERROR );
                return Optional.empty();
            }
        }
        return Optional.of( projectPath );
    }
}
