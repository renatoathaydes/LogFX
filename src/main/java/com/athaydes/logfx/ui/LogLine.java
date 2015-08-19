package com.athaydes.logfx.ui;

import com.athaydes.logfx.binding.BindableValue;
import javafx.beans.binding.NumberBinding;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;


/**
 *
 */
public class LogLine extends Label {

    private static Insets padding = new Insets( 2, 5, 2, 5 );

    private static LogLine selected = null;

    private static final EventHandler<MouseEvent> clickHandler = new EventHandler<MouseEvent>() {
        final Paint selectedBkgColor = Color.DARKBLUE;
        final Paint selectedFillColor = Color.WHITE;

        @Override
        public void handle( MouseEvent event ) {
            boolean unselect = event.getSource() == selected;
            if ( selected != null ) {
                selected.setColors( selected.bkgColor, selected.fillColor );
            }
            if ( unselect ) {
                selected = null;
            } else {
                LogLine newSelection = ( LogLine ) event.getSource();
                selected = newSelection;
                newSelection.setColors( selectedBkgColor, selectedFillColor );
            }
        }
    };

    private Paint bkgColor;
    private Paint fillColor;

    public LogLine( BindableValue<Font> fontValue,
                    NumberBinding widthProperty,
                    Paint bkgColor, Paint fillColor ) {
        setBackground( FxUtils.simpleBackground( bkgColor ) );
        setTextFill( fillColor );
        fontProperty().bind( fontValue );
        minWidthProperty().bind( widthProperty );
        setPadding( padding );
        setOnMouseReleased( clickHandler );
    }

    public void setText( String text, Paint bkgColor, Paint fillColor ) {
        super.setText( text );
        this.bkgColor = bkgColor;
        this.fillColor = fillColor;
        if ( this != selected ) {
            setColors( bkgColor, fillColor );
        }
    }

    private void setColors( Paint bkgColor, Paint fillColor ) {
        setBackground( FxUtils.simpleBackground( bkgColor ) );
        setTextFill( fillColor );
    }

}
