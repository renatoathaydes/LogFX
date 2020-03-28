package com.athaydes.logfx.ui;

import com.athaydes.logfx.LogFX;
import javafx.application.HostServices;
import javafx.scene.control.Hyperlink;

public class Link extends Hyperlink {
    private final HostServices hostServices;
    private String url;

    public Link() {
        this.hostServices = LogFX.hostServices();
        setOnAction( ( event ) -> hostServices.showDocument( url ) );
    }

    public String getUrl() {
        return url;
    }

    public void setUrl( String url ) {
        this.url = url;
    }
}
