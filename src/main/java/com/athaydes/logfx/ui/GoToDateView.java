package com.athaydes.logfx.ui;

import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;

/**
 * A view that allows the user to select a date-time to go to in an opened log file.
 */
public class GoToDateView {

    private final DatePicker datePicker = new DatePicker();
    private final Dialog dialog;
    //private final LogView logView;

    GoToDateView() {
        //  this.logView = logView;

        VBox root = new VBox( 20 );
        root.setStyle( "-fx-padding: 10;" );

        dialog = new Dialog( root );
        dialog.setStyle( StageStyle.UNDECORATED );
        dialog.setResizable( false );

        Label dateLabel = new Label( "Go to date-time:" );

        Button cancelButton = new Button( "Cancel" );
        cancelButton.setOnAction( event -> dialog.hide() );

        Button goButton = new Button( "Go" );
        goButton.setOnAction( event -> {
            //logView.goTo()

            dialog.hide();
        } );

        HBox buttonBox = new HBox( 10 );
        buttonBox.getChildren().addAll( cancelButton, goButton );

        root.getChildren().addAll( dateLabel, datePicker, buttonBox );
    }

    void show() {
        dialog.show();
    }


}
