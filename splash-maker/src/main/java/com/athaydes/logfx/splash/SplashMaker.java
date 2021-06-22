package com.athaydes.logfx.splash;

import com.athaydes.logfx.ui.AboutLogFXView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.List;

public final class SplashMaker extends Application {

    @Override
    public void start( Stage primaryStage ) {
        List<String> args = getParameters().getUnnamed();
        if ( args.size() != 1 ) {
            throw new IllegalStateException( "Wrong number of arguments provided, expected 1, got " + args.size() );
        }

        var view = new AboutLogFXView();
        var dialog = view.createDialog();

        Platform.runLater( () -> {
            var image = dialog.getBox().snapshot( new SnapshotParameters(), null );
            var file = new File( args.get( 0 ) );
            if (!file.getParentFile().mkdirs() && !file.getParentFile().isDirectory()) {
                throw new RuntimeException("Not a directory (and cannot be created): " + file.getParentFile());
            }
            System.out.println( "Writing splash image to " + file.getAbsolutePath() );
            try {
                ImageIO.write( SwingFXUtils.fromFXImage( image, null ), "png", file );
                System.out.println( "Image written to " + file.getAbsolutePath() );
            } catch ( IOException e ) {
                e.printStackTrace();
                System.exit( 1 );
            }
            System.exit( 0 );
        } );
    }

    public static void main( String[] args ) {
        launch( args );
    }
}
