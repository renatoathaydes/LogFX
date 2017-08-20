package com.athaydes.logfx.ui;

import com.athaydes.logfx.binding.BindableValue;
import com.athaydes.logfx.file.FileContentReader;
import com.athaydes.logfx.text.HighlightExpression;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.io.File;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * View of a log file.
 */
public class LogView extends VBox {

    private static final int MAX_LINES = 100;

    private final HighlightOptions highlightOptions;
    private final FileContentReader fileContentReader;

    public LogView( BindableValue<Font> fontValue,
                    ReadOnlyDoubleProperty widthProperty,
                    HighlightOptions highlightOptions,
                    FileContentReader fileContentReader ) {
        this.highlightOptions = highlightOptions;
        this.fileContentReader = fileContentReader;

        final HighlightExpression expression = highlightOptions.expressionFor( "" );
        final NumberBinding width = Bindings.max( widthProperty(), widthProperty );

        for ( int i = 0; i < MAX_LINES; i++ ) {
            getChildren().add( new LogLine( fontValue, width,
                    expression.getBkgColor(), expression.getFillColor() ) );
        }

        highlightOptions.getObservableExpressions().addListener( ( Observable observable ) -> {
            for ( int i = 0; i < MAX_LINES; i++ ) {
                updateLine( i );
            }
        } );

        fileContentReader.setChangeListener( this::onFileChange );

        FxUtils.runLater( this::immediateOnFileChange );
    }

    private void onFileChange() {
        FxUtils.runWithMaxFrequency( this::immediateOnFileChange, 2_000 );
    }

    private void immediateOnFileChange() {
        Optional<Stream<String>> lines = fileContentReader.refresh( MAX_LINES );
        if ( lines.isPresent() ) {
            updateWith( lines.get().iterator() );
        } else {
            fileDoesNotExist();
        }
    }

    private void updateWith( Iterator<String> lines ) {
        int index = 0;

        while ( lines.hasNext() ) {
            final int currentIndex = index;
            final String lineText = lines.next();
            Platform.runLater( () -> {
                LogLine line = lineAt( currentIndex );
                updateLine( line, lineText );
            } );
            index++;
        }

        // fill the remaining lines with the empty String
        for ( ; index < MAX_LINES; index++ ) {
            final int currentIndex = index;
            Platform.runLater( () -> {
                LogLine line = lineAt( currentIndex );
                updateLine( line, "" );
            } );
        }
    }

    private void fileDoesNotExist() {
        // TODO show special tab
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

    public File getFile() {
        return fileContentReader.getFile();
    }

    public void closeFileReader() {
        fileContentReader.close();
    }
}
