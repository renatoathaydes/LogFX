package com.athaydes.logfx.ui;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;

import java.util.OptionalInt;

public class TimeGapEditor {
    private final Dialog dialog;

    TimeGapEditor( LogView selectedLogView, Runnable onAccept ) {
        VBox root = new VBox( 20 );

        dialog = new Dialog( root );
        dialog.setStyle( StageStyle.UNDECORATED );
        dialog.setResizable( false );
        dialog.setWidth( 360.0 );

        Label label = new Label( "Set the minimum time gap to display in milliseconds:" );

        IntField valueField = new IntField( selectedLogView.getMinTimeGap().getValue() );
        valueField.setMaxWidth( 120.0 );
        valueField.setTooltip( new Tooltip( "Enter the number of milliseconds (zero or larger)" ) );

        Button goButton = new Button( "OK" );
        goButton.disableProperty().bind( valueField.validProperty().not() );

        EventHandler<ActionEvent> goAction = event -> {
            selectedLogView.getMinTimeGap().set( valueField.getValue() );
            dialog.hide();
            Platform.runLater( onAccept );
        };

        valueField.setOnAction( goAction );
        goButton.setOnAction( goAction );

        dialog.dialogStage.focusedProperty().addListener( ( obs, oldVal, newVal ) -> {
            if ( !newVal ) dialog.hide();
        } );

        HBox buttonBox = new HBox( 10 );
        buttonBox.getChildren().add( goButton );

        root.getChildren().addAll( label, valueField, buttonBox );
    }

    void show() {
        dialog.show();
    }

    private static class IntField extends TextField {

        private final BooleanProperty valid = new SimpleBooleanProperty( true );

        private int value;

        IntField( int value ) {
            this.value = value;
            setText( Integer.toString( value ) );
            textProperty().addListener( ( observable, oldValue, newValue ) -> {
                var maybeValue = getPositiveInt( newValue );
                if ( maybeValue.isPresent() ) {
                    this.value = maybeValue.getAsInt();
                    valid.set( true );
                    getStyleClass().remove( "error" );
                } else {
                    valid.set( false );
                    if ( !getStyleClass().contains( "error" ) ) {
                        getStyleClass().add( "error" );
                    }
                }
            } );
        }

        private OptionalInt getPositiveInt( String newValue ) {
            try {
                var i = Integer.parseInt( newValue );
                return i >= 0 ? OptionalInt.of( i ) : OptionalInt.empty();
            } catch ( NumberFormatException ignore ) {
                return OptionalInt.empty();
            }
        }

        BooleanProperty validProperty() {
            return valid;
        }

        int getValue() {
            return value;
        }
    }

}
