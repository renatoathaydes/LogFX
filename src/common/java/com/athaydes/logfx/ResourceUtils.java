package com.athaydes.logfx;

import java.net.URL;

public final class ResourceUtils {

    public static URL resourceUrl( String name ) {
        return ResourceUtils.class.getResource( name );
    }

    public static String resourcePath( String name ) {
        return resourceUrl( name ).toExternalForm();
    }
}
