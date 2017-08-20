package com.athaydes.logfx.ui;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.function.Consumer;

/**
 * A container of {@link LogView}s.
 */
public final class LogViewPane {

    private final SplitPane pane = new SplitPane();

    public LogViewPane() {
        MenuItem closeMenuItem = new MenuItem( "Close" );
        closeMenuItem.setOnAction( ( event ) -> {
            Node focusedNode = pane.getScene().focusOwnerProperty().get();
            if ( focusedNode instanceof LogViewScrollPane ) {
                ( ( LogViewScrollPane ) focusedNode ).closeView();
            }
        } );

        MenuItem hideMenuItem = new MenuItem( "Hide" );
        hideMenuItem.setOnAction( event -> {
            // TODO hide pane properly
            hide();
        } );

        pane.setContextMenu( new ContextMenu( closeMenuItem, hideMenuItem ) );
    }

    public DoubleProperty prefHeightProperty() {
        return pane.prefHeightProperty();
    }

    public Node getNode() {
        return pane;
    }

    public void add( LogView logView, Runnable onCloseFile ) {
        pane.getItems().add( new LogViewWrapper( logView, ( wrapper ) -> {
            pane.getItems().remove( wrapper );
            onCloseFile.run();
        } ) );
    }

    private void hide() {
        if ( pane.getItems().size() > 1 ) {
            pane.setDividerPosition( 0, 0.0 );
        }
    }

    public void close() {
        for ( int i = 0; i < pane.getItems().size(); i++ ) {
            LogViewWrapper wrapper = ( LogViewWrapper ) pane.getItems().get( i );
            wrapper.stop();
        }
    }

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
        }

        void closeView() {
            wrapper.closeView();
        }
    }

    private static class LogViewWrapper extends VBox {

        private final LogView logView;
        private final Consumer<LogViewWrapper> onClose;

        LogViewWrapper( LogView logView,
                        Consumer<LogViewWrapper> onClose ) {
            super( 2.0 );

            this.logView = logView;
            this.onClose = onClose;

            getChildren().addAll(
                    new LogViewHeader( logView.getFile(), () -> onClose.accept( this ) ),
                    new LogViewScrollPane( this ) );

            Platform.runLater( () -> {
                // TODO read file
            } );
        }

        void closeView() {
            try {
                logView.closeFileReader();
            } finally {
                onClose.accept( this );
            }
        }

        void stop() {
            // do not call onClose as this is not closing the view, just stopping the app
            logView.closeFileReader();
        }
    }

    private static class LogViewHeader extends BorderPane {

        LogViewHeader( File file, Runnable onClose ) {
            HBox leftAlignedBox = new HBox( 2.0 );
            HBox rightAlignedBox = new HBox( 2.0 );

            Button fileNameLabel = new Button( file.getName() );
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

            Button closeButton = new Button( "Close" );
            closeButton.setOnAction( event -> onClose.run() );

            rightAlignedBox.getChildren().add( closeButton );

            setLeft( leftAlignedBox );
            setRight( rightAlignedBox );
        }
    }

}
