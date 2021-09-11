package com.athaydes.logfx.ui;

import com.athaydes.logfx.data.LogLineColors;
import com.athaydes.logfx.text.HighlightExpression;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import static com.athaydes.logfx.ui.Arrow.Direction.DOWN;
import static com.athaydes.logfx.ui.Arrow.Direction.UP;
import static com.athaydes.logfx.ui.AwesomeIcons.HELP;
import static com.athaydes.logfx.ui.AwesomeIcons.TRASH;
import static com.athaydes.logfx.ui.FxUtils.resourcePath;

/**
 * The highlight options screen.
 */
public class HighlightOptions extends VBox {

    private static final Logger log = LoggerFactory.getLogger( HighlightOptions.class );

    private final ObservableList<HighlightExpression> observableExpressions;
    private final BooleanProperty isFilterEnabled;

    private String groupName;
    private final VBox expressionsBox;
    private final StandardLogColorsRow standardLogColorsRow;

    public HighlightOptions( String groupName,
                             SimpleObjectProperty<LogLineColors> standardLogColors,
                             ObservableList<HighlightExpression> observableExpressions,
                             BooleanProperty isFilterEnabled ) {
        this.groupName = groupName;
        this.observableExpressions = observableExpressions;
        this.isFilterEnabled = isFilterEnabled;
        this.expressionsBox = new VBox( 2 );

        // allow the caller to add children AFTER calling this HighlightOptions
        Platform.runLater( () -> expressionsBox.getChildren().addAll( observableExpressions.stream()
                .map( ex -> new HighlightExpressionRow( ex, observableExpressions, expressionsBox ) )
                .toArray( HighlightExpressionRow[]::new ) ) );

        setSpacing( 5 );
        setPadding( new Insets( 5 ) );
        Label headerLabel = new Label( "Enter highlight expressions:" );

        CheckBox enableFilter = new CheckBox( "Filter?" );
        enableFilter.selectedProperty().bindBidirectional( isFilterEnabled );
        enableFilter.setTooltip( new Tooltip( "Filter log lines that match selected rules" ) );

        Node helpIcon = createHelpIcon();

        HBox headerRow = new HBox( 10 );
        headerRow.getChildren().addAll( headerLabel, helpIcon );

        BorderPane headerPane = new BorderPane();
        headerPane.setLeft( headerRow );
        headerPane.setRight( enableFilter );

        BorderPane buttonsPane = new BorderPane();

        Button newRow = new Button( "Add rule" );
        newRow.setOnAction( this::addRow );

        Button selectAllFilters = new Button( "All" );
        selectAllFilters.setOnAction( ( event ) -> enableFilters( true ) );
        Button disableAllFilters = new Button( "None" );
        disableAllFilters.setOnAction( ( event ) -> enableFilters( false ) );

        HBox filterButtons = new HBox( 5, new Label( "Select Filters:" ),
                selectAllFilters, disableAllFilters );

        buttonsPane.setLeft( newRow );
        buttonsPane.setRight( filterButtons );

        standardLogColorsRow = new StandardLogColorsRow( standardLogColors );

        getChildren().addAll(
                headerPane,
                expressionsBox,
                standardLogColorsRow,
                buttonsPane );
    }

    void onShow() {
        // try to focus on the first rule's text input
        ObservableList<Node> expressionRows = expressionsBox.getChildren();
        Row firstRow = expressionRows.isEmpty() ? standardLogColorsRow : ( Row ) expressionRows.get( 0 );
        firstRow.receiveFocus();
    }

    ObservableList<HighlightExpression> getHighlightOptions() {
        return observableExpressions;
    }

    private void enableFilters( boolean enable ) {
        if ( !enable ) {
            // set first if disabling because then we avoid refreshing files more than once
            isFilterEnabled.setValue( false );
        }

        List<HighlightExpression> newExpressions = observableExpressions.stream()
                .map( ( e ) -> e.withFilter( enable ) )
                .collect( Collectors.toList() );
        observableExpressions.setAll( newExpressions );

        expressionsBox.getChildren().setAll( observableExpressions.stream()
                .map( ex -> new HighlightExpressionRow( ex, observableExpressions, expressionsBox ) )
                .toArray( HighlightExpressionRow[]::new ) );

        if ( enable ) {
            // set last if enabling so only when all expressions are selected, do we refresh the file
            isFilterEnabled.setValue( true );
        }
    }

    private Node createHelpIcon() {
        Node help = AwesomeIcons.createIcon( HELP );

        AnchorPane content = helpScreen();
        Dialog helpDialog = new Dialog( content );
        helpDialog.setTitle( "Highlight Options Help" );
        helpDialog.setStyle( StageStyle.UTILITY );
        helpDialog.setResizable( false );

        help.setOnMouseClicked( event -> {
            helpDialog.setOwner( getScene().getWindow() );
            helpDialog.show();
        } );

        help.setOnMouseEntered( event -> getScene().setCursor( Cursor.HAND ) );
        help.setOnMouseExited( event -> getScene().setCursor( Cursor.DEFAULT ) );

        return help;
    }

    private static AnchorPane helpScreen() {
        BiFunction<String, String, Text> text = ( String value, String cssClass ) -> {
            var t = new Text( value );
            if ( cssClass != null ) t.getStyleClass().add( cssClass );
            return t;
        };
        var pane = new AnchorPane();
        pane.setPrefHeight( 400 );
        pane.setPrefWidth( 600 );
        pane.getStylesheets().add( resourcePath( "css/highlight-options-help.css" ) );

        var texts = new TextFlow(
                text.apply( "Highlight Options\n", "h2" ),
                text.apply( "This dialog allows you to set up rules for highlighting text in the logs.\n", null ),
                text.apply( "When a line of a log file is displayed, its contents are checked against each of these " +
                        "rules.\n" +
                        "The first rule to match is applied. All rules below the matching rule are ignored.\n", null ),
                text.apply( "The last rule is always empty, which means it always matches. Therefore, it can be used " +
                        "to define the standard style of the log.\n\n", null ),
                text.apply( "Writing expressions\n", "h2" ),
                text.apply( "Each rule defines a regular expression that is used to determine if its styles should be" +
                        " applied to a log line.\n", null ),
                text.apply( "For example, to match all lines containing the word ", null ),
                text.apply( "Error", "code" ),
                text.apply( " just enter the ", null ),
                text.apply( "Error", "code" ),
                text.apply( " expression.\n", null ),
                text.apply( "To match any line containing one or more ", null ),
                text.apply( "A", "code" ),
                text.apply( "'s, enter ", null ),
                text.apply( "A+", "code" ),
                text.apply( ".\n", null ),
                text.apply( "To match a whole line, you can use the usual ", null ),
                text.apply( "^", "code" ),
                text.apply( " (start) and ", null ),
                text.apply( "\\$", "code" ),
                text.apply( " (end) anchors.\nSo, to mach lines starting with ", null ),
                text.apply( "[INFO]", "code" ),
                text.apply( ", enter ", null ),
                text.apply( "^\\[INFO\\]", null ),
                text.apply( " (notice that the brackets must be escaped).\n\n" +
                        "To make the expression case-insensitive, start it with ", null ),
                text.apply( "(?i)", "code" ),
                text.apply( ".\n", null ),
                text.apply( "For assistance writing Java regular expressions, check the ", null ),
                new Link( "https://docs.oracle.com/javase/tutorial/essential/regex/", "Oracle Regex Tutorial" ),
                text.apply( ".\n\n", null ),
                text.apply( "Choosing the style for a rule\n", "h2" ),
                text.apply( "If a rule matches, the background and text colors selected in the first and second color" +
                        " pickers, respectively, are applied to the log line.\nA color may be specified by:\n\n" +
                        "   - name: e.g. blue or yellow.\n" +
                        "   - hex-value: e.g. 0x00ff00.\n" +
                        "   - web-value: e.g. #C0FFEE.\n\n", null ),
                text.apply( "See the ", null ),
                new Link( "https://docs.oracle.com/javase/8/javafx/api/javafx/scene/paint/Color.html", "Color class" ),
                text.apply( " for all options.", null )
        );

        pane.getChildren().add( texts );

        return pane;
    }

    private void addRow( Object ignore ) {
        HighlightExpression expression = new HighlightExpression( "", nextColor(), nextColor(), false );
        observableExpressions.add( expression );
        expressionsBox.getChildren().add( new HighlightExpressionRow(
                expression, observableExpressions, expressionsBox ) );
    }

    static Color nextColor() {
        return Color.color( Math.random(), Math.random(), Math.random() );
    }

    public String getGroupName() {
        return groupName;
    }

    void setGroupName( String groupName ) {
        this.groupName = groupName;
    }

    private static class StandardLogColorsRow extends Row {
        private final SimpleObjectProperty<LogLineColors> standardLogColors;

        StandardLogColorsRow( SimpleObjectProperty<LogLineColors> standardLogColors ) {
            super( "<default for all groups>", standardLogColors.get().getBackground(),
                    standardLogColors.get().getFill(), false );
            this.standardLogColors = standardLogColors;

            expressionField.setEditable( false );
            expressionField.setDisable( true );

            getChildren().addAll( expressionField,
                    bkgColorPicker.node(), fillColorPicker.node() );
        }

        @Override
        protected void update( Color bkgColor, Color fillColor, boolean isFiltered ) {
            standardLogColors.set( new LogLineColors( bkgColor, fillColor ) );
        }
    }

    private static class HighlightExpressionRow extends Row {
        private HighlightExpression expression;
        private final ObservableList<HighlightExpression> observableExpressions;
        private final VBox parent;

        HighlightExpressionRow( HighlightExpression expression,
                                ObservableList<HighlightExpression> observableExpressions,
                                VBox parent ) {
            super( expression.getPattern().pattern(), expression.getBkgColor(),
                    expression.getFillColor(), expression.isFiltered() );
            this.expression = expression;
            this.observableExpressions = observableExpressions;
            this.parent = parent;

            getChildren().addAll( expressionField,
                    bkgColorPicker.node(), fillColorPicker.node(),
                    isFilteredBox, upDownButtons(), removeButton() );
        }

        private Node upDownButtons() {
            VBox upDownArrows = new VBox( 2 );
            upDownArrows.getChildren().addAll(
                    Arrow.arrowButton( UP, moveUpEventHandler(), "Move rule up" ),
                    Arrow.arrowButton( DOWN, moveDownEventHandler(), "Move rule down" ) );
            return upDownArrows;
        }

        private EventHandler<ActionEvent> moveUpEventHandler() {
            return event -> {
                int index = observableExpressions.indexOf( expression );
                if ( index > 0 ) {
                    observableExpressions.remove( index );
                    parent.getChildren().remove( index );
                    observableExpressions.add( index - 1, expression );
                    parent.getChildren().add( index - 1, this );
                }
            };
        }

        private EventHandler<ActionEvent> moveDownEventHandler() {
            return event -> {
                int index = observableExpressions.indexOf( expression );
                if ( index < observableExpressions.size() - 1 ) {
                    observableExpressions.remove( index );
                    parent.getChildren().remove( index );
                    observableExpressions.add( index + 1, expression );
                    parent.getChildren().add( index + 1, this );
                }
            };
        }

        private Node removeButton() {
            Button removeButton = AwesomeIcons.createIconButton( TRASH );
            removeButton.setTooltip( new Tooltip( "Remove rule" ) );
            removeButton.setOnMouseClicked( event -> {
                observableExpressions.remove( expression );
                parent.getChildren().remove( this );
            } );
            removeButton.setOnMouseEntered( event -> getScene().setCursor( Cursor.HAND ) );
            removeButton.setOnMouseExited( event -> getScene().setCursor( Cursor.DEFAULT ) );
            return removeButton;
        }

        @Override
        protected void update( Color bkgColor, Color fillColor, boolean isFiltered ) {
            int index = observableExpressions.indexOf( expression );
            try {
                expression = new HighlightExpression(
                        expressionField.getText(), bkgColor, fillColor, isFiltered );
                observableExpressions.set( index, expression );
                expressionField.getStyleClass().remove( "error" );
            } catch ( PatternSyntaxException e ) {
                if ( !expressionField.getStyleClass().contains( "error" ) ) {
                    expressionField.getStyleClass().add( "error" );
                }
                log.warn( "Invalid regular expression: {}", e.toString() );
            }
        }

    }

    private static abstract class Row extends HBox {
        final TextField expressionField;
        final ColorChooser bkgColorPicker;
        final ColorChooser fillColorPicker;
        final CheckBox isFilteredBox;

        Row( String pattern, Paint backgroundColor, Paint fillColor, boolean isFiltered ) {
            setSpacing( 5 );

            expressionField = new TextField( pattern );
            expressionField.setMinWidth( 300 );
            expressionField.setTooltip( new Tooltip( "Enter a regular expression." ) );

            bkgColorPicker = new ColorChooser( backgroundColor.toString(), "Enter the background color." );
            fillColorPicker = new ColorChooser( fillColor.toString(), "Enter the text color." );

            isFilteredBox = new CheckBox();
            isFilteredBox.setSelected( isFiltered );
            isFilteredBox.setTooltip( new Tooltip( "Include in filter" ) );

            InvalidationListener updater = ( ignore ) ->
                    update( bkgColorPicker.getColor(), fillColorPicker.getColor(),
                            isFilteredBox.selectedProperty().get() );

            bkgColorPicker.addListener( updater );
            fillColorPicker.addListener( updater );
            isFilteredBox.selectedProperty().addListener( updater );
            expressionField.textProperty().addListener( updater );

            setMinWidth( 500 );
        }

        @MustCallOnJavaFXThread
        protected abstract void update( Color bkgColor, Color fillColor, boolean isFiltered );

        void receiveFocus() {
            expressionField.requestFocus();
        }
    }

    public static Dialog showHighlightOptionsDialog( HighlightGroupsView highlightGroups ) {
        ScrollPane pane = new ScrollPane( highlightGroups );
        pane.setHbarPolicy( ScrollPane.ScrollBarPolicy.NEVER );
        Dialog dialog = new Dialog( pane );
        dialog.setTitle( "Highlight Options" );
        dialog.setSize( 880.0, 340.0 );
        dialog.setStyle( StageStyle.UTILITY );
        dialog.setResizable( false );
        dialog.makeTransparentWhenLoseFocus();
        dialog.getBox().setAlignment( Pos.TOP_CENTER );

        var paneButtons = new VBox( 10 );
        var closeButton = new Button( "Close" );
        closeButton.setOnAction( event -> dialog.hide() );
        paneButtons.getChildren().add( closeButton );
        dialog.getBox().getChildren().add( paneButtons );

        dialog.show();
        Platform.runLater( highlightGroups::onShow );
        return dialog;
    }

}
