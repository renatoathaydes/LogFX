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

import java.util.OptionalLong;

public class TimeGapEditor {
    private final Dialog dialog;

    TimeGapEditor( LogView selectedLogView, Runnable onAccept ) {
        VBox root = new VBox( 20 );

        dialog = new Dialog( root );
        dialog.setStyle( StageStyle.UNDECORATED );
        dialog.setResizable( false );
        dialog.setWidth( 360.0 );

        Label label = new Label( "Set the minimum time gap to display in milliseconds:" );

        LongField valueField = new LongField( selectedLogView.getMinTimeGap().getValue() );
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

    private static class LongField extends TextField {

        private final BooleanProperty valid = new SimpleBooleanProperty( true );

        private long value;

        LongField( long value ) {
            this.value = value;
            setText( Long.toString( value ) );
            textProperty().addListener( ( observable, oldValue, newValue ) -> {
                var maybeValue = getPositiveLong( newValue );
                if ( maybeValue.isPresent() ) {
                    this.value = maybeValue.getAsLong();
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

        private OptionalLong getPositiveLong( String newValue ) {
            try {
                var i = Long.parseLong( newValue );
                return i >= 0 ? OptionalLong.of( i ) : OptionalLong.empty();
            } catch ( NumberFormatException ignore ) {
                return OptionalLong.empty();
            }
        }

        BooleanProperty validProperty() {
            return valid;
        }

        long getValue() {
            return value;
        }
    }

}
