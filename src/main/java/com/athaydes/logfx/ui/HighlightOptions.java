package com.athaydes.logfx.ui;

import com.athaydes.logfx.data.LogLineColors;
import com.athaydes.logfx.text.HighlightExpression;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import static com.athaydes.logfx.ui.Arrow.Direction.DOWN;
import static com.athaydes.logfx.ui.Arrow.Direction.UP;
import static com.athaydes.logfx.ui.AwesomeIcons.HELP;
import static com.athaydes.logfx.ui.AwesomeIcons.TRASH;
import static java.util.stream.Collectors.joining;

/**
 * The highlight options screen.
 */
public class HighlightOptions extends VBox {

    private static final Logger log = LoggerFactory.getLogger( HighlightOptions.class );

    private final SimpleObjectProperty<LogLineColors> standardLogColors;
    private final ObservableList<HighlightExpression> observableExpressions;
    private final BooleanProperty isFilterEnabled;

    private final VBox expressionsBox;

    public HighlightOptions( SimpleObjectProperty<LogLineColors> standardLogColors,
                             ObservableList<HighlightExpression> observableExpressions,
                             BooleanProperty isFilterEnabled ) {
        this.standardLogColors = standardLogColors;
        this.observableExpressions = observableExpressions;
        this.isFilterEnabled = isFilterEnabled;
        this.expressionsBox = new VBox( 2 );

        expressionsBox.getChildren().addAll( observableExpressions.stream()
                .map( ex -> new HighlightExpressionRow( ex, observableExpressions, expressionsBox ) )
                .toArray( HighlightExpressionRow[]::new ) );

        setSpacing( 5 );
        setPadding( new Insets( 5 ) );
        Label headerLabel = new Label( "Enter highlight expressions:" );
        headerLabel.setFont( Font.font( "Lucida", FontWeight.BOLD, 14 ) );

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

        getChildren().addAll(
                headerPane,
                expressionsBox,
                new StandardLogColorsRow( standardLogColors ),
                buttonsPane );
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
        WebView htmlContent = new WebView();
        WebEngine webEngine = htmlContent.getEngine();

        String htmlText;
        try {
            htmlText = new BufferedReader( new InputStreamReader(
                    getClass().getResourceAsStream( "/html/highlight-options-help.html" ),
                    StandardCharsets.UTF_8 )
            ).lines().collect( joining( "\n" ) );

            webEngine.setUserStyleSheetLocation( getClass().getResource( "/css/web-view.css" ).toString() );
        } catch ( Exception e ) {
            log.warn( "Error loading HTML resources", e );
            htmlText = "<div>Could not open the help file</div>";
        }

        Label help = AwesomeIcons.createIconLabel( HELP );
        Dialog helpDialog = new Dialog( htmlContent );
        helpDialog.setTitle( "Highlight Options Help" );
        helpDialog.setStyle( StageStyle.UTILITY );
        helpDialog.setResizable( false );

        final String html = htmlText;

        help.setOnMouseClicked( event -> {
            helpDialog.setOwner( getScene().getWindow() );
            webEngine.loadContent( html );
            helpDialog.show();
        } );

        help.setOnMouseEntered( event -> getScene().setCursor( Cursor.HAND ) );
        help.setOnMouseExited( event -> getScene().setCursor( Cursor.DEFAULT ) );

        return help;
    }

    private void addRow( Object ignore ) {
        HighlightExpression expression = new HighlightExpression( "", nextColor(), nextColor(), false );
        observableExpressions.add( expression );
        expressionsBox.getChildren().add( new HighlightExpressionRow(
                expression, observableExpressions, expressionsBox ) );
    }

    private static Color nextColor() {
        return Color.color( Math.random(), Math.random(), Math.random() );
    }

    ObservableList<HighlightExpression> getObservableExpressions() {
        return observableExpressions;
    }

    SimpleObjectProperty<LogLineColors> getStandardLogColors() {
        return standardLogColors;
    }

    LogLineColors logLineColorsFor( String text ) {
        for ( HighlightExpression expression : observableExpressions ) {
            if ( expression.matches( text ) ) {
                return expression.getLogLineColors();
            }
        }

        return standardLogColors.get();
    }

    BooleanProperty filterEnabled() {
        return isFilterEnabled;
    }

    Optional<Predicate<String>> getLineFilter() {
        if ( isFilterEnabled.get() ) {
            List<HighlightExpression> filteredExpressions = observableExpressions.stream()
                    .filter( HighlightExpression::isFiltered )
                    .collect( Collectors.toList() );
            return Optional.of( ( line ) -> filteredExpressions.stream()
                    .anyMatch( ( exp ) -> exp.matches( line ) ) );
        } else {
            return Optional.empty();
        }
    }

    private static class StandardLogColorsRow extends Row {
        private final SimpleObjectProperty<LogLineColors> standardLogColors;

        StandardLogColorsRow( SimpleObjectProperty<LogLineColors> standardLogColors ) {
            super( "", standardLogColors.get().getBackground(), standardLogColors.get().getFill(), false );
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
            Label removeButton = AwesomeIcons.createIconLabel( TRASH );
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
                    update( bkgColorPicker.getColor(), fillColorPicker.getColor(), isFilteredBox.selectedProperty().get() );

            bkgColorPicker.addListener( updater );
            fillColorPicker.addListener( updater );
            isFilteredBox.selectedProperty().addListener( updater );
            expressionField.textProperty().addListener( updater );

            setMinWidth( 500 );
        }

        @MustCallOnJavaFXThread
        protected abstract void update( Color bkgColor, Color fillColor, boolean isFiltered );

    }

    public static Dialog showHighlightOptionsDialog( HighlightOptions highlightOptions ) {
        ScrollPane pane = new ScrollPane( highlightOptions );
        pane.setHbarPolicy( ScrollPane.ScrollBarPolicy.NEVER );
        Dialog dialog = new Dialog( new ScrollPane( highlightOptions ) );
        dialog.setSize( 850.0, 260.0 );
        dialog.setTitle( "Highlight Options" );
        dialog.setResizable( false );
        dialog.makeTransparentWhenLoseFocus();
        dialog.show();
        return dialog;
    }

}
