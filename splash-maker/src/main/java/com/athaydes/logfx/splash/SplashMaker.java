package com.athaydes.logfx.splash;

import com.athaydes.logfx.ui.AboutLogFXView;
import com.athaydes.logfx.ui.Dialog;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.transform.Transform;
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

        var filePrefix = args.get( 0 );
        var view = new AboutLogFXView();
        var dialog = view.createDialog();

        var scales = new double[]{ 1.25, 2.0 };
        var suffixes = new String[]{ ".png", "@2x.png" };

        Platform.runLater( () -> {
            for ( int i = 0; i < scales.length; i++ ) {
                var image = createScaledView( dialog, scales[ i ] );
                var file = new File( filePrefix + suffixes[ i ] );
                if ( !file.getParentFile().mkdirs() && !file.getParentFile().isDirectory() ) {
                    System.err.println( "Not a directory (and cannot be created): " + file.getParentFile() );
                    System.exit( 1 );
                }
                System.out.println( "Writing splash image to " + file.getAbsolutePath() );
                try {
                    ImageIO.write( SwingFXUtils.fromFXImage( image, null ), "png", file );
                    System.out.println( "Image written to " + file.getAbsolutePath() );
                } catch ( IOException e ) {
                    e.printStackTrace();
                    System.exit( 1 );
                }
            }

            System.exit( 0 );
        } );
    }

    /**
     * Source: https://news.kynosarges.org/2017/02/01/javafx-snapshot-scaling/
     *
     * @return scaled image
     */
    private static Image createScaledView( Dialog dialog, double scale ) {
        var node = dialog.getBox();

        var image = new WritableImage(
                ( int ) ( AboutLogFXView.WIDTH * scale ),
                ( int ) ( AboutLogFXView.HEIGHT * scale ) );

        var snapshotParameters = new SnapshotParameters();
        snapshotParameters.setTransform( Transform.scale( scale, scale ) );

        var result = node.snapshot( snapshotParameters, image );

        var view = new ImageView( result );
        view.setFitWidth( AboutLogFXView.WIDTH );
        view.setFitHeight( AboutLogFXView.HEIGHT );

        return view.getImage();
    }

    public static void main( String[] args ) {
        launch( args );
    }
}
