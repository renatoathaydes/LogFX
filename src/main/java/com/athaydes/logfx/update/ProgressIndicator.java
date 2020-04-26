package com.athaydes.logfx.update;

import com.athaydes.logfx.concurrency.Cancellable;
import com.athaydes.logfx.concurrency.TaskRunner;
import com.athaydes.logfx.ui.Dialog;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.StageStyle;

import java.time.Duration;

import static javafx.util.Duration.millis;

final class ProgressIndicator {
    private final TaskRunner taskRunner;
    private Cancellable updater;
    private boolean done;

    ProgressIndicator( TaskRunner taskRunner ) {
        this.taskRunner = taskRunner;
    }

    void start() {
        Platform.runLater( () -> {
            var progressBar = new ProgressBar();
            progressBar.setMaxHeight( 10.0 );
            progressBar.setPrefWidth( 200.0 );
            var dialog = new Dialog( new Label( "Downloading LogFX" ), progressBar );
            dialog.setStyle( StageStyle.UNDECORATED );
            dialog.doNotCloseOnEscapePressed();
            dialog.show();
            updater = taskRunner.scheduleRepeatingTask( Duration.ofMillis( 150 ), () -> {
                Platform.runLater( () -> {
                    var current = progressBar.getProgress();
                    var step = current < 0.5 ? 0.05 : current < 0.9 ? 0.01 : 0.001;
                    var timeline = new Timeline(
                            new KeyFrame(
                                    millis( 0 ),
                                    new KeyValue( progressBar.progressProperty(), current )
                            ),
                            new KeyFrame(
                                    millis( 100 ),
                                    new KeyValue( progressBar.progressProperty(), done ? 1.0 : current + step )
                            )
                    );
                    if ( done ) {
                        timeline.setOnFinished( ( ignore ) -> {
                            updater.cancel();
                            dialog.hide();
                            updater = null;
                        } );
                    }
                    timeline.playFromStart();
                } );
            } );
        } );
    }

    void done() {
        Platform.runLater( () -> done = true );
    }
}
