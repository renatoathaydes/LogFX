package com.athaydes.logfx;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Preloader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.athaydes.logfx.ui.AboutLogFXView.HEIGHT;
import static com.athaydes.logfx.ui.AboutLogFXView.WIDTH;

/**
 * JavaFX Preloader that displays a splash screen while the main application loads.
 * <p>
 * The splash image path can be overridden via the {@code LOGFX_SPLASH_IMAGE} environment variable.
 * If not set, it defaults to {@code $JAVA_HOME/bin/logfx-logo.png}.
 * If the image file doesn't exist, no splash is shown.
 */
public class SplashPreloader extends Preloader {

    private Stage stage;

    @Override
    public void start( Stage primaryStage ) {
        this.stage = primaryStage;

        var envPath = System.getenv( "LOGFX_SPLASH_IMAGE" );
        Path imagePath = envPath != null
                ? Path.of( envPath )
                : Path.of( System.getProperty( "java.home" ), "bin", "logfx-logo.png" );
        if ( !Files.exists( imagePath ) ) {
            return;
        }

        Image image = new Image( imagePath.toUri().toString() );
        ImageView imageView = new ImageView( image );
        imageView.setFitWidth( WIDTH );
        imageView.setFitHeight( HEIGHT );
        imageView.setPreserveRatio( true );

        StackPane root = new StackPane( imageView );
        Scene scene = new Scene( root, WIDTH, HEIGHT, Color.TRANSPARENT );

        primaryStage.initStyle( StageStyle.TRANSPARENT );
        primaryStage.setScene( scene );
        primaryStage.setWidth( WIDTH );
        primaryStage.setHeight( HEIGHT );
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    @Override
    public void handleStateChangeNotification( StateChangeNotification info ) {
        if ( info.getType() == StateChangeNotification.Type.BEFORE_START ) {
            if ( stage != null && stage.getScene() != null ) {
                var root = stage.getScene().getRoot();
                var duration = Duration.seconds( 1 );

                var fade = new FadeTransition( duration, root );
                fade.setFromValue( 1.0 );
                fade.setToValue( 0.0 );
                fade.setInterpolator( Interpolator.EASE_IN );

                var scale = new ScaleTransition( duration, root );
                scale.setToX( 1.1 );
                scale.setToY( 1.1 );
                scale.setInterpolator( Interpolator.EASE_IN );

                var transition = new ParallelTransition( fade, scale );
                transition.setOnFinished( e -> stage.hide() );
                transition.play();
            }
        }
    }
}
