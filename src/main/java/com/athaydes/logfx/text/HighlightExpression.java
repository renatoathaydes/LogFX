package com.athaydes.logfx.text;

import com.athaydes.logfx.data.LogLineColors;
import javafx.scene.paint.Paint;

import java.util.regex.Pattern;

/**
 * A log line highlight expression.
 * <p>
 * This class is immutable, so every time the user changes one of its properties
 * the UI components managing it need to create a new instance.
 */
public final class HighlightExpression {

    private final Pattern expression;
    private final Paint bkgColor;
    private final Paint fillColor;
    private final boolean isFiltered;

    public HighlightExpression( String expression, Paint bkgColor, Paint fillColor, boolean isFiltered ) {
        this.expression = Pattern.compile( expression );
        this.bkgColor = bkgColor;
        this.fillColor = fillColor;
        this.isFiltered=isFiltered;
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

    public boolean isFiltered() {
        return isFiltered;
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
                ", isFiltered=" + isFiltered +
                '}';
    }
}
