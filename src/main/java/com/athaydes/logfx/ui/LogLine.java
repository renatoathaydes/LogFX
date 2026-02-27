package com.athaydes.logfx.ui;

import com.athaydes.logfx.binding.BindableValue;
import com.athaydes.logfx.data.LogLineColors;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.binding.NumberBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.time.Duration;

/**
 * Node holding a single log line in a {@link LogView}.
 */
class LogLine extends VBox implements SelectionHandler.SelectableNode {

    private static final int MAX_LINE_LENGTH = 5000;

    private static final Color SELECTION_BG = Color.web( "#039ED3" );
    private static final Color SELECTION_TEXT = SELECTION_BG.deriveColor( 0, 1, 0.2, 1 );

    private final int lineIndex;
    private String fullText = "";
    private boolean displayTimeGap;
    private final StackPane lineContainer;
    private final Text textNode;
    private final Label timeGap;
    private final BindableValue<Font> fontValue;
    private LogLineColors currentColors;
    private boolean isSelected;

    LogLine( BindableValue<Font> fontValue,
             int lineIndex,
             NumberBinding widthProperty,
             Paint bkgColor, Paint fillColor ) {
        this.fontValue = fontValue;
        this.lineIndex = lineIndex;
        this.currentColors = new LogLineColors( bkgColor, fillColor );

        this.textNode = new Text();
        this.lineContainer = new StackPane( textNode );
        lineContainer.setAlignment( Pos.CENTER_LEFT );
        lineContainer.setPadding( new Insets( 0, 5, 0, 5 ) );

        this.timeGap = new Label();
        timeGap.getStyleClass().add( "log-line-time-gap" );
        timeGap.minWidthProperty().bind( widthProperty );

        lineContainer.setBackground( FxUtils.simpleBackground( bkgColor ) );
        if ( fillColor instanceof Color color ) {
            textNode.setFill( color );
        } else {
            textNode.setFill( Color.WHITE );
        }
        textNode.fontProperty().bind( fontValue );
        lineContainer.minWidthProperty().bind( widthProperty );

        setOnMouseClicked( event -> {
            if ( event.getClickCount() > 1 ) swapSelectableText();
        } );

        getChildren().add( lineContainer );
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
        //   * a timeGap Label AND the lineContainer StackPane
        //   * OR a TextField (editable line)
        var child = getChildren().get( 0 );
        if ( child == lineContainer || child == timeGap ) {
            setLineEditingChildren();
        } else {
            setDefaultChildren();
        }
    }

    @MustCallOnJavaFXThread
    private void setDefaultChildren() {
        getChildren().clear();
        if ( displayTimeGap ) {
            getChildren().add( timeGap );
        }
        getChildren().add( lineContainer );
    }

    @MustCallOnJavaFXThread
    private void setLineEditingChildren() {
        getChildren().clear();
        var textField = new TextField( fullText );
        textField.setFont( fontValue.getValue() );
        textField.minWidthProperty().bind( lineContainer.minWidthProperty() );
        textField.focusedProperty().addListener( ( ignore ) -> {
            if ( !textField.isFocused() ) swapSelectableText();
        } );
        textField.setOnKeyPressed( event -> {
            if ( event.getCode() == KeyCode.ESCAPE ) {
                // focus on something else in order to trigger the listener above
                requestFocus();
            }
        } );
        getChildren().add( textField );
        Platform.runLater( textField::requestFocus );
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
        this.currentColors = colors;
        var uiText = fullText.length() > MAX_LINE_LENGTH
                ? fullText.substring( 0, MAX_LINE_LENGTH ) + "..."
                : fullText;
        textNode.setText( uiText );

        if ( !isSelected ) {
            applyHighlightColors();
        }

        var displayTimeGap = timeGap != null;
        if ( displayTimeGap != this.displayTimeGap ) {
            this.displayTimeGap = displayTimeGap;
            if ( displayTimeGap ) {
                this.timeGap.setText( "Time gap: " + timeGap.toMillis() + "ms" );
                getChildren().add( 0, this.timeGap );
            } else {
                assert ( getChildren().size() == 2 );
                getChildren().remove( 0 );
            }
        }
    }

    @MustCallOnJavaFXThread
    @Override
    public void setSelect( boolean select ) {
        this.isSelected = select;
        if ( select ) {
            applySelectionColors();
        } else {
            applyHighlightColors();
        }
    }

    private void applyHighlightColors() {
        lineContainer.setBackground( FxUtils.simpleBackground( currentColors.getBackground() ) );
        Paint fill = currentColors.getFill();
        if ( fill instanceof Color color ) {
            textNode.setFill( color );
        }
    }

    private void applySelectionColors() {
        lineContainer.setBackground( FxUtils.simpleBackground( SELECTION_BG ) );
        textNode.setFill( SELECTION_TEXT );
    }

    @MustCallOnJavaFXThread
    void animate( Color color ) {
        Animation animation = new BackgroundTransition( color );
        animation.play();
    }

    private class BackgroundTransition extends Transition {
        private final Color originalBkgColor;
        private final Color originalTextColor;
        private final Color targetColor;

        BackgroundTransition( Color targetColor ) {
            var fills = lineContainer.getBackground().getFills();
            if ( !fills.isEmpty() && fills.get( 0 ).getFill() instanceof Color c ) {
                this.originalBkgColor = c;
            } else {
                this.originalBkgColor = Color.WHITE;
            }
            this.originalTextColor = textNode.getFill() instanceof Color c ? c : Color.WHITE;

            if ( targetColor.equals( originalBkgColor ) ) {
                this.targetColor = targetColor.invert();
            } else {
                this.targetColor = targetColor;
            }

            setCycleDuration( javafx.util.Duration.millis( 650 ) );
            setInterpolator( Interpolator.EASE_OUT );
            setCycleCount( 6 );
            setAutoReverse( true );
            setOnFinished( event -> {
                lineContainer.setBackground( FxUtils.simpleBackground( originalBkgColor ) );
                textNode.setFill( originalTextColor );
            } );
        }

        @Override
        protected void interpolate( double frac ) {
            Color interpolatedColor = originalBkgColor.interpolate( targetColor, frac );
            lineContainer.setBackground( FxUtils.simpleBackground( interpolatedColor ) );
        }
    }

}
