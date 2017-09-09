package com.athaydes.logfx.ui;

import com.athaydes.logfx.binding.BindableValue;
import com.athaydes.logfx.concurrency.TaskRunner;
import com.athaydes.logfx.file.FileChangeWatcher;
import com.athaydes.logfx.file.FileContentReader;
import com.athaydes.logfx.file.FileContentReader.FileQueryResult;
import com.athaydes.logfx.file.OutsideRangeQueryResult;
import com.athaydes.logfx.text.HighlightExpression;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;

/**
 * View of a log file.
 */
public class LogView extends VBox {

    private static final Logger log = LoggerFactory.getLogger( LogView.class );

    public static final int MAX_LINES = 100;
    private static final double DELTA_FACTOR = 10.0;

    private final HighlightOptions highlightOptions;
    private final ExecutorService fileReaderExecutor = Executors.newSingleThreadExecutor();
    private final BooleanProperty tailingFile = new SimpleBooleanProperty( false );
    private final FileContentReader fileContentReader;
    private final File file;
    private final FileChangeWatcher fileChangeWatcher;
    private final Supplier<LogLine> logLineFactory;
    private final TaskRunner taskRunner;
    private final SelectionHandler selectionHandler;
    private volatile Consumer<Boolean> onFileExists = ( ignore ) -> {
    };

    @MustCallOnJavaFXThread
    public LogView( BindableValue<Font> fontValue,
                    ReadOnlyDoubleProperty widthProperty,
                    HighlightOptions highlightOptions,
                    FileContentReader fileContentReader,
                    TaskRunner taskRunner ) {
        this.highlightOptions = highlightOptions;
        this.fileContentReader = fileContentReader;
        this.taskRunner = taskRunner;
        this.selectionHandler = new SelectionHandler( this );
        this.file = fileContentReader.getFile();
        this.fileChangeWatcher = new FileChangeWatcher( file );

        final HighlightExpression expression = highlightOptions.expressionFor( "" );
        final NumberBinding width = Bindings.max( widthProperty(), widthProperty );

        logLineFactory = () -> new LogLine( fontValue, width,
                expression.getBkgColor(), expression.getFillColor() );

        for ( int i = 0; i < MAX_LINES; i++ ) {
            getChildren().add( logLineFactory.get() );
        }

        highlightOptions.getObservableExpressions().addListener( ( Observable observable ) -> {
            for ( int i = 0; i < MAX_LINES; i++ ) {
                updateLine( i );
            }
        } );

        fileChangeWatcher.setOnChange( this::onFileChange );

        tailingFile.addListener( event -> {
            if ( tailingFile.get() ) {
                onFileChange();
            }
        } );
    }

    Optional<ClipboardContent> getSelection() {
        return selectionHandler.getSelection();
    }

    void loadFileContents() {
        immediateOnFileChange();
    }

    void setOnFileExists( Consumer<Boolean> onFileExists ) {
        this.onFileExists = onFileExists;
    }

    void pageUp() {
        move( MAX_LINES * DELTA_FACTOR );
    }

    void pageDown() {
        move( -MAX_LINES * DELTA_FACTOR );
    }

    void move( double deltaY ) {
        int lines = Double.valueOf( deltaY / DELTA_FACTOR ).intValue();
        log.trace( "Moving by deltaY={}, lines={}", deltaY, lines );

        fileReaderExecutor.execute( () -> {
            Optional<? extends List<String>> result;
            if ( deltaY > 0.0 ) {
                result = fileContentReader.moveUp( lines );
                result.ifPresent( this::addTopLines );
            } else {
                result = fileContentReader.moveDown( -lines );
                result.ifPresent( this::addBottomLines );
            }
            onFileExists.accept( result.isPresent() );
        } );
    }

    BooleanProperty tailingFileProperty() {
        return tailingFile;
    }

    void toTop() {
        fileReaderExecutor.execute( () -> {
            fileContentReader.top();
            onFileChange();
        } );
    }

    void goTo( LocalDateTime dateTime, IntConsumer whenDoneAcceptLineNumber ) {
        // FIXME find the pattern in the files or ask user for a pattern
        Pattern pattern = Pattern.compile( "INFO ([A-za-z0-9: ]+)-.*" );
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern( "EEE MMM dd HH:mm:ss z yyyy" );

        fileReaderExecutor.execute( () -> {
            FileQueryResult result = fileContentReader.moveTo( dateTime, ( line ) -> {
                Matcher matcher = pattern.matcher( line );
                if ( matcher.matches() ) {
                    try {
                        return Optional.of( LocalDateTime.parse( matcher.group( 1 ).trim(), dateFormat ) );
                    } catch ( DateTimeParseException e ) {
                        log.warn( "Error parsing date: {}", e.toString() );
                    }
                }
                return Optional.empty();
            } );
            if ( result.isSuccess() ) {
                log.debug( "Successfully found date: {}, result: {}", dateTime, result );
                onFileChange();

                final int lineNumber = result.isAfterRange() ?
                        MAX_LINES :
                        ( result.isBeforeRange() ?
                                1 :
                                result.fileLineNumber() );
                final boolean outOfRange = result instanceof OutsideRangeQueryResult;

                Platform.runLater( () -> {
                    LogLine line = lineAt( lineNumber - 1 );
                    line.animate( outOfRange ? Color.RED : Color.LAWNGREEN );
                    whenDoneAcceptLineNumber.accept( lineNumber );
                } );
            } else {
                log.warn( "Failed to open date-time in log, could not recognize dates in the log" );
            }
        } );
    }

    private void addTopLines( List<String> topLines ) {
        Platform.runLater( () -> {
            ObservableList<Node> children = getChildren();
            if ( topLines.size() >= children.size() ) {
                children.clear();
            } else {
                children.remove( children.size() - topLines.size(), children.size() );
            }

            children.addAll( 0, topLines.stream()
                    .map( lineText -> {
                        LogLine logLine = logLineFactory.get();
                        updateLine( logLine, lineText );
                        return logLine;
                    } )
                    .collect( toList() ) );
        } );

    }

    private void addBottomLines( List<String> bottomLines ) {
        Platform.runLater( () -> {
            ObservableList<Node> children = getChildren();
            if ( bottomLines.size() >= children.size() ) {
                children.clear();
            } else {
                children.remove( 0, bottomLines.size() );
            }

            children.addAll( bottomLines.stream()
                    .map( lineText -> {
                        LogLine logLine = logLineFactory.get();
                        updateLine( logLine, lineText );
                        return logLine;
                    } )
                    .collect( toList() ) );
        } );
    }

    private void onFileChange() {
        taskRunner.runWithMaxFrequency( this::immediateOnFileChange, 2_000 );
    }

    private void immediateOnFileChange() {
        fileReaderExecutor.execute( () -> {
            if ( tailingFileProperty().get() ) {
                fileContentReader.tail();
            }
            Optional<? extends List<String>> lines = fileContentReader.refresh();
            lines.ifPresent( list -> updateWith( list.iterator() ) );
            onFileExists.accept( lines.isPresent() );
        } );
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

    @MustCallOnJavaFXThread
    private void updateLine( int index ) {
        LogLine line = lineAt( index );
        updateLine( line, line.getText() );// update only styles
    }

    @MustCallOnJavaFXThread
    private void updateLine( LogLine line, String text ) {
        HighlightExpression expression = highlightOptions.expressionFor( text );
        line.setText( text, expression.getBkgColor(), expression.getFillColor() );
    }

    @MustCallOnJavaFXThread
    private LogLine lineAt( int index ) {
        return ( LogLine ) getChildren().get( index );
    }

    File getFile() {
        return file;
    }

    void closeFileReader() {
        fileChangeWatcher.close();
        fileReaderExecutor.shutdown();
    }
}
