package com.athaydes.logfx.ui;

import com.athaydes.logfx.concurrency.Cancellable;
import com.athaydes.logfx.concurrency.TaskRunner;
import com.athaydes.logfx.config.HighlightGroups;
import com.athaydes.logfx.data.LogFile;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import static com.athaydes.logfx.ui.LogView.MAX_LINES;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * A container of {@link LogView}s.
 */
public final class LogViewPane {

    private static final Logger log = LoggerFactory.getLogger( LogViewPane.class );

    private final TaskRunner taskRunner;
    private final HighlightGroups highlightGroups;

    private final SplitPane pane = new SplitPane();

    private final AtomicReference<Cancellable> previousDividerSettingTask = new AtomicReference<>();

    // a simple observable that changes state every time a change occurs in a pane divider
    // (changes in number of dividers as well as in their positions)
    private final ObjectProperty<Boolean> panesDividersObservable = new SimpleObjectProperty<>( false );

    @MustCallOnJavaFXThread
    public LogViewPane( TaskRunner taskRunner,
                        Supplier<StartUpView> startUpViewGetter,
                        HighlightGroups highlightGroups,
                        boolean showEmptyPanel ) {
        this.taskRunner = taskRunner;
        this.highlightGroups = highlightGroups;
        MenuItem copyMenuItem = new MenuItem( "Copy Selection" );
        copyMenuItem.setAccelerator( new KeyCodeCombination( KeyCode.C, KeyCombination.SHORTCUT_DOWN ) );
        copyMenuItem.setOnAction( event -> getFocusedView()
                .flatMap( wrapper -> wrapper.logView.getSelectionHandler().getSelection() )
                .ifPresent( content -> Clipboard.getSystemClipboard().setContent( content ) ) );

        MenuItem selectAllMenuItem = new MenuItem( "Select All" );
        selectAllMenuItem.setAccelerator( new KeyCodeCombination( KeyCode.A, KeyCombination.SHORTCUT_DOWN ) );
        selectAllMenuItem.setOnAction( event -> selectAllLogViewRows() );

        MenuItem closeMenuItem = new MenuItem( "Close" );
        closeMenuItem.setAccelerator( new KeyCodeCombination( KeyCode.W, KeyCombination.SHORTCUT_DOWN ) );
        closeMenuItem.setOnAction( ( event ) ->
                getFocusedView().ifPresent( LogViewWrapper::closeView ) );

        MenuItem pauseMenuItem = new MenuItem( "Pause/Resume file auto-refresh" );
        pauseMenuItem.setAccelerator( new KeyCodeCombination( KeyCode.P, KeyCombination.SHORTCUT_DOWN ) );
        pauseMenuItem.setOnAction( ( event ) ->
                getFocusedView().ifPresent( view -> view.header.togglePauseRefresh() ) );

        MenuItem minimizeMenuItem = new MenuItem( "Minimize" );
        minimizeMenuItem.setAccelerator( new KeyCodeCombination( KeyCode.M,
                KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN ) );
        minimizeMenuItem.setOnAction( event -> {
            if ( pane.getItems().size() < 2 ) {
                return; // we can't hide anything if there isn't more than 1 pane
            }
            getFocusedView().ifPresent( wrapper -> {
                var previousTask = previousDividerSettingTask.get();
                if ( previousTask != null ) previousTask.cancel();
                previousDividerSettingTask.set( taskRunner.repeat( 2, Duration.ofMillis( 150 ), () -> Platform.runLater( () -> {
                    int index = pane.getItems().indexOf( wrapper );
                    if ( index == pane.getItems().size() - 1 ) {
                        // last pane, so we can only hide by opening up the previous one
                        pane.setDividerPosition( index - 1, 1.0 );
                    } else if ( index >= 0 ) {
                        // in all other cases, just hide the pane itself
                        pane.setDividerPosition( index, 0.0 );
                    }
                } ) ) );
            } );
        } );

        MenuItem maximizeMenuItem = new MenuItem( "Maximize" );
        maximizeMenuItem.setAccelerator( new KeyCodeCombination( KeyCode.M,
                KeyCombination.SHORTCUT_DOWN ) );
        maximizeMenuItem.setOnAction( event -> {
            if ( pane.getItems().size() < 2 ) {
                return; // we can't maximize anything if there isn't more than 1 pane
            }
            getFocusedView().ifPresent( wrapper -> {
                var previousTask = previousDividerSettingTask.get();
                if ( previousTask != null ) previousTask.cancel();
                previousDividerSettingTask.set( taskRunner.repeat( pane.getItems().size() - 1, Duration.ofMillis( 150 ),
                        () -> Platform.runLater( () -> {
                            int topDividerIndex = pane.getItems().indexOf( wrapper ) - 1;
                            for ( int i = 0; i <= topDividerIndex; i++ ) {
                                log.debug( "Setting divider [{}] to {}", i, 0.0 );
                                pane.setDividerPosition( i, 0.0 );
                            }

                            for ( int i = pane.getItems().size() - 2; i > topDividerIndex; i-- ) {
                                log.debug( "Setting divider [{}] to {}", i, 1.0 );
                                pane.setDividerPosition( i, 1.0 );
                            }
                        } ) ) );
            } );
        } );

        MenuItem goToDateMenuItem = new MenuItem( "To date-time" );
        goToDateMenuItem.setAccelerator( new KeyCodeCombination( KeyCode.G, KeyCombination.SHORTCUT_DOWN ) );
        goToDateMenuItem.setOnAction( event -> {
            Optional<LogViewWrapper> wrapper = getFocusedView();
            if ( wrapper.isPresent() ) {
                wrapper.get().toDateTime();
            } else {
                new GoToDateView( null, this::getAllLogViews ).show();
            }
        } );

        MenuItem changeHighlightGroup = new MenuItem( "Select highlight group" );
        changeHighlightGroup.setAccelerator( new KeyCodeCombination( KeyCode.J, KeyCombination.SHORTCUT_DOWN ) );
        changeHighlightGroup.setOnAction( event -> {
            Optional<LogViewWrapper> wrapper = getFocusedView();
            if ( wrapper.isPresent() ) {
                wrapper.get().showHighlightGroupSelector();
            } else {
                Dialog.showMessage( "No file pane selected", Dialog.MessageLevel.INFO );
            }
        } );

        MenuItem toTopMenuItem = new MenuItem( "To top of file" );
        toTopMenuItem.setAccelerator( new KeyCodeCombination( KeyCode.T,
                KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN ) );
        toTopMenuItem.setOnAction( event -> getFocusedView().ifPresent( LogViewWrapper::toTop ) );

        MenuItem pageUpMenuItem = new MenuItem( "Page Up" );
        pageUpMenuItem.setAccelerator( new KeyCodeCombination( KeyCode.U, KeyCombination.SHORTCUT_DOWN ) );
        pageUpMenuItem.setOnAction( event -> getFocusedView().ifPresent( LogViewWrapper::pageUp ) );

        MenuItem pageDownMenuItem = new MenuItem( "Page Down" );
        pageDownMenuItem.setAccelerator( new KeyCodeCombination( KeyCode.D, KeyCombination.SHORTCUT_DOWN ) );
        pageDownMenuItem.setOnAction( event -> getFocusedView().ifPresent( LogViewWrapper::pageDown ) );

        MenuItem tailMenuItem = new MenuItem( "Tail file (on/off)" );
        tailMenuItem.setAccelerator( new KeyCodeCombination( KeyCode.T, KeyCombination.SHORTCUT_DOWN ) );
        tailMenuItem.setOnAction( event -> getFocusedView()
                .ifPresent( LogViewWrapper::switchTailFile ) );

        MenuItem timeGapMenuItem = new MenuItem( "Display time gaps (on/off)" );
        timeGapMenuItem.setAccelerator( new KeyCodeCombination( KeyCode.G, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN ) );
        timeGapMenuItem.setOnAction( event -> getFocusedView().ifPresent( LogViewWrapper::switchTimeGap ) );

        pane.setContextMenu( new ContextMenu(
                copyMenuItem,
                selectAllMenuItem,
                new SeparatorMenuItem(),
                toTopMenuItem, tailMenuItem, pageUpMenuItem, pageDownMenuItem, goToDateMenuItem, changeHighlightGroup,
                new SeparatorMenuItem(),
                pauseMenuItem, timeGapMenuItem,
                new SeparatorMenuItem(),
                minimizeMenuItem, maximizeMenuItem, closeMenuItem ) );

        // aggregate any change in the position of number of dividers into a single listener
        InvalidationListener dividersListener = ( event ) ->
                panesDividersObservable.setValue( !panesDividersObservable.getValue() );

        pane.getDividers().addListener( dividersListener );
        pane.getDividers().addListener( ( ListChangeListener<? super SplitPane.Divider> ) change -> {
            if ( change.next() ) {
                if ( change.wasAdded() ) {
                    change.getAddedSubList().forEach( divider ->
                            divider.positionProperty().addListener( dividersListener ) );
                } else if ( change.wasRemoved() ) {
                    change.getRemoved().forEach( divider ->
                            divider.positionProperty().removeListener( dividersListener ) );
                }
            }
        } );

        pane.getItems().addListener( ( InvalidationListener ) ( event ) -> {
            if ( pane.getItems().isEmpty() ) {
                pane.getItems().add( startUpViewGetter.get() );
            }
        } );

        pane.setOnKeyPressed( event -> {
            if ( event.isControlDown() ) {
                switch ( event.getCode() ) {
                    case N -> getFocusedView().ifPresent( view -> {
                        var handler = view.getLogView().getSelectionHandler();
                        handler.getNextItem().thenAccept( handler::select );
                    } );
                    case P -> getFocusedView().ifPresent( view -> {
                        var handler = view.getLogView().getSelectionHandler();
                        handler.getPreviousItem().thenAccept( handler::select );
                    } );
                }
            }
        } );

        if ( showEmptyPanel ) {
            pane.getItems().add( startUpViewGetter.get() );
        }
    }

    public void selectAllLogViewRows() {
        getFocusedView().ifPresent( view -> {
            var handler = view.getLogView().getSelectionHandler();
            view.getLogView().getSelectableEnds().ifPresent( ends ->
                    handler.selectAllBetween( ends.getKey(), ends.getValue() ) );
        } );
    }

    public ObjectProperty<Orientation> orientationProperty() {
        return pane.orientationProperty();
    }

    public void showContextMenu() {
        Optional.ofNullable( pane.getContextMenu() )
                .ifPresent( menu -> menu.show( pane.getScene().getWindow() ) );
    }

    @MustCallOnJavaFXThread
    public void switchOrientation() {
        if ( pane.getOrientation().equals( Orientation.VERTICAL ) ) {
            pane.setOrientation( Orientation.HORIZONTAL );
        } else {
            pane.setOrientation( Orientation.VERTICAL );
        }
    }

    @MustCallOnJavaFXThread
    public DoubleProperty prefHeightProperty() {
        return pane.prefHeightProperty();
    }

    @MustCallOnJavaFXThread
    public Node getNode() {
        return pane;
    }

    public int indexOf( LogView view ) {
        int index = 0;
        boolean found = false;
        for ( Node item : pane.getItems() ) {
            if ( item instanceof LogViewWrapper ) {
                if ( ( ( LogViewWrapper ) item ).logView == view ) {
                    found = true;
                    break;
                }
            }
            index++;
        }

        if ( found ) {
            return index;
        } else {
            return -1;
        }
    }

    @MustCallOnJavaFXThread
    public boolean contains( File file ) {
        return getAllLogViews().stream()
                .anyMatch( v -> v.getLogView().getLogFile().file.equals( file ) );
    }

    @MustCallOnJavaFXThread
    public void add( LogView logView, Runnable onCloseFile, int index, Consumer<LogView> editGroupForFile ) {
        LogViewWrapper logViewWrapper = new LogViewWrapper( logView, highlightGroups,
                this::getAllLogViews, ( wrapper ) -> {
            try {
                pane.getItems().remove( wrapper );
            } finally {
                onCloseFile.run();
            }
        }, editGroupForFile );

        logView.setScrollToLineFunction( logViewWrapper::scrollTo );

        if ( pane.getItems().size() == 1 &&
                pane.getItems().get( 0 ) instanceof StartUpView ) {
            pane.getItems().set( 0, logViewWrapper );
        } else {
            if ( index < 0 || index >= pane.getItems().size() ) {
                pane.getItems().add( logViewWrapper );
            } else {
                pane.getItems().add( index, logViewWrapper );
            }
        }
    }

    @MustCallOnJavaFXThread
    public void remove( LogFile logFile ) {
        getAllLogViews().stream()
                .filter( v -> v.getLogView().getLogFile() == logFile )
                .findFirst()
                .ifPresent( LogViewWrapper::closeView );
    }

    @MustCallOnJavaFXThread
    private List<LogViewWrapper> getAllLogViews() {
        return pane.getItems().stream()
                .filter( it -> it instanceof LogViewWrapper )
                .map( LogViewWrapper.class::cast )
                .collect( toList() );
    }

    @MustCallOnJavaFXThread
    private Optional<LogViewWrapper> getFocusedView() {
        Node node = pane.getScene().focusOwnerProperty().get();

        // go up in the hierarchy until we find a wrapper, or just hit null
        while ( node != null && !( node instanceof LogViewWrapper ) ) {
            node = node.getParent();
        }

        if ( node != null ) {
            return Optional.of( ( LogViewWrapper ) node );
        } else {
            return Optional.empty();
        }
    }

    @MustCallOnJavaFXThread
    public void close() {
        for ( int i = 0; i < pane.getItems().size(); i++ ) {
            Node item = pane.getItems().get( i );
            if ( item instanceof LogViewWrapper wrapper ) {
                wrapper.stop();
            }
        }
    }

    @MustCallOnJavaFXThread
    public void focusOn( File file ) {
        for ( Node item : pane.getItems() ) {
            if ( item instanceof LogViewWrapper ) {
                if ( ( ( LogViewWrapper ) item ).logView.getFile().equals( file ) ) {
                    item.requestFocus();
                    break;
                }
            }
        }
    }

    public ObjectProperty<?> panesDividersProperty() {
        return panesDividersObservable;
    }

    @MustCallOnJavaFXThread
    public void setDividerPositions( List<Double> separators ) {
        double[] positions = separators.stream().mapToDouble( i -> i ).toArray();

        if ( log.isDebugEnabled() ) {
            log.debug( "Number of dividers: {}, positions: {}", positions.length,
                    Arrays.stream( positions )
                            .mapToObj( d -> String.format( "%.2f", d ) )
                            .collect( joining( ", " ) ) );
        }

        var previousTask = previousDividerSettingTask.get();
        if ( previousTask != null ) previousTask.cancel();
        previousDividerSettingTask.set( taskRunner.repeat( positions.length, Duration.ofMillis( 150 ),
                () -> Platform.runLater( () -> {
                    for ( int i = 0; i < positions.length; i++ ) {
                        if ( i < pane.getDividers().size() ) {
                            log.debug( "Setting divider [{}] to {}", i, positions[ i ] );
                            pane.setDividerPosition( i, positions[ i ] );
                        } else {
                            log.debug( "Ignoring divider position as it is beyond panes count" );
                        }
                    }
                } ) ) );
    }

    @MustCallOnJavaFXThread
    public List<Double> getSeparatorsPositions() {
        return DoubleStream.of( pane.getDividerPositions() )
                .boxed().collect( toList() );
    }

    @MustCallOnJavaFXThread
    public ScrollPane getScrollPaneFor( LogView view ) {
        for ( Node item : pane.getItems() ) {
            if ( item instanceof LogViewWrapper wrapper ) {
                if ( wrapper.getLogView() == view ) {
                    return wrapper.getScrollPane();
                }
            }
        }

        return null;
    }

    @MustCallOnJavaFXThread
    public void dividePanesEvenly() {
        if ( pane.getItems().size() < 2 ) {
            return; // nothing to do if there isn't more than 1 pane
        }

        final int dividerCount = pane.getItems().size() - 1;
        final double positionStep = 1.0 / ( dividerCount + 1 );
        setDividerPositions( IntStream.range( 0, dividerCount )
                .mapToObj( i -> ( i + 1 ) * positionStep )
                .collect( toList() ) );
    }

    private static class LogViewScrollPane extends ScrollPane {

        LogViewScrollPane( LogViewWrapper wrapper ) {
            super( wrapper.logView );
            setFocusTraversable( true );

            wrapper.header.tailFileProperty().addListener( observable -> {
                if ( wrapper.header.tailFileProperty().get() ) {
                    setVvalue( 1.0 );
                }
            } );

            setOnScroll( event -> {
                double deltaY = event.getDeltaY();
                if ( deltaY > 0.0 ) {
                    wrapper.stopTailingFile(); // stop tailing when user scrolls up
                } else if ( wrapper.isTailingFile() ) {
                    return; // no need to scroll down when tailing file
                }
                wrapper.logView.move( deltaY, factor -> Platform.runLater( () -> {
                    if ( deltaY > 0 ) {
                        setVvalue( factor );
                    } else {
                        setVvalue( 1.0 - factor );
                    }
                } ) );
            } );
        }

    }

    static class LogViewWrapper extends VBox {

        private static final Logger log = LoggerFactory.getLogger( LogViewWrapper.class );

        private final LogView logView;
        private final Consumer<LogViewWrapper> onCloseFile;
        private final LogViewHeader header;
        private final LogViewScrollPane scrollPane;
        private final Supplier<List<LogViewWrapper>> logViewsGetter;

        @MustCallOnJavaFXThread
        LogViewWrapper( LogView logView,
                        HighlightGroups highlightGroups,
                        Supplier<List<LogViewWrapper>> logViewsGetter,
                        Consumer<LogViewWrapper> onCloseFile,
                        Consumer<LogView> editGroupForLogFile ) {
            super( 2.0 );

            this.logView = logView;
            this.onCloseFile = onCloseFile;
            this.logViewsGetter = logViewsGetter;

            this.header = new LogViewHeader( logView, this::closeView, this::toDateTime,
                    highlightGroups, editGroupForLogFile );
            this.scrollPane = new LogViewScrollPane( this );

            getChildren().setAll( header, scrollPane );

            header.tailFileProperty().bindBidirectional( logView.tailingFileProperty() );
            logView.allowRefreshProperty().bind( header.pauseRefreshProperty().not() );

            header.timeGapProperty().bindBidirectional( logView.timeGapProperty() );

            logView.setOnFileExists( ( fileExists ) -> {
                if ( fileExists ) {
                    if ( !isShowingFileContents() ) {
                        log.debug( "Showing FileContents pane for file: {}", logView.getFile() );
                        Platform.runLater( () -> getChildren().setAll( header, scrollPane ) );
                    }
                } else {
                    if ( isShowingFileContents() ) {
                        log.debug( "Showing FileDoesNotExist pane for file: {}", logView.getFile() );
                        Platform.runLater( () -> getChildren().setAll( header, new FileDoesNotExistPane() ) );
                    }
                }
            } );

            logView.loadFileContents();
        }

        LogView getLogView() {
            return logView;
        }

        private LogViewScrollPane getScrollPane() {
            return scrollPane;
        }

        private boolean isShowingFileContents() {
            return getChildren().size() > 1 && getChildren().get( 1 ) instanceof LogViewScrollPane;
        }

        @MustCallOnJavaFXThread
        void showHighlightGroupSelector() {
            header.getGroupSelector().requestFocus();
            header.getGroupSelector().show();
        }

        @MustCallOnJavaFXThread
        void toDateTime() {
            stopTailingFile();
            GoToDateView goToView = new GoToDateView( logView, logViewsGetter );
            goToView.show();
        }

        @MustCallOnJavaFXThread
        void toTop() {
            stopTailingFile();
            logView.toTop();
            scrollPane.setVvalue( 0.0 );
        }

        @MustCallOnJavaFXThread
        void pageUp() {
            stopTailingFile();
            logView.pageUp();
        }

        @MustCallOnJavaFXThread
        void pageDown() {
            if ( !isTailingFile() ) {
                logView.pageDown();
            }
        }

        @MustCallOnJavaFXThread
        private void startTailingFile() {
            if ( !isTailingFile() ) {
                log.debug( "Starting tailing file" );
                header.tailFileProperty().setValue( true );
            }
        }

        @MustCallOnJavaFXThread
        private void stopTailingFile() {
            if ( isTailingFile() ) {
                log.debug( "Stopping tailing file" );
                header.tailFileProperty().setValue( false );
            }
        }

        private boolean isTimeGapDisplayed() {
            return header.timeGapProperty().get();
        }

        private void setDisplayTimeGap( boolean displayTimeGap ) {
            log.debug( "Display time gap: {}", displayTimeGap );
            header.timeGapProperty().setValue( displayTimeGap );
        }

        @MustCallOnJavaFXThread
        boolean isTailingFile() {
            return header.tailFileProperty().get();
        }

        @MustCallOnJavaFXThread
        void closeView() {
            try {
                logView.closeFileReader();
            } finally {
                onCloseFile.accept( this );
            }
        }

        @MustCallOnJavaFXThread
        void stop() {
            header.dispose();

            // do not call onClose as this is not closing the view, just stopping the app
            logView.closeFileReader();
        }

        @MustCallOnJavaFXThread
        void switchTailFile() {
            if ( isTailingFile() ) {
                stopTailingFile();
            } else {
                startTailingFile();
            }
        }

        @MustCallOnJavaFXThread
        void switchTimeGap() {
            final var doDisplay = !isTimeGapDisplayed();
            Runnable setter = () -> setDisplayTimeGap( doDisplay );
            if ( doDisplay ) {
                new TimeGapEditor( logView, setter ).show();
            } else {
                setter.run();
            }
        }

        @MustCallOnJavaFXThread
        void scrollTo( int lineNumber ) {
            double vvalue = ( double ) lineNumber / ( MAX_LINES - 1 );
            log.debug( "Setting scroll to {} for line number {}", vvalue, lineNumber );
            scrollPane.setVvalue( vvalue );
        }

    }

    private static class LogViewHeader extends BorderPane {

        private final BooleanProperty tailFile;
        private final BooleanProperty timeGap;
        private final BooleanProperty pauseRefresh;
        private final HighlightGroupSelector groupSelector;

        LogViewHeader( LogView logView, Runnable closeLogView, Runnable goToDateTime,
                       HighlightGroups groups, Consumer<LogView> editGroupForLogFile ) {
            groupSelector = new HighlightGroupSelector( groups, logView );
            groupSelector.setTooltip( new Tooltip( "Select highlight rules group for this file" ) );

            setMinWidth( 10.0 );

            File file = logView.getFile();

            HBox leftAlignedBox = new HBox( 2.0 );
            HBox rightAlignedBox = new HBox( 2.0 );

            Button fileNameLabel = new Button();
            fileNameLabel.setMinWidth( 5.0 );
            fileNameLabel.setTooltip( new Tooltip( file.getAbsolutePath() ) );

            Runnable updateFileLabel = () -> {
                if ( file.exists() ) {
                    final double fileLength = ( double ) file.length();
                    final String fileSizeText;
                    if ( fileLength < 10_000 ) {
                        fileSizeText = String.format( "(%.0f bytes)", fileLength );
                    } else {
                        double fileSize = fileLength / 1_000_000.0;
                        fileSizeText = String.format( "(%.2f MB)", fileSize );
                    }

                    Platform.runLater( () ->
                            fileNameLabel.setText( file.getName() + " " + fileSizeText ) );
                } else {
                    Platform.runLater( () -> fileNameLabel.setText( file.getName() ) );
                }
            };

            logView.onFileUpdate( updateFileLabel );
            updateFileLabel.run();

            leftAlignedBox.getChildren().add( fileNameLabel );

            Button highlightRulesButton = AwesomeIcons.createIconButton( AwesomeIcons.LIST_UL );
            highlightRulesButton.setTooltip( new Tooltip( "Edit highlight rules for this file" ) );
            highlightRulesButton.setOnAction( event -> editGroupForLogFile.accept( logView ) );

            Button goToDateButton = AwesomeIcons.createIconButton( AwesomeIcons.CLOCK );
            goToDateButton.setTooltip( new Tooltip( "Go to date-time" ) );
            goToDateButton.setOnAction( event -> goToDateTime.run() );

            ToggleButton tailFileButton = AwesomeIcons.createToggleButton( AwesomeIcons.ARROW_DOWN );
            tailFileButton.setTooltip( new Tooltip( "Tail file" ) );
            this.tailFile = tailFileButton.selectedProperty();
            this.tailFile.addListener( event -> {
                if ( tailFile.get() ) {
                    pauseRefreshProperty().set( false );
                }
            } );

            ToggleButton timeGapButton = AwesomeIcons.createToggleButton( AwesomeIcons.GAP );
            this.timeGap = timeGapButton.selectedProperty();
            timeGapButton.setTooltip( new Tooltip( "Display time gaps" ) );
            timeGapButton.setOnAction( event -> timeGap.set( !timeGap.getValue() ) );

            Button closeButton = AwesomeIcons.createIconButton( AwesomeIcons.CLOSE );
            closeButton.setTooltip( new Tooltip( "Close file" ) );
            closeButton.setOnAction( event -> closeLogView.run() );

            ToggleButton pauseRefreshButton = AwesomeIcons.createToggleButton( AwesomeIcons.PAUSE );
            pauseRefreshButton.setTooltip( new Tooltip( "Pause file auto-refresh.\n" +
                    "Disables moving up/down and all file reads." ) );
            this.pauseRefresh = pauseRefreshButton.selectedProperty();
            this.pauseRefresh.addListener( event -> {
                if ( pauseRefresh.get() ) {
                    tailFile.set( false );
                }
            } );

            rightAlignedBox.getChildren().addAll( highlightRulesButton, goToDateButton,
                    timeGapButton, tailFileButton, pauseRefreshButton, closeButton );

            setLeft( leftAlignedBox );
            setCenter( groupSelector );
            setRight( rightAlignedBox );
        }

        HighlightGroupSelector getGroupSelector() {
            return groupSelector;
        }

        BooleanProperty tailFileProperty() {
            return tailFile;
        }

        BooleanProperty timeGapProperty() {
            return timeGap;
        }

        BooleanProperty pauseRefreshProperty() {
            return pauseRefresh;
        }

        @MustCallOnJavaFXThread
        void togglePauseRefresh() {
            pauseRefresh.set( !pauseRefresh.getValue() );
        }

        public void dispose() {
            groupSelector.dispose();
        }
    }

    private static class FileDoesNotExistPane extends StackPane {

        FileDoesNotExistPane() {
            setPrefSize( 800, 200 );
            setMinSize( 10.0, 10.0 );
            setFocusTraversable( true );

            Text text = new Text( "File does not exist" );
            text.getStyleClass().add( "large-background-text" );
            getChildren().add( text );

            addEventHandler( MouseEvent.MOUSE_CLICKED, event -> requestFocus() );

            focusedProperty().addListener( observable -> {
                if ( isFocused() ) {
                    getStyleClass().add( "selected-pane" );
                } else {
                    getStyleClass().remove( "selected-pane" );
                }
            } );
        }

    }

}
