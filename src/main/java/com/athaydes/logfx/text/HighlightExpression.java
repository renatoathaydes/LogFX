package com.athaydes.logfx.text;

import javafx.scene.paint.Paint;

import java.util.regex.Pattern;

/**
 *
 */
public class HighlightExpression {

    private final Pattern expression;
    private final Paint bkgColor;
    private final Paint fillColor;

    public HighlightExpression( String expression, Paint bkgColor, Paint fillColor ) {
        this.expression = Pattern.compile( expression );
        this.bkgColor = bkgColor;
        this.fillColor = fillColor;
    }

    public Paint getBkgColor() {
        return bkgColor;
    }

    public Paint getFillColor() {
        return fillColor;
    }

    public boolean matches( String text ) {
        if ( text == null || text.equals( "" ) ) {
            text = ".*";
        }
        return expression.matcher( text ).matches();
    }

}
