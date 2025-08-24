package com.athaydes.logfx.ui;

import com.athaydes.logfx.LogFXHostServices;
import javafx.scene.control.Hyperlink;

public class Link extends Hyperlink {
    private final String url;

    public Link( String url ) {
        this( url, url );
    }

    public Link( String url, String text ) {
        super( text );
        this.url = url;
        setOnAction( ( event ) -> LogFXHostServices.get().showDocument( this.url ) );
    }

    public String getUrl() {
        return url;
    }
}
