package com.athaydes.logfx.ui;

import com.athaydes.logfx.ui.LogViewPane.LogViewWrapper;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;

/**
 * A view that allows the user to select a date-time to go to in an opened log file.
 */
class GoToDateView {

    private final Dialog dialog;

    GoToDateView( LogView selectedLogView,
                  Supplier<List<LogViewWrapper>> logViewsGetter ) {
        VBox root = new VBox( 20 );

        dialog = new Dialog( root );
        dialog.setStyle( StageStyle.UNDECORATED );
        dialog.setResizable( false );

        Label dateLabel = new Label( "Go to date-time:" );

        DateTimeTextField dateTimeField = new DateTimeTextField();
        dateTimeField.setMinWidth( 240.0 );
        dateTimeField.setTooltip( new Tooltip( "Enter a date and time (yyyy-MM-dd HH:mm:ss[.SSS z])" ) );

        CheckBox goToAll = new CheckBox( "All logs" );
        goToAll.setTooltip( new Tooltip( "Check this to go to the same date-time in all logs" ) );

        if ( selectedLogView == null ) {
            goToAll.setSelected( true );
            goToAll.setDisable( true );
        }

        Button cancelButton = new Button( "Cancel" );
        cancelButton.setOnAction( event -> dialog.hide() );

        Button goButton = new Button( "Go" );
        goButton.disableProperty().bind( dateTimeField.validProperty().not() );

        goButton.setOnAction( event -> {
            dateTimeField.getValue().ifPresent( dateTime -> {
                List<LogViewWrapper> viewWrappers = new ArrayList<>( 3 );
                if ( goToAll.isSelected() ) {
                    viewWrappers.addAll( logViewsGetter.get() );
                } else if ( selectedLogView != null ) {
                    viewWrappers.addAll( logViewsGetter.get().stream()
                            .filter( wrapper -> wrapper.getLogView() == selectedLogView )
                            .collect( toList() ) );
                }

                for ( LogViewWrapper wrapper : viewWrappers ) {
                    Platform.runLater( () -> wrapper.getLogView()
                            .goTo( dateTime, wrapper::scrollTo ) );
                }
            } );

            dialog.hide();
        } );

        HBox buttonBox = new HBox( 10 );
        buttonBox.getChildren().addAll( cancelButton, goButton );

        root.getChildren().addAll( dateLabel, dateTimeField, buttonBox, goToAll );
    }

    void show() {
        dialog.show();
    }

    private static class DateTimeTextField extends TextField {

        private static String lastValidDateTimeText = null;

        private final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendValue( ChronoField.YEAR )
                .appendLiteral( '-' )
                .appendValue( ChronoField.MONTH_OF_YEAR )
                .appendLiteral( '-' )
                .appendValue( ChronoField.DAY_OF_MONTH )
                .appendLiteral( ' ' )
                .appendValue( ChronoField.HOUR_OF_DAY )
                .appendLiteral( ':' )
                .appendValue( ChronoField.MINUTE_OF_HOUR )
                .appendLiteral( ':' )
                .appendValue( ChronoField.SECOND_OF_MINUTE )
                .optionalStart()
                .appendLiteral( '.' )
                .appendValue( ChronoField.MILLI_OF_SECOND )
                .optionalEnd()
                .optionalStart()
                .appendLiteral( ' ' )
                .appendZoneOrOffsetId()
                .toFormatter();

        private final BooleanProperty valid = new SimpleBooleanProperty( true );

        private ZonedDateTime dateTime;

        DateTimeTextField() {
            textProperty().addListener( ( observable, oldValue, newValue ) -> {
                try {
                    this.dateTime = formatter.parse( newValue, ZonedDateTime::from );
                    valid.set( true );
                    getStyleClass().remove( "error" );
                    lastValidDateTimeText = newValue;
                } catch ( DateTimeParseException e ) {
                    valid.set( false );
                    if ( !getStyleClass().contains( "error" ) ) {
                        getStyleClass().add( "error" );
                    }
                }
            } );

            if ( lastValidDateTimeText != null ) {
                setText( lastValidDateTimeText );
            } else {
                setText( formatter.format( LocalDateTime.now() ) + " " + localTimeOffset() );
            }
        }

        private static String localTimeOffset() {
            int offsetInHours = TimeZone.getDefault().getOffset( System.currentTimeMillis() ) / 3600000;
            int absOffsetInHours = Math.abs( offsetInHours );

            return ( offsetInHours >= 0 ? "+" : "-" ) +
                    ( ( absOffsetInHours < 10 ) ? "0" : "" ) +
                    absOffsetInHours + ":00";
        }

        BooleanProperty validProperty() {
            return valid;
        }

        Optional<ZonedDateTime> getValue() {
            return Optional.ofNullable( dateTime );
        }
    }

}
