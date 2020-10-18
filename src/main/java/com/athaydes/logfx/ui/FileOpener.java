package com.athaydes.logfx.ui;

import com.athaydes.logfx.data.LogFile;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * Operating System File opener Dialog.
 */
public class FileOpener {

    public static final int MAX_OPEN_FILES = 10;

    private static final Logger log = LoggerFactory.getLogger( FileOpener.class );

    public static void run( Stage stage,
                            Collection<LogFile> logFiles,
                            Consumer<File> onFileSelected ) {
        if ( logFiles.size() >= MAX_OPEN_FILES ) {
            log.warn( "Cannot open file, too many open files" );
            Dialog.showMessage( "Too many open files already!\n" +
                            "Close a file or more to make room for other files.",
                    Dialog.MessageLevel.WARNING );
            return;
        }
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
