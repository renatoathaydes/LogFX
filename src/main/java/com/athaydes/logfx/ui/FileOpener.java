package com.athaydes.logfx.ui;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.function.Consumer;

/**
 * Operating System File opener Dialog.
 */
public class FileOpener {

    private static final Logger log = LoggerFactory.getLogger( FileOpener.class );

    public FileOpener( Stage stage, Consumer<File> onFileSelected ) {
        log.debug( "Opening file" );
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle( "Select a file" );
        File file = fileChooser.showOpenDialog( stage );
        log.debug( "Selected file {}", file );
        if ( file != null ) {
            onFileSelected.accept( file );
        }
    }

}
