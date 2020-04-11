package com.athaydes.logfx.update;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

final class Http {
    static HttpURLConnection connect( URL url, String method, boolean followRedirects )
            throws IOException {
        var con = ( HttpURLConnection ) url.openConnection();
        con.setRequestMethod( method );
        con.setConnectTimeout( 5000 );
        con.setReadTimeout( 5000 );
        con.setInstanceFollowRedirects( followRedirects );
        return con;
    }

}
