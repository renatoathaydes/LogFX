package com.athaydes.logfx.ui;

import com.athaydes.logfx.file.FileReader;
import javafx.beans.property.DoubleProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.Closeable;
import java.io.File;
import java.util.function.Consumer;

/**
 * A container of {@link LogView}s.
 */
public final class LogViewPane implements Closeable {

    private final SplitPane pane = new SplitPane();

    public DoubleProperty prefHeightProperty() {
        return pane.prefHeightProperty();
    }

    public Node getNode() {
        return pane;
    }

    public void add( LogView logView, FileReader fileReader, Runnable onCloseFile ) {
        pane.getItems().add( new LogViewWrapper( logView, fileReader,
                ( wrapper ) -> {
                    pane.getItems().remove( wrapper );
                    try {
                        onCloseFile.run();
                    } finally {
                        wrapper.close();
                    }
                } ) );
    }

    public void close() {
        for ( int i = 0; i < pane.getItems().size(); i++ ) {
            LogViewWrapper wrapper = ( LogViewWrapper ) pane.getItems().get( i );
            wrapper.close();
        }
    }

    private static class LogViewWrapper extends VBox {

        private final LogView logView;
        private final FileReader fileReader;

        LogViewWrapper( LogView logView,
                        FileReader fileReader,
                        Consumer<LogViewWrapper> onClose ) {
            super( 2.0 );

            this.logView = logView;
            this.fileReader = fileReader;

            getChildren().addAll(
                    new LogViewHeader( fileReader.getFile(), () -> onClose.accept( this ) ),
                    new ScrollPane( logView ) );
        }

        void close() {
            fileReader.stop();
        }
    }

    private static class LogViewHeader extends BorderPane {

        LogViewHeader( File file, Runnable onClose ) {
            HBox leftAlignedBox = new HBox( 2.0 );
            HBox rightAlignedBox = new HBox( 2.0 );

            Button fileNameLabel = new Button( file.getName() );
            fileNameLabel.setDisable( true );
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
