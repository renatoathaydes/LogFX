package com.athaydes.logfx.ui;

import com.athaydes.logfx.Constants;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.StageStyle;

/**
 * About LogFX View.
 */
public class AboutLogFXView {

    VBox createNode() {
        VBox contents = new VBox( 25 );
        contents.setPrefSize( 500, 300 );
        contents.setAlignment( Pos.CENTER );
        contents.getStylesheets().add( "css/about.css" );

        HBox textBox = new HBox( 0 );
        textBox.setPrefWidth( 500 );
        textBox.setAlignment( Pos.CENTER );
        Text logText = new Text( "Log" );
        logText.setId( "logfx-text-log" );
        Text fxText = new Text( "FX" );
        fxText.setId( "logfx-text-fx" );
        textBox.getChildren().addAll( logText, fxText );

        VBox smallText = new VBox( 10 );
        smallText.setPrefWidth( 500 );
        smallText.setAlignment( Pos.CENTER );
        Text version = new Text( "Version " + Constants.LOGFX_VERSION );
        Text byRenato = new Text( "Copyright Renato Athaydes, 2017-2020. All rights reserved." );
        Text license = new Text( "Licensed under the GPLv3 License." );
        Text awesomeFontsAttribution = new Text( "Icons provided by https://fontawesome.com" );
        Link link = new Link( "https://github.com/renatoathaydes/LogFX" );
        smallText.getChildren().addAll( version, byRenato, link, license, awesomeFontsAttribution );

        contents.getChildren().addAll( textBox, smallText );

        return contents;
    }

    @MustCallOnJavaFXThread
    public void show() {
        Dialog dialog = new Dialog( ( String ) null, createNode() );
        dialog.setStyle( StageStyle.UNDECORATED );
        dialog.setResizable( false );
        dialog.closeWhenLoseFocus();

        dialog.show();
    }
}
