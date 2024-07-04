package com.athaydes.logfx.ui;

import com.athaydes.logfx.binding.BindableValue;
import com.athaydes.logfx.data.LogLineColors;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.binding.NumberBinding;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

import java.time.Duration;
import java.util.List;


/**
 * Node holding a single log line in a {@link LogView}.
 */
class LogLine extends VBox implements SelectionHandler.SelectableNode {

    private static final int MAX_LINE_LENGTH = 5000;

    private final int lineIndex;
    private String fullText = "";
    private boolean displayTimeGap;
    private final Label stdLine;
    private final Label timeGap;
    private final BindableValue<Font> fontValue;

    LogLine( BindableValue<Font> fontValue,
             int lineIndex,
             NumberBinding widthProperty,
             Paint bkgColor, Paint fillColor ) {
        this.stdLine = new Label();
        this.fontValue = fontValue;
        this.lineIndex = lineIndex;

        this.timeGap = new Label();
        timeGap.getStyleClass().add( "log-line-time-gap" );
        timeGap.minWidthProperty().bind( widthProperty );

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
    public Node getNode() {
        return this;
    }

    @Override
    public int getLineIndex() {
        return lineIndex;
    }

    @Override
    public String getText() {
        return fullText;
    }

    private void swapSelectableText() {
        // the children may be either:
        //   * a timeGap Label AND the stdLine Label
        //   * OR a TextField (editable line)
        var child = getChildren().get( 0 );
        getChildren().clear();
        if ( child == stdLine || child == timeGap ) {
            var textField = new TextField( fullText );
            textField.setFont( fontValue.getValue() );
            textField.minWidthProperty().bind( stdLine.minWidthProperty() );
            textField.focusedProperty().addListener( ( ignore ) -> {
                if ( !textField.isFocused() ) swapSelectableText();
            } );
            getChildren().add( textField );
            Platform.runLater( textField::requestFocus );
        } else {
            if ( displayTimeGap ) {
                getChildren().add( timeGap );
            }
            getChildren().add( stdLine );
        }
    }

    /**
     * Set the text value.
     *
     * @param text    text value
     * @param colors  colors of this line
     * @param timeGap time gap duration, or null to not display a time gap
     */
    @MustCallOnJavaFXThread
    void setText( String text, LogLineColors colors, Duration timeGap ) {
        this.fullText = text;
        var uiText = fullText.length() > MAX_LINE_LENGTH
                ? fullText.substring( 0, MAX_LINE_LENGTH ) + "..."
                : fullText;
        stdLine.setText( uiText );
        stdLine.setBackground( FxUtils.simpleBackground( colors.getBackground() ) );
        stdLine.setTextFill( colors.getFill() );

        var displayTimeGap = timeGap != null;
        if ( displayTimeGap != this.displayTimeGap ) {
            this.displayTimeGap = displayTimeGap;
            if ( displayTimeGap ) {
                stdLine.getStyleClass().add( "with-time-gap" );
                this.timeGap.setText( "Time gap: " + timeGap.toMillis() + "ms" );
                getChildren().add( 0, this.timeGap );
            } else {
                assert ( getChildren().size() == 2 );
                stdLine.getStyleClass().remove( "with-time-gap" );
                getChildren().remove( 0 );
            }
        }
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

            setCycleDuration( javafx.util.Duration.millis( 650 ) );
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
