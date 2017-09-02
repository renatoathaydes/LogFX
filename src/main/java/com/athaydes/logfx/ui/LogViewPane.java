package com.athaydes.logfx.ui;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

/**
 * A container of {@link LogView}s.
 */
public final class LogViewPane {

    private final SplitPane pane = new SplitPane();

    // a simple observable that changes state every time a change occurs in a pane divider
    // (changes in number of dividers as well as in their positions)
    private final ObjectProperty<Boolean> panesDividersObservable = new SimpleObjectProperty<>( false );

    @MustCallOnJavaFXThread
    public LogViewPane() {
        MenuItem copyMenuItem = new MenuItem( "Copy Selection" );
        copyMenuItem.setAccelerator( new KeyCodeCombination( KeyCode.C, KeyCombination.META_DOWN ) );
        copyMenuItem.setOnAction( event -> {
            getFocusedView().ifPresent( wrapper ->
                    wrapper.logView.getSelection().ifPresent( content -> {
                        Clipboard.getSystemClipboard().setContent( content );
                    } ) );
        } );

        MenuItem closeMenuItem = new MenuItem( "Close" );
        closeMenuItem.setAccelerator( new KeyCodeCombination( KeyCode.W, KeyCombination.META_DOWN ) );
        closeMenuItem.setOnAction( ( event ) ->
                getFocusedView().ifPresent( LogViewWrapper::closeView ) );

        MenuItem hideMenuItem = new MenuItem( "Hide" );
        hideMenuItem.setAccelerator( new KeyCodeCombination( KeyCode.H,
                KeyCombination.META_DOWN, KeyCombination.SHIFT_DOWN ) );
        hideMenuItem.setOnAction( event -> {
            if ( pane.getItems().size() < 2 ) {
                return; // we can't hide anything if there isn't more than 1 pane
            }
            getFocusedView().ifPresent( wrapper -> {
                int index = pane.getItems().indexOf( wrapper );
                if ( index == pane.getItems().size() - 1 ) {
                    // last pane, so we can only hide by opening up the previous one
                    pane.setDividerPosition( index - 1, 1.0 );
                } else if ( index >= 0 ) {
                    // in all other cases, just hide the pane itself
                    pane.setDividerPosition( index, 0.0 );
                }
            } );
        } );

        MenuItem tailMenuItem = new MenuItem( "Tail file (on/off)" );
        tailMenuItem.setAccelerator( new KeyCodeCombination( KeyCode.T, KeyCombination.META_DOWN ) );
        tailMenuItem.setOnAction( event -> getFocusedView()
                .ifPresent( LogViewWrapper::switchTailFile ) );

        MenuItem separator = new SeparatorMenuItem();

        pane.setContextMenu( new ContextMenu(
                copyMenuItem, separator,
                closeMenuItem, hideMenuItem, tailMenuItem ) );

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
    }

    public ObjectProperty<Orientation> orientationProperty() {
        return pane.orientationProperty();
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

    @MustCallOnJavaFXThread
    public void add( LogView logView, Runnable onCloseFile ) {
        pane.getItems().add( new LogViewWrapper( logView, ( wrapper ) -> {
            pane.getItems().remove( wrapper );
            onCloseFile.run();
        } ) );
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
    private void hide() {
        if ( pane.getItems().size() > 1 ) {
            pane.setDividerPosition( 0, 0.0 );
        }
    }

    @MustCallOnJavaFXThread
    public void close() {
        for ( int i = 0; i < pane.getItems().size(); i++ ) {
            LogViewWrapper wrapper = ( LogViewWrapper ) pane.getItems().get( i );
            wrapper.stop();
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

        // set divider positions from last to first, then first to last, to try to honour all of them
        for ( int i = positions.length - 1; i >= 0; i-- ) {
            pane.setDividerPosition( i, positions[ i ] );
        }
        Platform.runLater( () -> {
            for ( int i = 0; i < positions.length; i++ ) {
                pane.setDividerPosition( i, positions[ i ] );
            }
        } );
    }

    @MustCallOnJavaFXThread
    public List<Double> getSeparatorsPositions() {
        return DoubleStream.of( pane.getDividerPositions() )
                .boxed().collect( Collectors.toList() );
    }

    private static class LogViewScrollPane extends ScrollPane {
        private final LogViewWrapper wrapper;

        LogViewScrollPane( LogViewWrapper wrapper ) {
            super( wrapper.logView );
            setFocusTraversable( true );

            this.wrapper = wrapper;

            setOnScroll( event -> {
                double deltaY = event.getDeltaY();
                if ( deltaY > 0.0 ) {
                    wrapper.stopTailingFile(); // stop tailing when user scrolls up
                } else if ( wrapper.isTailingFile() ) {
                    return; // no need to scroll down when tailing file
                }

                switch ( event.getTextDeltaYUnits() ) {
                    case NONE:
                        // no change
                        break;
                    case LINES:
                        deltaY *= 10.0;
                        break;
                    case PAGES:
                        deltaY *= 50.0;
                        break;
                }
                wrapper.logView.move( deltaY );
            } );
        }

    }

    private static class LogViewWrapper extends VBox {

        private static final Logger log = LoggerFactory.getLogger( LogViewWrapper.class );

        private final LogView logView;
        private final Consumer<LogViewWrapper> onClose;
        private final LogViewHeader header;
        private final LogViewScrollPane scrollPane;

        @MustCallOnJavaFXThread
        LogViewWrapper( LogView logView,
                        Consumer<LogViewWrapper> onClose ) {
            super( 2.0 );

            this.logView = logView;
            this.onClose = onClose;

            this.header = new LogViewHeader( logView.getFile(),
                    () -> onClose.accept( this ) );

            this.scrollPane = new LogViewScrollPane( this );

            getChildren().setAll( header, scrollPane );

            header.tailFileProperty().addListener( ( observable, wasSelected, isSelected ) -> {
                if ( isSelected ) {
                    startTailingFile();
                } else {
                    stopTailingFile();
                }
            } );

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

        private boolean isShowingFileContents() {
            return getChildren().size() > 1 && getChildren().get( 1 ) instanceof LogViewScrollPane;
        }

        @MustCallOnJavaFXThread
        private void startTailingFile() {
            if ( !logView.isTailingFile() ) {
                log.debug( "Starting tailing file" );
                header.tailFileProperty().setValue( true );
                logView.startTailingFile();
                scrollPane.setVvalue( 1.0 );
            }
        }

        @MustCallOnJavaFXThread
        private void stopTailingFile() {
            if ( logView.isTailingFile() ) {
                log.debug( "Stopping tailing file" );
                header.tailFileProperty().setValue( false );
                logView.stopTailingFile();
            }
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
                onClose.accept( this );
            }
        }

        @MustCallOnJavaFXThread
        void stop() {
            // do not call onClose as this is not closing the view, just stopping the app
            logView.closeFileReader();
        }

        void switchTailFile() {
            if ( isTailingFile() ) {
                stopTailingFile();
            } else {
                startTailingFile();
            }
        }
    }

    private static class LogViewHeader extends BorderPane {

        private final BooleanProperty tailFile;

        LogViewHeader( File file, Runnable onClose ) {
            setMinWidth( 10.0 );

            HBox leftAlignedBox = new HBox( 2.0 );
            HBox rightAlignedBox = new HBox( 2.0 );

            Button fileNameLabel = new Button( file.getName() );
            fileNameLabel.setMinWidth( 5.0 );
            fileNameLabel.setTooltip( new Tooltip( file.getAbsolutePath() ) );
            leftAlignedBox.getChildren().add( fileNameLabel );

            if ( file.exists() ) {
                double fileLength = ( double ) file.length();
                String fileSizeText;
                if ( fileLength < 10_000 ) {
                    fileSizeText = String.format( "(%.0f bytes)", fileLength );
                } else {
                    double fileSize = fileLength / 1_000_000.0;
                    fileSizeText = String.format( "(%.2f MB)", fileSize );
                }

                fileNameLabel.setText( fileNameLabel.getText() + " " + fileSizeText );
            }

            ToggleButton tailFileButton = AwesomeIcons.createToggleButton( AwesomeIcons.ARROW_DOWN );
            tailFileButton.setTooltip( new Tooltip( "Tail file" ) );
            this.tailFile = tailFileButton.selectedProperty();

            Button closeButton = AwesomeIcons.createIconButton( AwesomeIcons.CLOSE );
            closeButton.setTooltip( new Tooltip( "Close file" ) );
            closeButton.setOnAction( event -> onClose.run() );

            rightAlignedBox.getChildren().addAll( tailFileButton, closeButton );

            setLeft( leftAlignedBox );
            setRight( rightAlignedBox );
        }

        BooleanProperty tailFileProperty() {
            return tailFile;
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

            addEventHandler( MouseEvent.MOUSE_CLICKED, event -> {
                requestFocus();
            } );

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
