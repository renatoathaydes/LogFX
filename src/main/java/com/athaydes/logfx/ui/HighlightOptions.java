package com.athaydes.logfx.ui;

import com.athaydes.logfx.data.LogLineColors;
import com.athaydes.logfx.text.HighlightExpression;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
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
import java.util.regex.PatternSyntaxException;

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

    private final VBox expressionsBox;

    public HighlightOptions( SimpleObjectProperty<LogLineColors> standardLogColors,
                             ObservableList<HighlightExpression> observableExpressions ) {
        this.standardLogColors = standardLogColors;
        this.observableExpressions = observableExpressions;
        this.expressionsBox = new VBox( 2 );

        expressionsBox.getChildren().addAll( observableExpressions.stream()
                .map( ex -> new HighlightExpressionRow( ex, observableExpressions, expressionsBox ) )
                .toArray( HighlightExpressionRow[]::new ) );

        setSpacing( 5 );
        setPadding( new Insets( 5 ) );
        Label headerLabel = new Label( "Enter highlight expressions:" );
        headerLabel.setFont( Font.font( "Lucida", FontWeight.BOLD, 14 ) );

        Node helpIcon = createHelpIcon();

        HBox headerRow = new HBox( 10 );
        headerRow.getChildren().addAll( headerLabel, helpIcon );

        Button newRow = new Button( "Add rule" );
        newRow.setOnAction( this::addRow );

        getChildren().addAll(
                headerRow,
                expressionsBox,
                new StandardLogColorsRow( standardLogColors ),
                new HBox( 5, newRow ) );
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
        HighlightExpression expression = new HighlightExpression( "", nextColor(), nextColor() );
        observableExpressions.add( expression );
        expressionsBox.getChildren().add( new HighlightExpressionRow(
                expression, observableExpressions, expressionsBox ) );
    }

    private static Color nextColor() {
        return Color.color( Math.random(), Math.random(), Math.random() );
    }

    public ObservableList<HighlightExpression> getObservableExpressions() {
        return observableExpressions;
    }

    public SimpleObjectProperty<LogLineColors> getStandardLogColors() {
        return standardLogColors;
    }

    public LogLineColors logLineColorsFor( String text ) {
        for ( HighlightExpression expression : observableExpressions ) {
            if ( expression.matches( text ) ) {
                return expression.getLogLineColors();
            }
        }

        return standardLogColors.get();
    }

    private static class StandardLogColorsRow extends Row {
        private final SimpleObjectProperty<LogLineColors> standardLogColors;

        StandardLogColorsRow( SimpleObjectProperty<LogLineColors> standardLogColors ) {
            super( "", standardLogColors.get().getBackground(), standardLogColors.get().getFill() );
            this.standardLogColors = standardLogColors;

            expressionField.setEditable( false );
            expressionField.setDisable( true );
        }

        @Override
        protected void update() {
            standardLogColors.set( new LogLineColors(
                    bkgColorRectangle.getFill(), fillColorRectangle.getFill() ) );
        }
    }

    private static class HighlightExpressionRow extends Row {
        private HighlightExpression expression;
        private final ObservableList<HighlightExpression> observableExpressions;
        private final VBox parent;

        public HighlightExpressionRow( HighlightExpression expression,
                                       ObservableList<HighlightExpression> observableExpressions,
                                       VBox parent ) {
            super( expression.getPattern().pattern(), expression.getBkgColor(), expression.getFillColor() );
            this.expression = expression;
            this.observableExpressions = observableExpressions;
            this.parent = parent;

            getChildren().addAll( upDownButtons(), removeButton() );
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
        protected void update() {
            try {
                int index = observableExpressions.indexOf( expression );
                expression = new HighlightExpression(
                        expressionField.getText(), bkgColorRectangle.getFill(), fillColorRectangle.getFill() );
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
        final TextField bkgColorField;
        final TextField fillColorField;
        final Rectangle bkgColorRectangle;
        final Rectangle fillColorRectangle;

        Row( String pattern, Paint backgroundColor, Paint fillColor ) {
            setSpacing( 5 );

            expressionField = new TextField( pattern );
            expressionField.setMinWidth( 300 );
            expressionField.setTooltip( new Tooltip( "Enter a regular expression." ) );
            expressionField.textProperty().addListener( event -> update() );

            bkgColorRectangle = new Rectangle( 20, 20 );
            fillColorRectangle = new Rectangle( 20, 20 );

            bkgColorField = fieldFor( bkgColorRectangle,
                    backgroundColor.toString(), "Enter the background color." );
            fillColorField = fieldFor( fillColorRectangle,
                    fillColor.toString(), "Enter the text color." );

            setMinWidth( 500 );
            getChildren().addAll( expressionField,
                    bkgColorField, bkgColorRectangle,
                    fillColorField, fillColorRectangle );
        }

        @MustCallOnJavaFXThread
        protected abstract void update();

        private TextField fieldFor( Rectangle colorRectangle,
                                    String initialColor,
                                    String toolTipText ) {
            TextField field = new TextField( initialColor );
            field.setTooltip( new Tooltip( toolTipText ) );
            colorRectangle.setFill( Color.valueOf( field.getText() ) );
            field.setMinWidth( 30 );
            field.textProperty().addListener( ( ignore, oldValue, newValue ) -> {
                try {
                    colorRectangle.setFill( Color.valueOf( newValue ) );
                    field.getStyleClass().remove( "error" );
                    update();
                } catch ( IllegalArgumentException e ) {
                    if ( !field.getStyleClass().contains( "error" ) ) {
                        field.getStyleClass().add( "error" );
                    }
                    log.debug( "Invalid color entered" );
                }
            } );
            return field;
        }

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
