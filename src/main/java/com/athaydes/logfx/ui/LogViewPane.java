package com.athaydes.logfx.ui;

import javafx.beans.property.DoubleProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;

/**
 * A container of {@link LogView}s.
 */
public final class LogViewPane {

    private final SplitPane pane = new SplitPane();

    public LogViewPane( LogView logView, String fileName ) {
        add( logView, fileName );
    }

    public DoubleProperty prefHeightProperty() {
        return pane.prefHeightProperty();
    }

    public Node getNode() {
        return pane;
    }

    public void add( LogView logView, String fileName ) {
        pane.getItems().add( new LogViewWrapper( logView, fileName ) );
    }

    public LogView get( int index ) {
        LogViewWrapper wrapper = ( LogViewWrapper ) pane.getItems().get( index );
        return wrapper.getLogView();
    }

    private static class LogViewWrapper extends ScrollPane {

        private final LogView logView;

        LogViewWrapper( LogView logView, String fileName ) {
            super( new VBox( 2.0, new Label( fileName ), logView ) );
            this.logView = logView;
        }

        LogView getLogView() {
            return logView;
        }
    }
}
