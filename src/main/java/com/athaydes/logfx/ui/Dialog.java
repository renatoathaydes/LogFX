package com.athaydes.logfx.ui;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.MotionBlur;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dialog window to display other nodes vertically.
 */
public class Dialog {

    private static final Logger log = LoggerFactory.getLogger( Dialog.class );

    public enum MessageLevel {
        INFO, WARNING, ERROR
    }

    private static Window primaryStage = null;

    public static void setPrimaryStage( Window primaryStage ) {
        Dialog.primaryStage = primaryStage;
    }

    private final Stage dialogStage = new Stage();
    private final VBox box = new VBox( 10 );
    private boolean hasBeenShown = false;
    private boolean closeWhenLoseFocus = false;

    public Dialog( Node top, Node... others ) {
        this( null, top, others );
    }

    public Dialog( String stylesheet, Node top, Node... others ) {
        dialogStage.initOwner( primaryStage );
        dialogStage.initModality( Modality.NONE );
        box.setAlignment( Pos.CENTER );
        box.setPadding( new Insets( 20 ) );
        box.getChildren().add( top );
        box.getChildren().addAll( others );
        dialogStage.setScene( new Scene( box ) );
        dialogStage.getScene().setFill( Color.TRANSPARENT );

        if ( stylesheet == null ) {
            FxUtils.setupStylesheet( dialogStage.getScene() );
        } else {
            dialogStage.getScene().getStylesheets().add( stylesheet );
        }

        dialogStage.focusedProperty().addListener( observable -> {
            if ( dialogStage.isFocused() ) {
                dialogStage.setOpacity( 1.0 );
            } else if ( closeWhenLoseFocus ) {
                dialogStage.close();
            } else {
                dialogStage.setOpacity( 0.5 );
            }
        } );
    }

    public VBox getBox() {
        return box;
    }

    public void setCloseWhenLoseFocus( boolean closeWhenLoseFocus ) {
        this.closeWhenLoseFocus = closeWhenLoseFocus;
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

    public void setSize( double width, double height ) {
        dialogStage.setWidth( width );
        dialogStage.setHeight( height );
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

    static void showMessage( String text, MessageLevel level ) {
        Platform.runLater( () -> {
            Text messageText = new Text( text );
            Dialog dialog = new Dialog( messageText );
            dialog.setStyle( StageStyle.TRANSPARENT );
            dialog.getBox().setSpacing( 0 );
            dialog.getBox().getStyleClass().addAll( "message", level.name().toLowerCase() );

            MotionBlur blurText = new MotionBlur();
            blurText.setAngle( 0 );
            blurText.setRadius( 0 );
            messageText.setEffect( blurText );

            Animation blurAnimation = new Timeline( new KeyFrame( Duration.millis( 550 ),
                    new KeyValue( blurText.angleProperty(), 45.0 ),
                    new KeyValue( blurText.radiusProperty(), 20.0 ) ) );
            blurAnimation.setDelay( Duration.millis( 500 ) );

            FadeTransition hideAnimation = new FadeTransition( Duration.seconds( 2 ), dialog.getBox() );
            hideAnimation.setFromValue( 1.0 );
            hideAnimation.setToValue( 0.1 );

            ParallelTransition allAnimations = new ParallelTransition( hideAnimation, blurAnimation );
            allAnimations.setDelay( Duration.seconds( 3 ) );
            allAnimations.setOnFinished( event -> dialog.hide() );

            dialog.getBox().setOnMouseClicked( event -> {
                allAnimations.setDelay( Duration.seconds( 0 ) );
                allAnimations.stop();
                allAnimations.play();
            } );

            dialog.show();
            allAnimations.play();
        } );
    }

}
