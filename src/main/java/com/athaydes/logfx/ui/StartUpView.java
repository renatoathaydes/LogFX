package com.athaydes.logfx.ui;

import javafx.application.HostServices;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.File;
import java.util.function.Consumer;

/**
 * The startup view.
 * <p>
 * Shown when no log files are open.
 */
public class StartUpView extends StackPane {

    public StartUpView( HostServices hostServices,
                        Stage stage,
                        Consumer<File> openFile ) {
        VBox box = new AboutLogFXView( hostServices ).createNode();

        String metaKey = FxUtils.isMac() ? "⌘" : "Ctrl+";

        Hyperlink link = new Hyperlink( String.format( "Open file (%sO)", metaKey ) );
        link.getStyleClass().add( "large-background-text" );
        link.setOnAction( ( event ) -> new FileOpener( stage, openFile ) );

        Text dropText = new Text( "Or drop files here" );
        dropText.getStyleClass().add( "large-background-text" );

        StackPane fileDropPane = new StackPane( dropText );
        fileDropPane.getStyleClass().add("drop-file-pane");

        FileDragAndDrop.install( fileDropPane, openFile );

        box.getChildren().addAll( link, fileDropPane );
        getChildren().addAll( box );
    }

}
