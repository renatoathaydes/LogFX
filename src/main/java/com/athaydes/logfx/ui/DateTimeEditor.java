package com.athaydes.logfx.ui;

import com.athaydes.logfx.data.YesOrNoMap;
import com.athaydes.logfx.text.DateTimeFormatGuesser;
import com.athaydes.logfx.text.PatternBasedDateTimeFormatGuess;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.athaydes.logfx.ResourceUtils.resourcePath;

public final class DateTimeEditor extends BorderPane {
    private static final int LIGHT_RADIUS = 8;
    private static final Logger log = LoggerFactory.getLogger( DateTimeEditor.class );

    private final ObservableList<PatternBasedDateTimeFormatGuess> guesses;
    private final ListView<PatternBasedDateTimeFormatGuess> guessesList;
    private final TextField nameField;
    private final TextField regexField;
    private final TextField formatField;
    private final TextField testTextField;
    private final TextField dateTimeOutputField;
    private final TextField errorField;
    private final Circle regexLight;
    private final Circle formatLight;
    private final Button addButton;
    private final Button removeButton;
    private final Button resetButton;

    // Currently editable values
    private Pattern lineRegex;
    private DateTimeFormatter formatter;

    public DateTimeEditor( ObservableList<PatternBasedDateTimeFormatGuess> guesses ) {
        setPrefWidth( 700.0 );

        // the list must be mutable, do not count on the provided list being mutable
        this.guesses = guesses;
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
        Node regexHelpIcon = HelpIconFactory.create( "Regex Help", this::getScene, regexHelp() );
        HBox regexLabelBox = new HBox( 10, regexLabel, regexHelpIcon );
        regexBox.getChildren().addAll( regexLabelBox, regexField );

        // Format input
        VBox formatBox = new VBox( 5 );
        Label formatLabel = new Label( "DateTime Format:" );
        formatField = new TextField();
        formatField.getStyleClass().add( "text-input" );
        formatField.setPromptText( "Enter DateTime format (e.g. yyyy-MM-dd HH:mm:ss)" );
        formatField.setTooltip( new Tooltip( "Enter DateTime format (e.g. yyyy-MM-dd HH:mm:ss)" ) );
        Node formatHelpIcon = HelpIconFactory.create( "DateTime Format Help", this::getScene, dateTimeFormatHelp() );
        HBox formatLabelBox = new HBox( 10, formatLabel, formatHelpIcon );
        formatBox.getChildren().addAll( formatLabelBox, formatField );

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

        errorField = new TextField();
        errorField.setDisable( true );
        errorField.getStyleClass().add( "error-output" );
        errorField.setPromptText( "Errors parsing the Regex or DateTime format will show up here" );

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
        resetButton = new Button( "Reset All" );
        buttonBox.getChildren().addAll( addButton, removeButton, resetButton );

        editor.getChildren().addAll( nameBox, regexBox, formatBox, testBox, buttonBox, errorField );
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
                log.debug( "Selected DateTimeFormat: {}", newVal );
                if ( !newVal.name().equals( nameField.getText() ) ) {
                    nameField.setText( newVal.name() );
                }
                if ( !newVal.linePattern().pattern().equals( regexField.getText() ) ) {
                    regexField.setText( newVal.linePattern().pattern() );
                }
                if ( !newVal.formatterString().equals( formatField.getText() ) ) {
                    formatField.setText( newVal.formatterString() );
                }
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

        resetButton.setOnAction( e -> {
            Dialog.showQuestionDialog( "Are you sure you want to reset all DateTime patterns to the default values?",
                    new YesOrNoMap( this::resetPatterns ).toMap() );
        } );

        nameField.textProperty().addListener( e -> {
            var guess = getSelected();
            if ( guess != null && !guess.name().equals( nameField.getText() ) ) {
                saveSelectedPattern();
            }
        } );

        regexField.textProperty().addListener( e -> {
            try {
                lineRegex = Pattern.compile( regexField.getText() );
                log.debug( "Regex set to: {}", regexField.getText() );
                regexField.getStyleClass().remove( "error" );
            } catch ( PatternSyntaxException ex ) {
                lineRegex = null;
                FxUtils.addIfNotPresent( regexField.getStyleClass(), "error" );
                return;
            }
            var guess = getSelected();
            if ( guess != null && !guess.linePattern().pattern().equals( regexField.getText() ) ) {
                saveSelectedPattern();
            }
        } );

        formatField.textProperty().addListener( e -> {
            try {
                formatter = DateTimeFormatter.ofPattern( formatField.getText() );
                log.debug( "Format set to: {}", formatField.getText() );
                formatField.getStyleClass().remove( "error" );
            } catch ( IllegalArgumentException ex ) {
                formatter = null;
                FxUtils.addIfNotPresent( formatField.getStyleClass(), "error" );
                return;
            }
            var guess = getSelected();
            if ( guess != null && !guess.formatterString().equals( formatField.getText() ) ) {
                saveSelectedPattern();
            }
        } );

        // Listener to update the test result lights and dateTime output text
        InvalidationListener resultListener = ( obs ) -> {
            dateTimeOutputField.setText( "" );
            errorField.setText( "" );
            setGrayLight( formatLight );
            setGrayLight( regexLight );
            var testText = testTextField.getText();

            if ( testText == null || testText.isBlank() || lineRegex == null || formatter == null ) {
                return;
            }

            log.debug( "Checking DateTime pattern result for text: {}", testText );
            var matcher = lineRegex.matcher( testText );
            if ( !matcher.find() ) {
                setRedLight( regexLight );
                return;
            }
            setGreenLight( regexLight );

            String dateTimeStr = null;
            try {
                dateTimeStr = matcher.group( PatternBasedDateTimeFormatGuess.DATE_TIME_GROUP );
            } catch ( IllegalArgumentException e ) {
                log.debug( "Pattern does not have the expected group {}", PatternBasedDateTimeFormatGuess.DATE_TIME_GROUP );
                errorField.setText( "REGEX: " + e.getMessage() );
            }

            if ( dateTimeStr == null ) {
                setRedLight( formatLight );
                return;
            }

            dateTimeOutputField.setText( dateTimeStr );
            try {
                formatter.parse( dateTimeStr );
                setGreenLight( formatLight );
            } catch ( DateTimeParseException e ) {
                log.debug( "Error parsing date-time string '{}': {}", dateTimeStr, e.getMessage() );
                errorField.setText( "DATETIME: " + e.getMessage() );
                setRedLight( formatLight );
            }
        };

        regexField.textProperty().addListener( resultListener );
        formatField.textProperty().addListener( resultListener );
        testTextField.textProperty().addListener( resultListener );
    }

    private void resetPatterns() {
        guesses.setAll( DateTimeFormatGuesser.standardGuesses() );
        Platform.runLater( () -> guessesList.getSelectionModel().selectFirst() );
    }

    private void saveSelectedPattern() {
        var name = nameField.getText().trim();
        if ( lineRegex == null || formatter == null || name.isEmpty() ) {
            log.trace( "Tried to save selected pattern, but state was not valid" );
            return;
        }
        PatternBasedDateTimeFormatGuess selected = guessesList.getSelectionModel().getSelectedItem();
        if ( selected != null ) {
            log.debug( "Saving the selected DateTime pattern with name '{}'", name );
            try {
                guesses.set( guessesList.getSelectionModel().getSelectedIndex(),
                        new PatternBasedDateTimeFormatGuess( name,
                                lineRegex, formatter, formatField.getText() ) );
            } catch ( IllegalArgumentException e ) {
                log.warn( "Could not save DateTime pattern with name '{}': {}", name, e.getMessage() );
            }
        }
    }

    private PatternBasedDateTimeFormatGuess getSelected() {
        int selectedIndex = guessesList.getSelectionModel().getSelectedIndex();
        if ( 0 <= selectedIndex && selectedIndex < guesses.size() ) {
            return guesses.get( selectedIndex );
        }
        return null;
    }

    private void clearFields() {
        log.debug( "Clearing fields." );
        nameField.clear();
        regexField.clear();
        formatField.clear();
        testTextField.clear();
        dateTimeOutputField.clear();
        guessesList.getSelectionModel().clearSelection();
        setGrayLight( regexLight );
        setGrayLight( formatLight );
        removeButton.setDisable( true );
    }

    private static void setGreenLight( Circle circle ) {
        circle.setFill( Color.GREEN );
        circle.getStyleClass().clear();
        circle.getStyleClass().add( "green-light" );
    }

    private static void setRedLight( Circle circle ) {
        circle.setFill( Color.RED );
        circle.getStyleClass().clear();
        circle.getStyleClass().add( "red-light" );
    }

    private static void setGrayLight( Circle circle ) {
        circle.setFill( Color.GRAY );
        circle.getStyleClass().clear();
    }

    private static AnchorPane regexHelp() {
        BiFunction<String, String, Text> text = ( String value, String cssClass ) -> {
            var t = new Text( value );
            if ( cssClass != null ) t.getStyleClass().add( cssClass );
            return t;
        };
        var pane = new AnchorPane();
        pane.setPrefHeight( 200 );
        pane.setPrefWidth( 600 );
        pane.getStylesheets().add( resourcePath( "css/highlight-options-help.css" ) );

        var texts = new TextFlow(
                text.apply( "Regex Pattern Help\n", "h2" ),
                text.apply( "\nThis Regex Pattern is used to extract the full DateTime String from ", null ),
                text.apply( "each log line.\n\nThe Regex must contain a named group called ", null ),
                text.apply( "dt", "code" ),
                text.apply( " and, ", null ),
                text.apply( "optionally, a group called ", null ),
                text.apply( "tz", "code" ),
                text.apply( " within the former group.\n", null ),
                text.apply( "The ", null ),
                text.apply( "dt", "code" ),
                text.apply( " group should contain the full DateTime String, while the ", null ),
                text.apply( "tz", "code" ),
                text.apply( " group contains the time-zone, if available.\n\n", null ),
                text.apply( "References", "h3" ),
                text.apply( "\n\nFor assistance writing Java regular expressions, check the ", null ),
                new Link( "https://docs.oracle.com/javase/tutorial/essential/regex/", "Oracle Regex Tutorial" ),
                text.apply( ".\n\n", null ),
                text.apply( "For live debugging a regex, try the ", null ),
                new Link( "https://regex101.com/", "regex101.com" ),
                text.apply( " website.\n\n", null ),
                text.apply( "For quick reference, check the documentation for the ", null ),
                new Link( "https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/util/regex/Pattern.html", "Pattern class" ),
                text.apply( ".\n", null )
        );

        pane.getChildren().add( texts );

        return pane;
    }

    private static AnchorPane dateTimeFormatHelp() {
        BiFunction<String, String, Text> text = ( String value, String cssClass ) -> {
            var t = new Text( value );
            if ( cssClass != null ) t.getStyleClass().add( cssClass );
            return t;
        };
        var pane = new AnchorPane();
        pane.setPrefHeight( 200 );
        pane.setPrefWidth( 600 );
        pane.getStylesheets().add( resourcePath( "css/highlight-options-help.css" ) );

        var texts = new TextFlow(
                text.apply( "DateTimeFormat Help\n", "h2" ),
                text.apply( "\nThe DateTime Format is used to parse the String extracted by the Regex Pattern into a ", null ),
                text.apply( "LocalDateTime", "code" ),
                text.apply( " Java object.\n\n", null ),
                text.apply( "Example", "h3" ),
                text.apply( "\n\nThe format string ", null ),
                text.apply( "yyyy-MM-dd HH:mm:ss zz", "code" ),
                text.apply( " successfully parses the text ", null ),
                text.apply( "2025-01-30 11:22:33 CET", "code" ),
                text.apply( ".\n\n", null ),
                text.apply( "References", "h3" ),
                text.apply( "\n\nCheck the documentation for the ", null ),
                new Link( "https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/time/format/DateTimeFormatter.html", "DateTimeFormatter class" ),
                text.apply( " for the full syntax.", null )
        );

        pane.getChildren().add( texts );

        return pane;
    }
}
