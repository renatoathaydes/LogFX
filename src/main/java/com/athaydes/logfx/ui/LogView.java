package com.athaydes.logfx.ui;

import com.athaydes.logfx.concurrency.IdentifiableRunnable;
import com.athaydes.logfx.concurrency.TaskRunner;
import com.athaydes.logfx.config.Config;
import com.athaydes.logfx.data.LinesScroller;
import com.athaydes.logfx.data.LinesSetter;
import com.athaydes.logfx.data.LogFile;
import com.athaydes.logfx.data.LogLineColors;
import com.athaydes.logfx.file.FileChangeWatcher;
import com.athaydes.logfx.file.FileContentReader;
import com.athaydes.logfx.file.FileSearcher;
import com.athaydes.logfx.iterable.ObservableListView;
import com.athaydes.logfx.text.DateTimeFormatGuess;
import com.athaydes.logfx.text.DateTimeFormatGuesser;
import com.athaydes.logfx.text.HighlightExpression;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

/**
 * View of a log file.
 */
public class LogView extends VBox implements SelectableContainer {

    private static final Logger log = LoggerFactory.getLogger( LogView.class );

    private static final Runnable DO_NOTHING = () -> {
    };

    public static final int MAX_LINES = 512;

    private final ExecutorService fileReaderExecutor = Executors.newSingleThreadExecutor();
    private final BooleanProperty tailingFile = new SimpleBooleanProperty( false );
    private final BooleanProperty allowRefresh = new SimpleBooleanProperty( true );
    private final BooleanProperty showTimeGap = new SimpleBooleanProperty( false );
    private final IntegerProperty minGapInMillis = new SimpleIntegerProperty( 1_000 );
    private final Config config;
    private final LogLineHighlighter highlighter;
    private final FileContentReader fileContentReader;
    private final LogFile logFile;
    private final FileChangeWatcher fileChangeWatcher;
    private final TaskRunner taskRunner;
    private final SelectionHandler selectionHandler;
    private final DateTimeFormatGuesser dateTimeFormatGuesser = DateTimeFormatGuesser.standard();
    private final LinesScroller linesScroller = new LinesScroller( MAX_LINES, this::lineContent,
            new LinesSetter( this::updateLines ) );
    private final ReentrantLock linesLock = new ReentrantLock( true );

    private volatile Consumer<Boolean> onFileExists = ( ignore ) -> {
    };

    private volatile Runnable onFileUpdate = DO_NOTHING;

    private DateTimeFormatGuess dateTimeFormatGuess = null;
    private IntConsumer scrollToLineFunction = ( i ) -> {
    };
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
        showTimeGap.addListener( ( Observable o ) -> {
            log.debug( "Updated time gap enabled property: {}", o );
            updateWith( getLines() );
        } );

        this.highlighter = new LogLineHighlighter( config, expressionsChangeListener, logFile );

        this.highlightGroupChangeListener = ( Observable o ) -> {
            highlighter.updateGroupFrom( logFile );
            immediateOnFileChange();
        };

        wireListeners();

        final NumberBinding width = Bindings.max( widthProperty(), widthProperty );

        LogLineColors logLineColors = highlighter.logLineColorsFor( "" );

        for ( int i = 0; i < MAX_LINES; i++ ) {
            getChildren().add( new LogLine( config.fontProperty(), i, width,
                    logLineColors.getBackground(), logLineColors.getFill() ) );
        }

        tailingFile.addListener( event -> {
            if ( tailingFile.get() ) {
                onFileChange();
            }
        } );

        this.fileChangeWatcher = new FileChangeWatcher( logFile.file, taskRunner, this::onFileChange );
    }

    @Override
    public Node getNode() {
        return this;
    }

    public void setScrollToLineFunction( IntConsumer scrollToLineFunction ) {
        this.scrollToLineFunction = scrollToLineFunction;
    }

    IntegerProperty getMinTimeGap() {
        return minGapInMillis;
    }

    @Override
    public void scrollToView( SelectionHandler.SelectableNode node ) {
        scrollToLineFunction.accept( node.getLineIndex() );
    }

    @Override
    public ObservableListView<? extends SelectionHandler.SelectableNode, Node> getSelectables() {
        return new ObservableListView<>( SelectionHandler.SelectableNode.class, getChildrenUnmodifiable() );
    }

    @Override
    public CompletionStage<SelectionHandler.SelectableNode> nextSelectable() {
        return loadNextSelectable( false );
    }

    @Override
    public CompletionStage<SelectionHandler.SelectableNode> previousSelectable() {
        return loadNextSelectable( true );
    }

    public Optional<Pair<SelectionHandler.SelectableNode, SelectionHandler.SelectableNode>> getSelectableEnds() {
        var children = getChildren();
        if ( !children.isEmpty() ) {
            return Optional.of( new Pair<>( lineAt( 0 ), lineAt( children.size() - 1 ) ) );
        }
        return Optional.empty();
    }

    private CompletionStage<SelectionHandler.SelectableNode> loadNextSelectable( boolean up ) {
        var future = new CompletableFuture<SelectionHandler.SelectableNode>();
        moveBy( 1, up, () -> Platform.runLater( () -> {
            var children = getChildren();
            if ( !children.isEmpty() ) {
                future.complete( lineAt( up ? 0 : children.size() - 1 ) );
            }
        } ) );
        return future;
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
        move( 51.0, ( d ) -> {
        } );
    }

    void pageDown() {
        move( -51.0, ( d ) -> {
        } );
    }

    void move( double deltaY, DoubleConsumer useScrollFactor ) {
        if ( deltaY == 0.0 ) return;

        if ( !allowRefresh.get() || fileReaderExecutor.isShutdown() ) {
            log.trace( "Ignoring call to move up/down as the {}",
                    fileReaderExecutor.isShutdown()
                            ? "reader executor is shutdown"
                            : "LogView is paused" );
            return;
        }

        // avoid repeating task in quick succession
        taskRunner.runWithMaxFrequency( new IdentifiableRunnable( "scroll", () -> {
            double scrollFactor = scaleScrollDelta( deltaY );
            int lines = Math.toIntExact( Math.round( MAX_LINES * scrollFactor ) );

            log.trace( "Moving by deltaY={}, factor={}, lines={}", deltaY, scrollFactor, lines );

            moveBy( lines, deltaY > 0.0, () -> useScrollFactor.accept( scrollFactor ) );
        } ), 50L, 0L );
    }

    private void moveBy( int lines, boolean up, Runnable then ) {
        fileReaderExecutor.execute( () -> {
            Optional<? extends List<String>> result;
            if ( up ) {
                result = fileContentReader.moveUp( lines );
                result.ifPresent( this::addTopLines );
            } else {
                result = fileContentReader.moveDown( lines );
                result.ifPresent( this::addBottomLines );
            }
            onFileExists.accept( result.isPresent() );
            if ( result.isPresent() && !result.get().isEmpty() ) {
                then.run();
            }
        } );
    }

    private static double scaleScrollDelta( double deltaY ) {
        double absDeltaY = Math.abs( deltaY );
        if ( absDeltaY < 0.001 ) return 0.0;
        if ( absDeltaY > 100.0 ) return 0.8;
        if ( absDeltaY > 50.0 ) return 0.5;
        return 0.2;
    }

    BooleanProperty tailingFileProperty() {
        return tailingFile;
    }

    BooleanProperty timeGapProperty() {
        return showTimeGap;
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
                findFileDateTimeFormatterFromFileContents( Optional.empty() );
            }
            if ( dateTimeFormatGuess == null ) {
                log.warn( "Could not guess date-time format from this log file, " +
                        "will not be able to find log lines by date" );
                Dialog.showMessage( "Unable to guess date-time format in file\n" +
                        logFile.file.getName(), Dialog.MessageLevel.INFO );
                return;
            }

            long startTime = System.currentTimeMillis();

            FileContentReader searchReader = fileContentReader.makeCopy();
            var searcher = new FileSearcher( searchReader );
            var comparisonsCount = new AtomicLong( 0 );

            var searchResult = searcher.search( line -> {
                var lineDateTime = dateTimeFormatGuess.guessDateTime( line );
                if ( lineDateTime.isEmpty() ) return FileSearcher.Comparison.UNKNOWN;
                comparisonsCount.incrementAndGet();
                return FileSearcher.Comparison.of( dateTime.compareTo( lineDateTime.get() ) );
            } );
            if ( searchResult.isEmpty() ) {
                log.warn( "Failed to find date-time in log, could not recognize dates in the log (took {} ms)",
                        System.currentTimeMillis() - startTime );

                Dialog.showMessage( "Unable to go to date-time.\n" +
                        "The date-times in the file could not be recognized.", Dialog.MessageLevel.WARNING );
                return;
            }

            // the searchReader found a result, so we need to copy its state to the original reader
            fileContentReader.copyState( searchReader );
            final var result = searchResult.get();
            if ( log.isInfoEnabled() ) {
                log.info( "Successfully found date (took {} ms, {} comparisons): {}, result: {}",
                        System.currentTimeMillis() - startTime, comparisonsCount.get(), dateTime, result );
            }
            onFileChange( () -> Platform.runLater( () -> {
                LogLine line = lineAt( result.lineNumber() );
                line.animate( result.resultCase() != FileSearcher.ResultCase.AT ? Color.RED : Color.LAWNGREEN );
                whenDoneAcceptLineNumber.accept( result.lineNumber() );
            } ) );
        } );
    }

    void onFileUpdate( Runnable onFileUpdate ) {
        this.onFileUpdate = onFileUpdate;
    }

    private String lineContent( int index ) {
        return lineAt( index ).getText();
    }

    private List<String> getLines() {
        var result = new ArrayList<String>( MAX_LINES );
        for ( int i = 0; i < MAX_LINES; i++ ) {
            result.add( lineContent( i ) );
        }
        return result;
    }

    private void updateLines( List<LinesSetter.LineChange> changes ) {
        var allLines = new String[ MAX_LINES ];
        for ( var change : changes ) {
            allLines[ change.index() ] = change.text();
        }
        updateWith( List.of( allLines ) );
    }

    // must be called from fileReaderExecutor Thread
    private DateTimeFormatGuess findFileDateTimeFormatterFromFileContents(
            @SuppressWarnings( "OptionalUsedAsFieldOrParameterType" ) Optional<List<String>> lines ) {
        Optional<? extends List<String>> maybeLines = lines.or( fileContentReader::refresh );
        DateTimeFormatGuess result = null;
        if ( maybeLines.isPresent() ) {
            result = dateTimeFormatGuesser
                    .guessDateTimeFormats( maybeLines.get() )
                    .orElse( null );
            if ( result != null ) {
                dateTimeFormatGuess = result;
            }
        } else {
            log.warn( "Unable to extract any date-time formatters from file as the file could not be read: {}",
                    logFile );
            Dialog.showMessage( "Could not read file\n" + logFile.file.getName(), Dialog.MessageLevel.INFO );
        }
        return result;
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
            taskRunner.runWithMaxFrequency( this::immediateOnFileChange, 2_000L, 0L );
        }
    }

    private void onFileChange( Runnable andThen ) {
        if ( allowRefresh.get() ) {
            taskRunner.runWithMaxFrequency( () -> immediateOnFileChange( andThen ), 2_000L, 0L );
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
            lines.ifPresent( this::updateWith );
            try {
                onFileExists.accept( lines.isPresent() );
            } finally {
                andThen.run();
            }
        } );
    }

    private void updateWith( List<String> lines ) {
        Objects.requireNonNull( lines );
        final var minTimeGap = Duration.ofMillis( this.minGapInMillis.get() );
        final DateTimeFormatGuess timeFormatGuess;
        if ( showTimeGap.get() ) {
            timeFormatGuess = findFileDateTimeFormatterFromFileContents( Optional.of( lines ) );
        } else {
            timeFormatGuess = null;
        }
        fileReaderExecutor.execute( () -> {
            int index = 0;
            // unlock happens either when an error happens in this Thread or when all lines have been updated
            linesLock.lock();
            var cancelled = new AtomicBoolean( false );
            var latch = new CountDownLatch( Math.max( lines.size(), MAX_LINES ) );
            ZonedDateTime previousTime = null;

            try {
                for ( ; index < lines.size() && !cancelled.get(); index++ ) {
                    final String lineText = lines.get( index );

                    // null line: leave the line alone and proceed
                    if ( lineText == null ) {
                        latch.countDown();
                        continue;
                    }

                    Duration timeGap;
                    if ( timeFormatGuess == null ) {
                        timeGap = null;
                    } else {
                        var time = timeFormatGuess.guessDateTime( lineText ).orElse( null );
                        timeGap = previousTime == null ? null :
                                getIfGreater( minTimeGap, Duration.between( previousTime, time ) );
                        previousTime = time;
                    }

                    final int currentIndex = index;

                    Platform.runLater( () -> {
                        if ( cancelled.get() ) return;
                        try {
                            LogLine line = lineAt( currentIndex );
                            line.setText( lineText, highlighter.logLineColorsFor( lineText ), timeGap );
                        } finally {
                            latch.countDown();
                        }
                    } );
                }

                // fill the remaining lines with the empty String
                for ( ; index < MAX_LINES; index++ ) {
                    final int currentIndex = index;
                    Platform.runLater( () -> {
                        if ( cancelled.get() ) return;
                        try {
                            LogLine line = lineAt( currentIndex );
                            line.setText( "", highlighter.logLineColorsFor( "" ), null );
                        } finally {
                            latch.countDown();
                        }
                    } );
                }
            } catch ( Exception e ) {
                cancelled.set( true );
                linesLock.unlock();
                return;
            }

            try {
                var ok = latch.await( 15, TimeUnit.SECONDS );
                if ( !ok ) {
                    log.warn( "Timeout while trying to update log lines" );
                }
            } catch ( InterruptedException e ) {
                log.warn( "Interrupted while trying to update log lines" );
            } finally {
                cancelled.set( true );
                linesLock.unlock();
            }
        } );
    }

    private static Duration getIfGreater( Duration minTimeGap, Duration duration ) {
        if ( duration == null ) return null;
        return duration.compareTo( minTimeGap ) > 0 ? duration : null;
    }

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
                        .toList();
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

            // observableExpressions cannot be null here
            observableExpressions.addListener( expressionsChangeListener );
        }

        void unwireListeners() {
            observableExpressions.removeListener( expressionsChangeListener );
        }
    }
}
