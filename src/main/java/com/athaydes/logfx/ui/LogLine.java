package com.athaydes.logfx.ui;

import com.athaydes.logfx.binding.BindableValue;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.beans.binding.NumberBinding;
import javafx.scene.control.Label;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.util.List;


/**
 * Node holding a single log line in a {@link LogView}.
 */
class LogLine extends Label implements SelectionHandler.SelectableNode {

    LogLine( BindableValue<Font> fontValue,
             NumberBinding widthProperty,
             Paint bkgColor, Paint fillColor ) {
        setBackground( FxUtils.simpleBackground( bkgColor ) );
        setTextFill( fillColor );
        fontProperty().bind( fontValue );
        minWidthProperty().bind( widthProperty );
        getStyleClass().add( "log-line" );
    }

    @MustCallOnJavaFXThread
    void setText( String text, Paint bkgColor, Paint fillColor ) {
        super.setText( text );
        setColors( bkgColor, fillColor );
    }

    @MustCallOnJavaFXThread
    private void setColors( Paint bkgColor, Paint fillColor ) {
        setBackground( FxUtils.simpleBackground( bkgColor ) );
        setTextFill( fillColor );
    }

    @MustCallOnJavaFXThread
    @Override
    public void setSelect( boolean select ) {
        if ( select ) {
            getStyleClass().add( "selected" );
        } else {
            getStyleClass().remove( "selected" );
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
            List<BackgroundFill> fills = getBackground().getFills();
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
            setOnFinished( event -> setBackground( FxUtils.simpleBackground( originalColor ) ) );
        }

        @Override
        protected void interpolate( double frac ) {
            setBackground( FxUtils.simpleBackground(
                    originalColor.interpolate( targetColor, frac ) ) );
        }
    }

}
