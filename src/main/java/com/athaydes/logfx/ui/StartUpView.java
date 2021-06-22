package com.athaydes.logfx.ui;

import com.athaydes.logfx.data.LogFile;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.File;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * The startup view.
 * <p>
 * Shown when no log files are open.
 */
public class StartUpView extends StackPane {

    public StartUpView( Stage stage,
                        Collection<LogFile> logFiles,
                        Consumer<File> openFile ) {
        VBox box = new AboutLogFXView().createNode();

        String metaKey = FxUtils.isMac() ? "⌘" : "Ctrl+";

        Hyperlink link = new Hyperlink( String.format( "Open file (%sO)", metaKey ) );
        link.getStyleClass().add( "large-background-text" );
        link.setOnAction( ( event ) -> FileOpener.run( stage, logFiles, openFile ) );

        Text dropText = new Text( "Or drop files here" );
        dropText.getStyleClass().add( "large-background-text" );

        StackPane fileDropPane = new StackPane( dropText );
        fileDropPane.getStyleClass().add( "drop-file-pane" );

        FileDragAndDrop.install( fileDropPane, openFile );

        box.getChildren().addAll( link, fileDropPane );
        getChildren().addAll( box );
    }

}
