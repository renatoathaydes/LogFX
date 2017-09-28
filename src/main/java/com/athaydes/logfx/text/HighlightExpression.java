package com.athaydes.logfx.text;

import com.athaydes.logfx.data.LogLineColors;
import javafx.scene.paint.Paint;

import java.util.regex.Pattern;

/**
 * A log line highlight expression.
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

    public Pattern getPattern() {
        return expression;
    }

    public LogLineColors getLogLineColors() {
        return new LogLineColors( bkgColor, fillColor );
    }

    public boolean matches( String text ) {
        if ( text == null ) {
            text = "";
        }

        // the find method does not anchor the String by default, unlike matches()
        return expression.matcher( text ).find();
    }

    @Override
    public String toString() {
        return "{" +
                "expression=" + expression +
                ", bkgColor=" + bkgColor +
                ", fillColor=" + fillColor +
                '}';
    }
}
