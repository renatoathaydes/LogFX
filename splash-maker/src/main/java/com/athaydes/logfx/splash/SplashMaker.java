package com.athaydes.logfx.splash;

import com.athaydes.logfx.LogFXHostServices;
import com.athaydes.logfx.ResourceUtils;
import com.athaydes.logfx.ui.AboutLogFXView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Transform;
import javafx.stage.Stage;
import jbuild.api.JbTask;
import jbuild.api.JbTaskInfo;
import jbuild.api.TaskPhase;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.List;

@JbTaskInfo( name = "makeSplashScreen",
        description = "Creates LogFX splash screen.",
        phase = @TaskPhase( name = "setup" ) )
public final class SplashMaker extends Application implements JbTask {

    @Override
    public void start( Stage primaryStage ) {
        System.out.println( "Starting SplashMaker..." );
        LogFXHostServices.set( getHostServices() );

        List<String> args = getParameters().getUnnamed();
        System.out.println( "SplashMaker parameters: " + args );
        if ( args.size() != 1 ) {
            System.err.println( "Wrong number of arguments provided, expected 1, got " + args.size() );
            exit( primaryStage );
            return;
        }

        final VBox node = new VBox( 10 );
        node.getStyleClass().add( "dialog-vbox" );
        node.setAlignment( Pos.CENTER );
        node.setPadding( new Insets( 20 ) );

        primaryStage.setScene( new Scene( node ) );
        primaryStage.getScene().getStylesheets().add( ResourceUtils.resourcePath( "css/LogFX.css" ) );

        node.getChildren().add( new AboutLogFXView().createNode() );

        final var filePrefix = args.get( 0 );
        final var scales = new double[]{ 1.25, 2.0 };
        final var suffixes = new String[]{ ".png", "@2x.png" };

        Platform.runLater( () -> {
            try {
                for ( int i = 0; i < scales.length; i++ ) {
                    System.out.println( "SplashMaker: Generating image with scale " + scales[ i ] + ": " +
                            filePrefix + suffixes[ i ] );
                    var image = createScaledView( node, scales[ i ] );
                    var file = new File( filePrefix + suffixes[ i ] );
                    if ( !file.getParentFile().mkdirs() && !file.getParentFile().isDirectory() ) {
                        System.err.println(
                                "SplashMaker error: Not a directory (and cannot be created): " + file.getParentFile()
                        );
                        return;
                    }
                    System.out.println( "SplashMaker: Writing splash image to " + file.getAbsolutePath() );

                    ImageIO.write( SwingFXUtils.fromFXImage( image, null ), "png", file );
                    System.out.println( "SplashMaker: Image written to " + file.getAbsolutePath() );
                }

                System.out.println( "SplashMaker: Done" );
            } catch ( Throwable e ) {
                System.err.println( "SplashMaker error: " + e );
            } finally {
                exit( primaryStage );
            }
        } );
    }

    void exit( Stage primaryStage ) {
        primaryStage.close();
        Platform.exit();
    }

    /**
     * Source: https://news.kynosarges.org/2017/02/01/javafx-snapshot-scaling/
     *
     * @return scaled image
     */
    private static Image createScaledView( Node node, double scale ) {
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

    /**
     * jb task runner.
     *
     * @param args command-line arguments provided by the user
     */
    @Override
    public void run( String... args ) {
        String imageFile;
        if ( args.length == 1 ) {
            imageFile = args[ 0 ];
        } else {
            imageFile = "build/image/bin/logfx-logo";
        }
        Application.launch( SplashMaker.class, imageFile );
    }

    /**
     * Main method, run by Gradle.
     *
     * @param args given by the Gradle runner
     */
    public static void main( String[] args ) {
        launch( args );
    }

}
