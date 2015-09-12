package com.athaydes.logfx.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 *
 */
public class Dialog {

    private final Stage dialogStage = new Stage();

    public Dialog( Node top, Node... others ) {
        dialogStage.initModality( Modality.NONE );
        VBox box = new VBox( 10 );
        box.setAlignment( Pos.CENTER );
        box.setPadding( new Insets( 20 ) );
        box.getChildren().add( top );
        box.getChildren().addAll( others );
        dialogStage.setScene( new Scene( box ) );
    }

    public void setTitle( String title ) {
        dialogStage.setTitle( title );
    }

    public void setAlwaysOnTop( boolean alwaysOnTop ) {
        dialogStage.setAlwaysOnTop( alwaysOnTop );
    }

    public void show() {
        dialogStage.centerOnScreen();
        dialogStage.show();
    }

    public void hide() {
        dialogStage.hide();
    }

    public static void showConfirmDialog( String text ) {
        Button okButton = new Button( "OK" );
        Dialog dialog = new Dialog(
                new Text( text ),
                okButton );
        okButton.setOnAction( event -> dialog.hide() );
        dialog.show();
    }

}
