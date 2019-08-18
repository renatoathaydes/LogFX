package com.athaydes.logfx.ui;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.MotionBlur;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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

import java.util.LinkedList;
import java.util.List;

/**
 * A dialog window to display other nodes vertically.
 */
public class Dialog {

    private static final Logger log = LoggerFactory.getLogger( Dialog.class );

    public enum MessageLevel {
        INFO, WARNING, ERROR;

        int getDelay() {
            switch ( this ) {
                case INFO:
                default:
                    return 3;
                case WARNING:
                    return 5;
                case ERROR:
                    return 15;
            }
        }
    }

    public enum DialogPosition {
        CENTER, TOP_CENTER
    }

    private static Window primaryStage = null;
    private static final List<Stage> currentMessageStages = new LinkedList<>();

    public static void setPrimaryStage( Window primaryStage ) {
        Dialog.primaryStage = primaryStage;
    }

    private final Stage dialogStage = new Stage();
    private final VBox box = new VBox( 10 );
    private boolean hasBeenShown = false;
    private boolean closeWhenLoseFocus = false;
    private boolean transparentWhenNoFocus = false;

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
        dialogStage.addEventHandler( KeyEvent.KEY_RELEASED, ( KeyEvent event ) -> {
            if ( KeyCode.ESCAPE == event.getCode() ) {
                dialogStage.close();
            }
        } );

        if ( stylesheet == null ) {
            FxUtils.setupStylesheet( dialogStage.getScene() );
        } else {
            dialogStage.getScene().getStylesheets().add( stylesheet );
        }
    }

    public VBox getBox() {
        return box;
    }

    public void closeWhenLoseFocus() {
        if ( !closeWhenLoseFocus ) {
            dialogStage.focusedProperty().addListener( observable -> {
                if ( !dialogStage.isFocused() ) {
                    dialogStage.close();
                }
            } );
            closeWhenLoseFocus = true;
        }
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

    public void makeTransparentWhenLoseFocus() {
        if ( !transparentWhenNoFocus ) {
            dialogStage.focusedProperty().addListener( observable -> {
                if ( dialogStage.isFocused() ) {
                    dialogStage.setOpacity( 1.0 );
                } else {
                    dialogStage.setOpacity( 0.5 );
                }
            } );
            transparentWhenNoFocus = true;
        }
    }

    public void show() {
        show( DialogPosition.CENTER );
    }

    public void show( DialogPosition position ) {
        switch ( position ) {
            case CENTER:
                dialogStage.centerOnScreen();
                break;
            case TOP_CENTER:
                Window owner = dialogStage.getOwner();
                dialogStage.setX( owner.getX() + ( owner.getWidth() * 0.25 ) );
                dialogStage.setY( owner.getY() + 30 );
                break;
        }

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

    public static void showMessage( String text, MessageLevel level ) {
        Platform.runLater( () -> {
            Text messageText = new Text( text );
            messageText.setWrappingWidth( Math.max( 100, primaryStage.getWidth() - 20 ) );

            Dialog dialog = new Dialog( messageText );
            dialog.setStyle( StageStyle.TRANSPARENT );
            dialog.getBox().setSpacing( 0 );
            dialog.getBox().getStyleClass().addAll( "message", level.name().toLowerCase() );

            MotionBlur blurText = new MotionBlur();
            blurText.setAngle( 0 );
            blurText.setRadius( 0 );
            messageText.setEffect( blurText );

            Animation blurAnimation = new Timeline( new KeyFrame( Duration.millis( 450 ),
                    new KeyValue( blurText.angleProperty(), 45.0 ),
                    new KeyValue( blurText.radiusProperty(), 20.0 ) ) );
            blurAnimation.setDelay( Duration.millis( 500 ) );

            FadeTransition hideAnimation = new FadeTransition( Duration.seconds( 1 ), dialog.getBox() );
            hideAnimation.setFromValue( 1.0 );
            hideAnimation.setToValue( 0.1 );
            hideAnimation.setInterpolator( Interpolator.EASE_IN );

            ParallelTransition allAnimations = new ParallelTransition( hideAnimation, blurAnimation );
            allAnimations.setDelay( Duration.seconds( level.getDelay() ) );
            allAnimations.setOnFinished( event -> {
                dialog.hide();
                currentMessageStages.remove( dialog.dialogStage );
            } );

            dialog.getBox().setOnMouseClicked( event -> {
                allAnimations.setDelay( Duration.seconds( 0 ) );
                allAnimations.stop();
                allAnimations.play();
            } );

            dialog.show( DialogPosition.TOP_CENTER );
            allAnimations.play();

            double yShift = 20 + currentMessageStages.stream()
                    .mapToDouble( stage -> stage.getHeight() + 5.0 )
                    .sum();

            StageTranslateTransition shiftDown = new StageTranslateTransition( dialog.dialogStage );
            shiftDown.setToY( yShift );
            shiftDown.setDuration( Duration.millis( 250.0 ) );
            shiftDown.setInterpolator( Interpolator.EASE_BOTH );
            shiftDown.play();

            currentMessageStages.add( dialog.dialogStage );
        } );
    }

    private static class StageTranslateTransition extends Transition {

        private final Stage stage;
        private final double originalY;
        private double toY = 100.0;

        StageTranslateTransition( Stage stage ) {
            this.stage = stage;
            this.originalY = stage.getY();
        }

        void setToY( double toY ) {
            this.toY = toY;
        }

        void setDuration( Duration duration ) {
            setCycleDuration( duration );
        }

        @Override
        protected void interpolate( double frac ) {
            stage.setY( originalY + toY * frac );
        }
    }

}
