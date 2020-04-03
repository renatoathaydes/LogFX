package com.athaydes.logfx.update;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Optional;

import static com.athaydes.logfx.Constants.LOGFX_VERSION;
import static com.athaydes.logfx.update.Http.connect;

public final class LogFXUpdater {
    private static final Logger log = LoggerFactory.getLogger( LogFXUpdater.class );

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
                        log.info( "LogFX version is not the latest" );
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

    private static void update( String newVersion ) {
        // TODO update LogFX
        log.info( "Downloading new LogFX version: {}", newVersion );
    }

    public static void main( String[] args ) {
        try {
            requiresUpdate().ifPresent( LogFXUpdater::update );
        } catch ( Exception e ) {
            log.warn( "Failure trying to check for updates", e );
        }
    }
}
