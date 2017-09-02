package com.athaydes.logfx.ui;

import com.athaydes.logfx.binding.BindableValue;
import javafx.beans.binding.NumberBinding;
import javafx.scene.control.Label;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;


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

    void setText( String text, Paint bkgColor, Paint fillColor ) {
        super.setText( text );
        setColors( bkgColor, fillColor );
    }

    private void setColors( Paint bkgColor, Paint fillColor ) {
        setBackground( FxUtils.simpleBackground( bkgColor ) );
        setTextFill( fillColor );
    }

    @Override
    public void setSelect( boolean select ) {
        if ( select ) {
            getStyleClass().add( "selected" );
        } else {
            getStyleClass().remove( "selected" );
        }
    }

}
