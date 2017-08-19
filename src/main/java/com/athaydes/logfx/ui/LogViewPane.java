package com.athaydes.logfx.ui;

import com.athaydes.logfx.file.FileReader;
import javafx.beans.property.DoubleProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;

import java.io.Closeable;

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

    private static class LogViewWrapper extends ScrollPane {

        private final LogView logView;
        private final FileReader fileReader;

        LogViewWrapper( LogView logView, FileReader fileReader ) {
            super( new VBox( 2.0, new Label( fileReader.getName() ), logView ) );
            this.logView = logView;
            this.fileReader = fileReader;
        }

        void close() {
            fileReader.stop();
        }
    }
}
