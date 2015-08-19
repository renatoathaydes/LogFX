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

import java.util.Arrays;
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

    public void showLines( String[] lines ) {
        final int max = maxLines.get();
        final String[] linesCopy = Arrays.copyOf( lines, max );
        final int extraLines = max - lines.length;
        if ( extraLines > 0 ) {
            Arrays.fill( linesCopy, max - extraLines, max, "" );
        }
        Platform.runLater( () -> {
            for ( int i = max; i > 0; i-- ) {
                String text = linesCopy[ i - 1 ];
                updateLine( lineAt( max - i ), text );
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
