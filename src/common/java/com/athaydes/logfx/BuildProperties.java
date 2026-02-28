package com.athaydes.logfx;

import java.io.IOException;
import java.util.Properties;

public final class BuildProperties {

    private static final Properties properties = new Properties();

    static {
        try ( var in = BuildProperties.class.getResourceAsStream( "/build-properties.yaml" ) ) {
            if ( in != null ) {
                properties.load( in );
            }
        } catch ( IOException e ) {
            System.err.println( "Failed to load build properties: " + e );
        }
    }

    public static String version() {
        return stripQuotes( properties.getProperty( "logfx", "unknown" ) );
    }

    private static String stripQuotes( String value ) {
        if ( value != null && value.length() >= 2
                && value.startsWith( "\"" ) && value.endsWith( "\"" ) ) {
            return value.substring( 1, value.length() - 1 );
        }
        return value;
    }
}
