package com.athaydes.logfx.ui;

import com.athaydes.logfx.concurrency.TaskRunner;
import com.athaydes.logfx.config.Config;
import com.athaydes.logfx.data.LinesScroller;
import com.athaydes.logfx.data.LogFile;
import com.athaydes.logfx.data.LogLineColors;
import com.athaydes.logfx.file.FileChangeWatcher;
import com.athaydes.logfx.file.FileContentReader;
import com.athaydes.logfx.file.FileContentReader.FileQueryResult;
import com.athaydes.logfx.file.OutsideRangeQueryResult;
import com.athaydes.logfx.text.DateTimeFormatGuess;
import com.athaydes.logfx.text.DateTimeFormatGuesser;
import com.athaydes.logfx.text.HighlightExpression;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * View of a log file.
 */
public class LogView extends VBox {

    private static final Logger log = LoggerFactory.getLogger( LogView.class );

    private static final Runnable DO_NOTHING = () -> {
    };

    public static final int MAX_LINES = 100;
    private static final double DELTA_FACTOR = 10.0;

    private final ExecutorService fileReaderExecutor = Executors.newSingleThreadExecutor();
    private final BooleanProperty tailingFile = new SimpleBooleanProperty( false );
    private final BooleanProperty allowRefresh = new SimpleBooleanProperty( true );
    private final Config config;
    private final LogLineHighlighter highlighter;
    private final FileContentReader fileContentReader;
    private final LogFile logFile;
    private final FileChangeWatcher fileChangeWatcher;
    private final TaskRunner taskRunner;
    private final SelectionHandler selectionHandler;
    private final DateTimeFormatGuesser dateTimeFormatGuesser = DateTimeFormatGuesser.standard();
    private final LinesScroller linesScroller = new LinesScroller( MAX_LINES, this::lineContent, this::setLine );

    private volatile Consumer<Boolean> onFileExists = ( ignore ) -> {
    };

    private volatile Runnable onFileUpdate = DO_NOTHING;

    private DateTimeFormatGuess dateTimeFormatGuess = null;
    private final InvalidationListener expressionsChangeListener;
    private final InvalidationListener highlightGroupChangeListener;

    @MustCallOnJavaFXThread
    public LogView( Config config,
                    ReadOnlyDoubleProperty widthProperty,
                    LogFile logFile,
                    FileContentReader fileContentReader,
                    TaskRunner taskRunner ) {
        this.config = config;
        this.fileContentReader = fileContentReader;
        this.taskRunner = taskRunner;
        this.selectionHandler = new SelectionHandler( this );
        this.logFile = logFile;

        this.expressionsChangeListener = ( Observable o ) -> immediateOnFileChange();

        logFile.highlightGroupProperty().addListener( expressionsChangeListener );

        this.highlighter = new LogLineHighlighter( config, expressionsChangeListener, logFile );

        this.highlightGroupChangeListener = ( Observable o ) -> {
            highlighter.updateGroupFrom( logFile );
            immediateOnFileChange();
        };

        wireListeners();

        final NumberBinding width = Bindings.max( widthProperty(), widthProperty );

        LogLineColors logLineColors = highlighter.logLineColorsFor( "" );

        for ( int i = 0; i < MAX_LINES; i++ ) {
            getChildren().add( new LogLine( config.fontProperty(), width,
                    logLineColors.getBackground(), logLineColors.getFill() ) );
        }

        tailingFile.addListener( event -> {
            if ( tailingFile.get() ) {
                onFileChange();
            }
        } );

        this.fileChangeWatcher = new FileChangeWatcher( logFile.file, taskRunner, this::onFileChange );
    }

    BooleanProperty allowRefreshProperty() {
        return allowRefresh;
    }

    SelectionHandler getSelectionHandler() {
        return selectionHandler;
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
        if ( !allowRefresh.get() ) {
            log.trace( "Ignoring call to move up/down as the LogView is paused" );
            return;
        }

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

    void goTo( ZonedDateTime dateTime, IntConsumer whenDoneAcceptLineNumber ) {
        fileReaderExecutor.execute( () -> {
            if ( dateTimeFormatGuess == null ) {
                findFileDateTimeFormatterFromFileContents();
            }
            if ( dateTimeFormatGuess == null ) {
                log.warn( "Could not guess date-time format from this log file, " +
                        "will not be able to find log lines by date" );
                Dialog.showMessage( "Unable to guess date-time format in file\n" +
                        logFile.file.getName(), Dialog.MessageLevel.INFO );
                return;
            }

            long startTime = System.currentTimeMillis();

            FileQueryResult result = fileContentReader.moveTo( dateTime, dateTimeFormatGuess::guessDateTime );
            if ( result.isSuccess() ) {
                if ( log.isInfoEnabled() ) {
                    log.info( "Successfully found date (in {} ms): {}, result: {}",
                            System.currentTimeMillis() - startTime, dateTime, result );
                }

                final int lineNumber = result.isAfterRange() ?
                        MAX_LINES :
                        ( result.isBeforeRange() ?
                                1 :
                                result.fileLineNumber() );
                final boolean outOfRange = result instanceof OutsideRangeQueryResult;

                onFileChange( () -> Platform.runLater( () -> {
                    LogLine line = lineAt( lineNumber - 1 );
                    line.animate( outOfRange ? Color.RED : Color.LAWNGREEN );
                    whenDoneAcceptLineNumber.accept( lineNumber );
                } ) );
            } else {
                log.warn( "Failed to open date-time in log, could not recognize dates in the log (took {} ms)",
                        System.currentTimeMillis() - startTime );

                Dialog.showMessage( "Unable to open date-time.\n" +
                        "The date-times in the file could not be recognized.", Dialog.MessageLevel.WARNING );
            }
        } );
    }

    void onFileUpdate( Runnable onFileUpdate ) {
        this.onFileUpdate = onFileUpdate;
    }

    private void setLine( int index, String line ) {
        LogLineColors logLineColors = highlighter.logLineColorsFor( line );
        LogLine logLine = lineAt( index );
        logLine.setText( line, logLineColors );

    }

    // must be called from fileReaderExecutor Thread
    private void findFileDateTimeFormatterFromFileContents() {
        Optional<? extends List<String>> lines = fileContentReader.refresh();
        if ( lines.isPresent() ) {
            dateTimeFormatGuess = dateTimeFormatGuesser
                    .guessDateTimeFormats( lines.get() ).orElse( null );
        } else {
            log.warn( "Unable to extract any date-time formatters from file as the file could not be read: {}",
                    logFile );
            Dialog.showMessage( "Could not read file\n" + logFile.file.getName(), Dialog.MessageLevel.INFO );
        }
    }

    private void addTopLines( List<String> topLines ) {
        Platform.runLater( () -> linesScroller.setTopLines( topLines ) );
    }

    private void addBottomLines( List<String> bottomLines ) {
        Platform.runLater( () -> linesScroller.setBottomLines( bottomLines ) );
    }

    private void onFileChange() {
        onFileUpdate.run();
        if ( allowRefresh.get() ) {
            taskRunner.runWithMaxFrequency( this::immediateOnFileChange, 2_000 );
        }
    }

    private void onFileChange( Runnable andThen ) {
        if ( allowRefresh.get() ) {
            taskRunner.runWithMaxFrequency( () -> immediateOnFileChange( andThen ), 2_000 );
        }
    }

    private void immediateOnFileChange() {
        immediateOnFileChange( DO_NOTHING );
    }

    private void immediateOnFileChange( Runnable andThen ) {
        if ( fileReaderExecutor.isShutdown() ) return;
        Predicate<String> filter = highlighter.getLineFilter().orElse( null );
        fileReaderExecutor.execute( () -> {
            fileContentReader.setLineFilter( filter );
            if ( tailingFileProperty().get() ) {
                fileContentReader.tail();
            }
            Optional<? extends List<String>> lines = fileContentReader.refresh();
            lines.ifPresent( list -> updateWith( list.iterator() ) );
            try {
                onFileExists.accept( lines.isPresent() );
            } finally {
                andThen.run();
            }
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
    private void updateLine( LogLine line, String text ) {
        LogLineColors logLineColors = highlighter.logLineColorsFor( text );
        line.setText( text, logLineColors );
    }

    private String lineContent( int index ) {
        return lineAt( index ).getText();
    }

    @MustCallOnJavaFXThread
    private LogLine lineAt( int index ) {
        return ( LogLine ) getChildren().get( index );
    }

    File getFile() {
        return logFile.file;
    }

    LogFile getLogFile() {
        return logFile;
    }

    void closeFileReader() {
        try {
            removeListeners();
        } finally {
            fileChangeWatcher.close();
            fileReaderExecutor.shutdown();
        }
    }

    private void wireListeners() {
        logFile.highlightGroupProperty().addListener( highlightGroupChangeListener );
        config.standardLogColorsProperty().addListener( expressionsChangeListener );
        config.filtersEnabledProperty().addListener( expressionsChangeListener );
    }

    private void removeListeners() {
        highlighter.unwireListeners();
        logFile.highlightGroupProperty().removeListener( highlightGroupChangeListener );
        config.standardLogColorsProperty().removeListener( expressionsChangeListener );
        config.filtersEnabledProperty().removeListener( expressionsChangeListener );
    }

    private static final class LogLineHighlighter {

        private final Config config;
        private final InvalidationListener expressionsChangeListener;
        private ObservableList<HighlightExpression> observableExpressions;

        LogLineHighlighter( Config config, InvalidationListener expressionsChangeListener, LogFile logFile ) {
            this.config = config;
            this.expressionsChangeListener = expressionsChangeListener;
            updateGroupFrom( logFile );
        }

        LogLineColors logLineColorsFor( String text ) {
            for ( HighlightExpression expression : observableExpressions ) {
                if ( expression.matches( text ) ) {
                    return expression.getLogLineColors();
                }
            }
            return config.standardLogColorsProperty().get();
        }

        Optional<Predicate<String>> getLineFilter() {
            if ( config.filtersEnabledProperty().get() ) {
                List<HighlightExpression> filteredExpressions = observableExpressions.stream()
                        .filter( HighlightExpression::isFiltered )
                        .collect( Collectors.toList() );
                return Optional.of( ( line ) -> filteredExpressions.stream()
                        .anyMatch( ( exp ) -> exp.matches( line ) ) );
            } else {
                return Optional.empty();
            }
        }

        void updateGroupFrom( LogFile logFile ) {
            if ( observableExpressions != null ) unwireListeners();
            String groupName = logFile.getHighlightGroup();
            observableExpressions = config.getHighlightGroups().getByName( groupName );
            if ( observableExpressions == null ) {
                log.warn( "Trying to use highlight group that does not exist [{}] for file: {}.",
                        groupName, logFile.file );
                observableExpressions = config.getHighlightGroups().getDefault();
                Platform.runLater( () -> logFile.highlightGroupProperty().setValue( "" ) );
            }
            observableExpressions.addListener( expressionsChangeListener );
        }

        void unwireListeners() {
            observableExpressions.removeListener( expressionsChangeListener );
        }
    }
}
