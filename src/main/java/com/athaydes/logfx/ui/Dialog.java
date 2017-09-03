package com.athaydes.logfx.ui;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A dialog window to display other nodes vertically.
 */
public class Dialog {

    private static final Logger log = LoggerFactory.getLogger( Dialog.class );

    private static Window primaryStage = null;

    public static void setPrimaryStage( Window primaryStage ) {
        Dialog.primaryStage = primaryStage;
    }

    private final Stage dialogStage = new Stage();
    private boolean hasBeenShown = false;

    public Dialog( Node top, Node... others ) {
        dialogStage.initOwner( primaryStage );
        dialogStage.initModality( Modality.NONE );
        VBox box = new VBox( 10 );
        box.setAlignment( Pos.CENTER );
        box.setPadding( new Insets( 20 ) );
        box.getChildren().add( top );
        box.getChildren().addAll( others );
        dialogStage.setScene( new Scene( box ) );
        dialogStage.getScene().getStylesheets().add( "css/LogFX.css" );
        dialogStage.focusedProperty().addListener( observable -> {
            if ( dialogStage.isFocused() ) {
                dialogStage.setOpacity( 1.0 );
            } else {
                dialogStage.setOpacity( 0.5 );
            }
        } );
    }

    public void setResizable( boolean resizable ) {
        dialogStage.setResizable( resizable );
    }

    public void setTitle( String title ) {
        dialogStage.setTitle( title );
    }

    public void setOnHidden( EventHandler<WindowEvent> handler ) {
        dialogStage.setOnHidden( handler );
    }

    public void setAlwaysOnTop( boolean alwaysOnTop ) {
        dialogStage.setAlwaysOnTop( alwaysOnTop );
    }

    public void show() {
        dialogStage.centerOnScreen();
        dialogStage.show();
        hasBeenShown = true;
    }

    public void hide() {
        dialogStage.hide();
    }

    public boolean isVisible() {
        return dialogStage.isShowing();
    }

    public void setOwner( Window owner ) {
        if ( !hasBeenShown ) {
            dialogStage.initOwner( owner );
        } else {
            log.debug( "Ignoring new owner as dialog has already been shown and cannot accept new owner" );
        }
    }

    public void setStyle( StageStyle style ) {
        if ( !hasBeenShown ) {
            dialogStage.initStyle( style );
        } else {
            log.debug( "Ignoring new StageStyle as dialog has already been shown and cannot change" );
        }
    }

    public static void showConfirmDialog( String text ) {
        Platform.runLater( () -> {
            Button okButton = new Button( "OK" );
            Dialog dialog = new Dialog(
                    new Text( text ),
                    okButton );
            okButton.setOnAction( event -> dialog.hide() );
            dialog.show();
        } );
    }

}
