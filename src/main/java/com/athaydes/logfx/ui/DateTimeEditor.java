package com.athaydes.logfx.ui;

import com.athaydes.logfx.text.PatternBasedDateTimeFormatGuess;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class DateTimeEditor extends BorderPane {
    private static final int LIGHT_RADIUS = 8;

    private final ObservableList<PatternBasedDateTimeFormatGuess> guesses;
    private final ListView<PatternBasedDateTimeFormatGuess> guessesList;
    private final TextField nameField;
    private final TextField regexField;
    private final TextField formatField;
    private final TextField testTextField;
    private final TextField dateTimeOutputField;
    private final Circle regexLight;
    private final Circle formatLight;
    private final Button addButton;
    private final Button removeButton;

    // Currently editable values
    private Pattern lineRegex;
    private DateTimeFormatter formatter;

    public DateTimeEditor( List<PatternBasedDateTimeFormatGuess> guesses ) {
        setPrefWidth( 700.0 );

        // the list must be mutable, do not count on the provided list being mutable
        this.guesses = FXCollections.observableArrayList( new ArrayList<>( guesses ) );
        this.guessesList = new ListView<>( this.guesses );

        guessesList.setCellFactory( list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem( PatternBasedDateTimeFormatGuess item, boolean empty ) {
                super.updateItem( item, empty );
                setText( empty || item == null ? null : item.name() );
            }
        } );

        // Left side - list of patterns
        guessesList.setPrefWidth( 200 );
        setLeft( new ScrollPane( guessesList ) );

        // Right side - editor
        VBox editor = new VBox( 10 );
        editor.setPadding( new Insets( 10 ) );

        // Name input
        VBox nameBox = new VBox( 5 );
        Label nameLabel = new Label( "Name:" );
        nameField = new TextField();
        nameField.getStyleClass().add( "text-input" );
        nameField.setPromptText( "Name for the DateTime pattern" );
        nameBox.getChildren().addAll( nameLabel, nameField );

        // Regex input
        VBox regexBox = new VBox( 5 );
        Label regexLabel = new Label( "Regex Pattern:" );
        regexField = new TextField();
        regexField.setTooltip( new Tooltip( "Regex to extract a DateTime from a log line. " +
                "Must include the 'dt' group, e.g. '(\\w+\\s+)+(?<dt>\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}).*'" ) );
        regexField.getStyleClass().add( "text-input" );
        regexField.setPromptText( "Enter a regex to extract the DateTime. " +
                "Use the " + PatternBasedDateTimeFormatGuess.DATE_TIME_GROUP + " group to capture datetime, " +
                "and " + PatternBasedDateTimeFormatGuess.TIMEZONE_GROUP + " to optionally capture the timezone." );
        regexBox.getChildren().addAll( regexLabel, regexField );

        // Format input
        VBox formatBox = new VBox( 5 );
        Label formatLabel = new Label( "DateTime Format:" );
        formatField = new TextField();
        formatField.getStyleClass().add( "text-input" );
        formatField.setPromptText( "Enter DateTime format (e.g. yyyy-MM-dd HH:mm:ss)" );
        formatBox.getChildren().addAll( formatLabel, formatField );

        // Test section
        VBox testBox = new VBox( 5 );
        testBox.getStyleClass().add( "solid-border" );

        Label testLabel = new Label( "Test Text:" );
        testTextField = new TextField();
        testTextField.getStyleClass().add( "text-input" );
        testTextField.setPromptText( "Enter text to test the regex and DateTime pattern." );

        dateTimeOutputField = new TextField();
        dateTimeOutputField.setDisable( true );
        dateTimeOutputField.setPromptText( "Parsed DateTime will appear here if it can be extracted from the Test Text " +
                "(captured by the regex group '" + PatternBasedDateTimeFormatGuess.DATE_TIME_GROUP + "')." );

        HBox testResultBox = new HBox( 10 );
        testResultBox.setAlignment( Pos.CENTER_LEFT );
        formatLight = new Circle( LIGHT_RADIUS, Color.GRAY );
        regexLight = new Circle( LIGHT_RADIUS, Color.GRAY );

        testResultBox.getChildren().addAll(
                regexLight, new Label( "Regex Match" ),
                new Rectangle( 10, 5, Color.TRANSPARENT ),
                formatLight, new Label( "DateTime Match" )
        );

        testBox.getChildren().addAll( testLabel, testTextField, testResultBox, dateTimeOutputField );

        // Buttons
        HBox buttonBox = new HBox( 10 );
        addButton = new Button( "Add" );
        removeButton = new Button( "Remove" );
        buttonBox.getChildren().addAll( addButton, removeButton );

        editor.getChildren().addAll( nameBox, regexBox, formatBox, testBox, buttonBox );
        setCenter( editor );

        // Initial button states
        removeButton.setDisable( true );

        setupListeners();

        if ( !guesses.isEmpty() ) {
            guessesList.getSelectionModel().selectFirst();
        }
    }

    private void setupListeners() {
        // List selection listener
        guessesList.getSelectionModel().selectedItemProperty().addListener( ( obs, oldVal, newVal ) -> {
            if ( newVal != null ) {
                nameField.setText( newVal.name() );
                regexField.setText( newVal.linePattern().pattern() );
                formatField.setText( newVal.formatterString() );
                removeButton.setDisable( false );
            } else {
                clearFields();
            }
        } );

        // Add button
        addButton.setOnAction( e -> {
            String name = "New DateTime Pattern";
            Pattern pattern = Pattern.compile( "(?<dt>.*)" );
            guesses.add( new PatternBasedDateTimeFormatGuess( name, pattern, "yyyy-MM-dd HH:mm:ss" ) );
            Platform.runLater( () -> guessesList.getSelectionModel().select( guesses.size() - 1 ) );
        } );

        // Remove button
        removeButton.setOnAction( e -> {
            PatternBasedDateTimeFormatGuess selected = guessesList.getSelectionModel().getSelectedItem();
            if ( selected != null ) {
                if ( guesses.size() == 1 ) {
                    Dialog.showMessage( "Cannot remove last remaining pattern", Dialog.MessageLevel.WARNING );
                    return;
                }
                guesses.remove( selected );
                guessesList.getSelectionModel().selectFirst();
            }
        } );
        guesses.addListener( ( ListChangeListener<? super PatternBasedDateTimeFormatGuess> ) ( change ) -> {
            removeButton.setDisable( change.getList().size() <= 1 );
        } );

        nameField.textProperty().addListener( e -> saveSelectedPattern() );

        regexField.textProperty().addListener( e -> {
            try {
                lineRegex = Pattern.compile( regexField.getText() );
                regexField.getStyleClass().remove( "error" );
                saveSelectedPattern();
            } catch ( PatternSyntaxException ex ) {
                lineRegex = null;
                FxUtils.addIfNotPresent( regexField.getStyleClass(), "error" );
            }
        } );

        formatField.textProperty().addListener( e -> {
            try {
                formatter = DateTimeFormatter.ofPattern( formatField.getText() );
                formatField.getStyleClass().remove( "error" );
                saveSelectedPattern();
            } catch ( IllegalArgumentException ex ) {
                formatter = null;
                FxUtils.addIfNotPresent( formatField.getStyleClass(), "error" );
            }
        } );

        // Listener to update the test result lights and dateTime output text
        ChangeListener<String> resultListener = ( obs, oldVal, newVal ) -> {
            dateTimeOutputField.setText( "" );
            formatLight.setFill( Color.GRAY );
            regexLight.setFill( Color.GRAY );

            if ( newVal == null || newVal.isEmpty() || lineRegex == null || formatter == null ) {
                return;
            }

            var matcher = lineRegex.matcher( newVal );
            if ( !matcher.find() ) {
                regexLight.setFill( Color.RED );
                return;
            }
            regexLight.setFill( Color.GREEN );

            String dateTimeStr = matcher.group( PatternBasedDateTimeFormatGuess.DATE_TIME_GROUP );
            if ( dateTimeStr == null ) {
                formatLight.setFill( Color.RED );
                return;
            }
            dateTimeOutputField.setText( dateTimeStr );
            try {
                formatter.parse( dateTimeStr );
                formatLight.setFill( Color.GREEN );
            } catch ( DateTimeParseException e ) {
                formatLight.setFill( Color.RED );
            }
        };

        regexField.textProperty().addListener( resultListener );
        formatField.textProperty().addListener( resultListener );
        testTextField.textProperty().addListener( resultListener );
    }

    private void saveSelectedPattern() {
        if ( lineRegex == null || formatter == null || nameField.getText().trim().isEmpty() ) {
            return;
        }
        PatternBasedDateTimeFormatGuess selected = guessesList.getSelectionModel().getSelectedItem();
        if ( selected != null ) {
            String name = nameField.getText().trim();
            guesses.set( guessesList.getSelectionModel().getSelectedIndex(),
                    new PatternBasedDateTimeFormatGuess( name,
                            lineRegex, formatter, formatField.getText() ) );
        }
    }

    private void clearFields() {
        nameField.clear();
        regexField.clear();
        formatField.clear();
        testTextField.clear();
        dateTimeOutputField.clear();
        guessesList.getSelectionModel().clearSelection();
        regexLight.setFill( Color.GRAY );
        formatLight.setFill( Color.GRAY );
        removeButton.setDisable( true );
    }
}
