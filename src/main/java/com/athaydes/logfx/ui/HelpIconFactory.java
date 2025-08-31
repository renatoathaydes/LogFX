package com.athaydes.logfx.ui;

import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.StageStyle;

import java.util.function.Supplier;

import static com.athaydes.logfx.ui.AwesomeIcons.HELP;

public class HelpIconFactory {

    public static Node create( String title, Supplier<Scene> getScene, Node content ) {
        Node help = AwesomeIcons.createIcon( HELP );

        Dialog helpDialog = new Dialog( content );
        helpDialog.setTitle( title );
        helpDialog.setStyle( StageStyle.UTILITY );
        helpDialog.setResizable( false );

        help.setOnMouseClicked( event -> {
            helpDialog.setOwner( getScene.get().getWindow() );
            helpDialog.show();
        } );

        help.setOnMouseEntered( event -> getScene.get().setCursor( Cursor.HAND ) );
        help.setOnMouseExited( event -> getScene.get().setCursor( Cursor.DEFAULT ) );

        return help;
    }
}
