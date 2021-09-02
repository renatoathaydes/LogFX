package com.athaydes.logfx.ui;

import com.athaydes.logfx.binding.BindableValue;
import com.athaydes.logfx.data.LogLineColors;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.binding.NumberBinding;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.util.List;


/**
 * Node holding a single log line in a {@link LogView}.
 */
class LogLine extends Parent implements SelectionHandler.SelectableNode {

    private final Label stdLine;
    private final BindableValue<Font> fontValue;

    LogLine( BindableValue<Font> fontValue,
             NumberBinding widthProperty,
             Paint bkgColor, Paint fillColor ) {
        this.stdLine = new Label();
        this.fontValue = fontValue;

        stdLine.setBackground( FxUtils.simpleBackground( bkgColor ) );
        stdLine.setTextFill( fillColor );
        stdLine.fontProperty().bind( fontValue );
        stdLine.minWidthProperty().bind( widthProperty );
        stdLine.getStyleClass().add( "log-line" );

        setOnMouseClicked( event -> {
            if ( event.getClickCount() > 1 ) swapSelectableText();
        } );

        getChildren().add( stdLine );
    }

    @Override
    public String getText() {
        return stdLine.getText();
    }

    private void swapSelectableText() {
        var child = getChildren().remove( 0 );
        if ( child == stdLine ) {
            var textField = new TextField( getText() );
            textField.setFont( fontValue.getValue() );
            textField.setEditable( false );
            textField.minWidthProperty().bind( stdLine.minWidthProperty() );
            textField.focusedProperty().addListener( ( ignore ) -> {
                if ( !textField.isFocused() ) swapSelectableText();
            } );
            getChildren().add( textField );
            Platform.runLater( textField::requestFocus );
        } else {
            getChildren().add( stdLine );
        }
    }

    @MustCallOnJavaFXThread
    void setText( String text, LogLineColors colors ) {
        stdLine.setText( text );
        stdLine.setBackground( FxUtils.simpleBackground( colors.getBackground() ) );
        stdLine.setTextFill( colors.getFill() );
    }

    @MustCallOnJavaFXThread
    @Override
    public void setSelect( boolean select ) {
        if ( select ) {
            stdLine.getStyleClass().add( "selected" );
        } else {
            stdLine.getStyleClass().remove( "selected" );
        }
    }

    @MustCallOnJavaFXThread
    void animate( Color color ) {
        Animation animation = new BackgroundTransition( color );
        animation.play();
    }

    private class BackgroundTransition extends Transition {

        private final Color originalColor;
        private final Color targetColor;

        BackgroundTransition( Color targetColor ) {
            List<BackgroundFill> fills = stdLine.getBackground().getFills();
            if ( !fills.isEmpty() && fills.get( 0 ).getFill() instanceof Color ) {
                this.originalColor = ( Color ) fills.get( 0 ).getFill();
            } else {
                this.originalColor = targetColor.invert();
            }

            if ( targetColor.equals( originalColor ) ) {
                this.targetColor = targetColor.invert();
            } else {
                this.targetColor = targetColor;
            }

            setCycleDuration( Duration.millis( 650 ) );
            setInterpolator( Interpolator.EASE_OUT );
            setCycleCount( 6 );
            setAutoReverse( true );
            setOnFinished( event -> stdLine.setBackground( FxUtils.simpleBackground( originalColor ) ) );
        }

        @Override
        protected void interpolate( double frac ) {
            stdLine.setBackground( FxUtils.simpleBackground(
                    originalColor.interpolate( targetColor, frac ) ) );
        }
    }

}
