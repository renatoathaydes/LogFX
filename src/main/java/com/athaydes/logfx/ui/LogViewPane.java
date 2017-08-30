package com.athaydes.logfx.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A container of {@link LogView}s.
 */
public final class LogViewPane {

    private final SplitPane pane = new SplitPane();

    @MustCallOnJavaFXThread
    public LogViewPane() {
        MenuItem closeMenuItem = new MenuItem( "Close" );
        closeMenuItem.setOnAction( ( event ) ->
                getFocusedView().ifPresent( LogViewScrollPane::closeView ) );

        MenuItem hideMenuItem = new MenuItem( "Hide" );
        hideMenuItem.setOnAction( event -> {
            if ( pane.getItems().size() < 2 ) {
                return; // we can't hide anything if there isn't more than 1 pane
            }
            getFocusedView().ifPresent( scrollPane -> {
                int index = pane.getItems().indexOf( scrollPane.wrapper );
                if ( index == pane.getItems().size() - 1 ) {
                    // last pane, so we can only hide by opening up the previous one
                    pane.setDividerPosition( index - 1, 1.0 );
                } else if ( index >= 0 ) {
                    // in all other cases, just hide the pane itself
                    pane.setDividerPosition( index, 0.0 );
                }
            } );
        } );

        pane.setContextMenu( new ContextMenu( closeMenuItem, hideMenuItem ) );
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
    private Optional<LogViewScrollPane> getFocusedView() {
        Node focusedNode = pane.getScene().focusOwnerProperty().get();
        if ( focusedNode instanceof LogViewScrollPane ) {
            return Optional.of( ( LogViewScrollPane ) focusedNode );
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

    private static class LogViewScrollPane extends ScrollPane {
        private final LogViewWrapper wrapper;

        LogViewScrollPane( LogViewWrapper wrapper ) {
            super( wrapper.logView );
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

        void closeView() {
            wrapper.closeView();
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

            getChildren().addAll( header, scrollPane );

            header.tailFileProperty().addListener( ( observable, wasSelected, isSelected ) -> {
                if ( isSelected ) {
                    startTailingFile();
                } else {
                    stopTailingFile();
                }
            } );
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

}
