package com.athaydes.logfx.ui;

import com.athaydes.logfx.LogFX;
import com.athaydes.logfx.concurrency.TaskRunner;
import com.athaydes.logfx.config.Properties;
import com.athaydes.logfx.file.FileChangeWatcher;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Paint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


/**
 * Utility functions for JavaFX-related functionality.
 */
public class FxUtils {
    /**
     * String values for OSs, must match the values used for releases on GitHub
     * as these values are used to find newer releases.
     */
    private static class OperatingSystems {

        static final String LINUX = "linux";
        static final String MAC = "mac";
        static final String WINDOWS = "windows";
    }

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

    public static URL resourceUrl( String name ) {
        return LogFX.class.getResource( name );
    }

    public static String resourcePath( String name ) {
        return resourceUrl( name ).toExternalForm();
    }

    /**
     * Setup the stylesheet for the given Scene.
     *
     * @param scene to setup stylesheet for
     */
    public static void setupStylesheet( Scene scene ) {
        String stylesheet = Properties.getCustomStylesheet()
                .map( FxUtils::toAbsoluteFileUri )
                .orElse( resourcePath( "css/LogFX.css" ) );

        String iconsStylesheet = resourcePath( "css/icons.css" );

        Runnable resetStylesheet = () -> Platform.runLater( () -> {
            scene.getStylesheets().clear();
            scene.getStylesheets().addAll( stylesheet, iconsStylesheet );
        } );

        if ( Properties.getCustomStylesheet().isPresent() ) {
            log.info( "Using custom stylesheet for Scene: {}", stylesheet );
            if ( Properties.isRefreshStylesheet() ) {
                FileChangeWatcher stylesheetWatcher = new FileChangeWatcher(
                        Properties.getCustomStylesheet().get(),
                        TaskRunner.getGlobalInstance(),
                        resetStylesheet );

                scene.getWindow().setOnCloseRequest( event -> stylesheetWatcher.close() );
            } else {
                log.info( "Custom stylesheet will not be refreshed" );
                resetStylesheet.run();
            }
        } else {
            log.debug( "Using default stylesheet for new Scene" );
            resetStylesheet.run();
        }

    }

    /**
     * @return true if running on Mac OS.
     */
    @SuppressWarnings( "StringEquality" )
    public static boolean isMac() {
        return getOs() == OperatingSystems.MAC;
    }

    /**
     * @return true if running on Windows.
     */
    @SuppressWarnings( "StringEquality" )
    public static boolean isWindows() {
        return getOs() == OperatingSystems.WINDOWS;
    }

    public static String getOs() {
        String os = System.getProperty( "os.name", "" );
        if ( os.contains( "Mac" ) ) {
            return OperatingSystems.MAC;
        }
        if ( os.contains( "Windows" ) ) {
            return OperatingSystems.WINDOWS;
        }
        // we can't guess anything else
        return OperatingSystems.LINUX;
    }

    private static String toAbsoluteFileUri( File file ) {
        String absolutePath = file.getAbsolutePath();
        if ( File.separatorChar == '\\' ) {
            // windows stuff
            return "file:///" + absolutePath.replace( "\\", "/" );
        } else {
            return "file:" + absolutePath;
        }
    }

    public static void addIfNotPresent( ObservableList<String> styleClasses,
                                        String... cssClasses ) {
        for ( String cssClass : cssClasses ) {
            if ( !styleClasses.contains( cssClass ) ) {
                styleClasses.add( cssClass );
            }
        }
    }
}
