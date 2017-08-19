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

    public void add( LogView logView, FileReader fileReader ) {
        pane.getItems().add( new LogViewWrapper( logView, fileReader ) );
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
                        FileReader fileReader ) {
            super( 2.0,
                    new LogViewHeader( fileReader.getFile() ),
                    new ScrollPane( logView ) );

            this.logView = logView;
            this.fileReader = fileReader;
        }

        void close() {
            fileReader.stop();
        }
    }

    private static class LogViewHeader extends BorderPane {

        LogViewHeader( File file ) {
            setBackground( LogFxButton.onBkgrd );

            HBox leftAlignedBox = new HBox( 2.0 );
            HBox rightAlignedBox = new HBox( 2.0 );

            Button fileNameLabel = LogFxButton.defaultLabel( file.getName() );
            fileNameLabel.setTooltip( new Tooltip( file.getAbsolutePath() ) );
            leftAlignedBox.getChildren().add( fileNameLabel );

            if ( file.exists() ) {
                double fileSize = ( ( double ) file.length() ) / 1_000_000.0;
                fileNameLabel.setText( fileNameLabel.getText() + " (" + fileSize + " MB)" );
            }

            rightAlignedBox.getChildren().add( LogFxButton.closeButton() );

            setLeft( leftAlignedBox );
            setRight( rightAlignedBox );
        }
    }

}
