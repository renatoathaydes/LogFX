package com.athaydes.logfx.ui;

import com.athaydes.logfx.binding.BindableValue;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.control.Label;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;


/**
 *
 */
public class LogLine extends Label {

    public LogLine( BindableValue<Font> fontValue,
                    ReadOnlyDoubleProperty widthProperty,
                    Paint bkgColor, Paint fillColor ) {
        setBackground( FxUtils.simpleBackground( bkgColor ) );
        setTextFill( fillColor );
        fontProperty().bind( fontValue );
        minWidthProperty().bind( widthProperty );
    }

    public void setText( String text, Paint bkgColor, Paint fillColor ) {
        super.setText( text );
        setBackground( FxUtils.simpleBackground( bkgColor ) );
        setTextFill( fillColor );
    }


}
