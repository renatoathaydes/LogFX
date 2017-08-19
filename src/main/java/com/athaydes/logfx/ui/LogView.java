package com.athaydes.logfx.ui;

import com.athaydes.logfx.binding.BindableValue;
import com.athaydes.logfx.text.HighlightExpression;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class LogView extends VBox {

    private static final AtomicInteger maxLines = new AtomicInteger( 100 );
    private final HighlightOptions highlightOptions;

    public LogView( BindableValue<Font> fontValue,
                    ReadOnlyDoubleProperty widthProperty,
                    HighlightOptions highlightOptions ) {
        this.highlightOptions = highlightOptions;
        updateCapacity( fontValue, Bindings.max( widthProperty(), widthProperty ) );
        highlightOptions.getObservableExpressions().addListener( ( Observable observable ) -> {
            synchronized ( maxLines ) {
                for ( int i = 0; i < maxLines.get(); i++ ) {
                    updateLine( i );
                }
            }
        } );
    }

    private void updateCapacity( BindableValue<Font> fontValue, NumberBinding widthProperty ) {
        final HighlightExpression expression = highlightOptions.expressionFor( "" );
        for ( int i = 0; i < maxLines.get(); i++ ) {
            getChildren().add( new LogLine( fontValue, widthProperty,
                    expression.getBkgColor(), expression.getFillColor() ) );
        }
    }

    public void showLines( List<String> lines ) {
        final int max = maxLines.get();
        final int extraLines = lines.size() - max;

        final List<String> linesCopy = lines.subList( Math.max( 0, extraLines ), lines.size() );
        Platform.runLater( () -> {
            int i = 0;
            for ( String line : linesCopy ) {
                updateLine( lineAt( i++ ), line );
            }
            for ( ; i < max; i++ ) {
                updateLine( lineAt( i ), "" );
            }
        } );
    }

    private void updateLine( int index ) {
        LogLine line = lineAt( index );
        updateLine( line, line.getText() );// update only styles
    }

    private void updateLine( LogLine line, String text ) {
        HighlightExpression expression = highlightOptions.expressionFor( text );
        line.setText( text, expression.getBkgColor(), expression.getFillColor() );
    }

    private LogLine lineAt( int index ) {
        return ( LogLine ) getChildren().get( index );
    }

    public static int getMaxLines() {
        return maxLines.get();
    }

}
