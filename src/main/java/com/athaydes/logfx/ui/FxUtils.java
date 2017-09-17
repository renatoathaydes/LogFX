package com.athaydes.logfx.ui;

import com.athaydes.logfx.concurrency.TaskRunner;
import com.athaydes.logfx.config.Properties;
import com.athaydes.logfx.file.FileChangeWatcher;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Paint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


/**
 * Utility functions for JavaFX-related functionality.
 */
public class FxUtils {

    private static final Logger log = LoggerFactory.getLogger( FxUtils.class );
    private static final Map<Paint, Background> bkgByPaint = new HashMap<>();

    /**
     * Creates and caches a simple {@link Background} for the given {@link Paint}.
     *
     * @param paint of the background
     * @return simple background
     */
    public static Background simpleBackground( Paint paint ) {
        return bkgByPaint.computeIfAbsent( paint,
                ignored -> new Background(
                        new BackgroundFill( paint, CornerRadii.EMPTY, Insets.EMPTY ) ) );
    }

    /**
     * Setup the stylesheet for the given Scene.
     *
     * @param scene to setup stylesheet for
     */
    public static void setupStylesheet( Scene scene ) {
        String stylesheet = Properties.getCustomStylesheet()
                .map( File::getAbsolutePath )
                .map( path -> "file:" + path )
                .orElse( "css/LogFX.css" );

        String iconsStylesheet = "css/icons.css";

        Runnable resetStylesheet = () -> Platform.runLater( () -> {
            scene.getStylesheets().clear();
            scene.getStylesheets().addAll( stylesheet, iconsStylesheet );
        } );

        if ( Properties.getCustomStylesheet().isPresent() ) {
            log.info( "Using custom stylesheet for new Scene: {}", stylesheet );
            if ( Properties.isRefreshStylesheet() ) {
                FileChangeWatcher stylesheetWatcher = new FileChangeWatcher(
                        Properties.getCustomStylesheet().get(), TaskRunner.getGlobalInstance() );
                stylesheetWatcher.setOnChange( resetStylesheet );

                scene.getWindow().setOnCloseRequest( event -> stylesheetWatcher.close() );
            } else {
                resetStylesheet.run();
            }
        } else {
            log.debug( "Using default stylesheet for new Scene" );
            resetStylesheet.run();
        }

    }

}
