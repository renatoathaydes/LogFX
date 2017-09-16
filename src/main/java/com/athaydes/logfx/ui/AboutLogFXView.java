package com.athaydes.logfx.ui;

import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.StageStyle;

/**
 * About LogFX View.
 */
public class AboutLogFXView {

    public void show() {
        BorderPane contents = new BorderPane();
        contents.setCenter( new Text( "LogFX" ) );
        contents.setBottom( new Text( "Created by Renato Athaydes, 2017" ) );

        Dialog dialog = new Dialog( contents );
        dialog.setStyle( StageStyle.UNDECORATED );
        dialog.setCloseWhenLoseFocus( true );

        dialog.show();
    }
}
