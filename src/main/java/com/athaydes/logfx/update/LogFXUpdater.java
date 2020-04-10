package com.athaydes.logfx.update;

import com.athaydes.logfx.concurrency.TaskRunner;
import com.athaydes.logfx.config.Properties;
import com.athaydes.logfx.ui.Dialog;
import com.athaydes.logfx.ui.FxUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.Optional;
import java.util.Set;

import static com.athaydes.logfx.Constants.LOGFX_VERSION;
import static com.athaydes.logfx.update.Http.connect;

public final class LogFXUpdater {
    private static final Logger log = LoggerFactory.getLogger( LogFXUpdater.class );
    private static final long UPDATE_CHECK_PERIOD_MS = 24 * 60 * 60 * 1000;
    private static final String LOGFX_UPDATE_CHECK = "logfx-update-check";
    static final String LOGFX_UPDATE_ZIP = "logfx-update.zip";

    /**
     * @return a new version if requires update, empty otherwise
     * @throws IOException on problems
     */
    private static Optional<String> requiresUpdate() throws IOException {
        var url = new URL( "https://bintray.com/renatoathaydes/maven/logfx/_latestVersion" );
        var con = connect( url, "HEAD", false );
        try {
            int status = con.getResponseCode();

            if ( 300 <= status && status <= 310 ) {
                String location = con.getHeaderField( "Location" );
                if ( location != null ) {
                    String latestVersion = extractVersionFromLocation( location );
                    if ( LOGFX_VERSION.equalsIgnoreCase( latestVersion ) ) {
                        log.debug( "Current LogFX version matches latest version: {}", latestVersion );
                    } else {
                        log.info( "LogFX version is not the latest. current={}, new={}", LOGFX_VERSION, latestVersion );
                        return Optional.of( latestVersion );
                    }
                }
            } else {
                log.warn( "Unexpected status code (not a redirect): {}", status );
            }
        } finally {
            con.disconnect();
        }

        return Optional.empty();
    }

    private static String extractVersionFromLocation( String location ) {
        URI uri = URI.create( location );
        String path = uri.getPath();
        int idx = path.lastIndexOf( '/' );
        if ( idx > 0 && idx < path.length() - 1 ) {
            return path.substring( idx + 1 );
        } else {
            throw new IllegalStateException( "Unexpected path, does not contain a version: " + location );
        }
    }

    private static void downloadUpdate( File rootDir, String newVersion ) throws Exception {
        if ( looksLikeLogFxRootDir( rootDir ) ) {
            log.info( "Downloading new LogFX version: {}", newVersion );
            var url = String.format( "https://github.com/renatoathaydes/LogFX/releases/download/%s/logfx-%s-%s.zip",
                    newVersion, newVersion, FxUtils.getOs() );
            var con = Http.connect( new URL( url ), "GET", true );
            try {
                if ( con.getResponseCode() == 200 ) {
                    Path updatePath = Properties.LOGFX_DIR.resolve( LOGFX_UPDATE_ZIP );
                    Files.copy( con.getInputStream(), updatePath, StandardCopyOption.REPLACE_EXISTING );
                    log.info( "Successfully downloaded LogFX {} to {}", newVersion, updatePath );
                    Dialog.showMessage( String.format( "LogFX has been updated to version %s.\n" +
                            "Restart LogFX to enjoy the new version.", newVersion ), Dialog.MessageLevel.INFO );
                } else {
                    log.warn( "Unexpected status when trying to download LogFX: {}", con.getResponseCode() );
                }
            } finally {
                con.disconnect();
            }
        } else {
            log.warn( "The root directory does not look like LogFX home, aborting update" );
        }
    }

    private static boolean looksLikeLogFxRootDir( File rootDir ) {
        if ( rootDir.isDirectory() ) {
            var children = rootDir.list();
            if ( children != null && children.length >= 4 ) {
                if ( Set.of( children ).containsAll( Set.of( "bin", "conf", "legal", "lib" ) ) ) {
                    return new File( rootDir, "bin/logfx" ).isFile();
                }
            }
        }
        return false;
    }

    private static boolean shouldCheckForUpdates() {
        Path updatesPath = Properties.LOGFX_DIR.resolve( LOGFX_UPDATE_CHECK );
        File updatesFile = updatesPath.toFile();
        if ( updatesFile.exists() ) {
            long lastCheck = updatesFile.lastModified();
            long now = System.currentTimeMillis();
            log.debug( "Last update check at {}, currently {}, diff = {}", lastCheck, now, now - lastCheck );
            try {
                Files.setLastModifiedTime( updatesPath, FileTime.fromMillis( now ) );
            } catch ( IOException e ) {
                log.warn( "Could not update logfx-update-check file timestamp" );
            }
            return now - lastCheck > UPDATE_CHECK_PERIOD_MS;
        } else {
            log.info( "Creating logfx-update-check file. Will check for LogFX updates daily." );
            try {
                Files.createFile( updatesPath );
            } catch ( IOException e ) {
                log.warn( "Could not create logfx-update-check file", e );
            }
            return false;
        }
    }

    public static void checkAndDownloadUpdateIfAvailable( TaskRunner taskRunner ) {
        if ( shouldCheckForUpdates() ) {
            var rootDir = new File( System.getProperty( "java.home" ) );

            log.info( "Checking for newer LogFX version" );
            taskRunner.runAsync( () -> {
                try {
                    requiresUpdate().ifPresent( newVersion ->
                            taskRunner.runAsync( () -> {
                                try {
                                    downloadUpdate( rootDir, newVersion );
                                } catch ( Exception e ) {
                                    log.error( "Unable to download LogFX", e );
                                }
                            } ) );
                } catch ( Exception e ) {
                    log.warn( "Problem checking for updates", e );
                }
            } );
        } else {
            log.debug( "Will not check for newer LogFX versions" );
        }
    }
}
