package com.athaydes.logfx.ui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.util.Duration;

public final class BottomMessagePane extends BorderPane {

    private static final Duration ANIMATION_DURATION = Duration.millis( 150 );

    // the value is read first time we need to animate it, so that we get the value set
    // in the CSS
    private double minHeight = 10.0;

    public static BottomMessagePane warningIfFiltersEnabled() {
        String helpText = "Filters are enabled. Not showing full file contents.";
        return new BottomMessagePane( new Text( helpText ) );
    }

    private BottomMessagePane( Text text ) {
        super( text );
        getStyleClass().add( "bottom-message-pane" );
    }

    public void setShow( boolean show, Runnable then ) {
        double toHeight = show ? maybeUpdateMinHeight() : 0.0;
        Timeline timeline = new Timeline( new KeyFrame( ANIMATION_DURATION,
                new KeyValue( minHeightProperty(), toHeight ) ) );
        timeline.setOnFinished( event -> then.run() );
        timeline.play();
    }

    private double maybeUpdateMinHeight() {
        if ( getMinHeight() > 0 ) {
            minHeight = getMinHeight();
        }
        return minHeight;
    }
}
