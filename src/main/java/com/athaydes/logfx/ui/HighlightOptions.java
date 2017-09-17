package com.athaydes.logfx.ui;

import com.athaydes.logfx.text.HighlightExpression;
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
import java.util.Optional;
import java.util.regex.PatternSyntaxException;

import static com.athaydes.logfx.ui.Arrow.Direction.DOWN;
import static com.athaydes.logfx.ui.Arrow.Direction.UP;
import static com.athaydes.logfx.ui.AwesomeIcons.HELP;
import static com.athaydes.logfx.ui.AwesomeIcons.TRASH;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * The highlight options screen.
 */
public class HighlightOptions extends VBox {

    private static final Logger log = LoggerFactory.getLogger( HighlightOptions.class );

    private final ObservableList<HighlightExpression> observableExpressions;

    public HighlightOptions( ObservableList<HighlightExpression> observableExpressions ) {
        this.observableExpressions = observableExpressions;
        setSpacing( 5 );
        setPadding( new Insets( 5 ) );
        Label headerLabel = new Label( "Enter highlight expressions:" );
        headerLabel.setFont( Font.font( "Lucida", FontWeight.BOLD, 14 ) );

        Node helpIcon = createHelpIcon();

        HBox headerRow = new HBox( 10 );
        headerRow.getChildren().addAll( headerLabel, helpIcon );

        Button newRow = new Button( "Add rule" );
        newRow.setOnAction( this::addRow );

        getChildren().add( headerRow );
        getChildren().addAll(
                observableExpressions.subList( 0, observableExpressions.size() - 1 ).stream()
                        .map( Row::new )
                        .collect( toList() ) );
        getChildren().addAll(
                new CatchAllRow( observableExpressions.get( observableExpressions.size() - 1 ) ),
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
        int index = 0;
        for ( Node child : getChildren() ) {
            if ( child instanceof CatchAllRow ) {
                break;
            }
            index++;
        }
        HighlightExpression expression = new HighlightExpression( "", nextColor(), nextColor() );
        getChildren().add( index, new Row( expression ) );
        observableExpressions.add( observableExpressions.size() - 1, expression );
    }

    private static Color nextColor() {
        return Color.color( Math.random(), Math.random(), Math.random() );
    }

    public ObservableList<HighlightExpression> getObservableExpressions() {
        return observableExpressions;
    }

    public HighlightExpression expressionFor( String text ) {
        for ( HighlightExpression expression : observableExpressions ) {
            if ( expression.matches( text ) ) {
                return expression;
            }
        }
        return observableExpressions.get( observableExpressions.size() - 1 );
    }

    private EventHandler<ActionEvent> moveUpEventHandler( Row row ) {
        HighlightExpression expression = row.expression;
        return ( event ) -> {
            int childIndex = getChildren().indexOf( row );
            if ( childIndex > 1 ) { // 0th child is the header, 1st child cannot be moved up
                int expressionIndex = observableExpressions.indexOf( expression );

                Row previousChild = ( Row ) getChildren().remove( childIndex - 1 );
                HighlightExpression previousExpression = observableExpressions.remove( expressionIndex - 1 );

                getChildren().add( childIndex, previousChild );
                observableExpressions.add( expressionIndex, previousExpression );
            }
        };
    }

    private EventHandler<ActionEvent> moveDownEventHandler( Row row ) {
        HighlightExpression expression = row.expression;
        return ( event ) -> {
            int childIndex = getChildren().indexOf( row );
            Node nextChild = getChildren().get( childIndex + 1 );
            if ( !( nextChild instanceof CatchAllRow ) && ( nextChild instanceof Row ) ) {
                getChildren().remove( nextChild );
                getChildren().add( childIndex, nextChild );

                int expressionIndex = observableExpressions.indexOf( expression );
                HighlightExpression nextExpression = observableExpressions.remove( expressionIndex + 1 );
                observableExpressions.add( expressionIndex, nextExpression );
            }
        };
    }

    private class CatchAllRow extends Row {
        CatchAllRow( HighlightExpression expression ) {
            super( expression, false );
            expressionField.setEditable( false );
            expressionField.setDisable( true );
        }

        @Override
        protected Optional<Node> upDownButtons() {
            return Optional.empty();
        }

        @Override
        protected Optional<Node> removeButton() {
            return Optional.empty();
        }
    }

    private class Row extends HBox {
        final TextField expressionField;
        final TextField bkgColorField;
        final TextField fillColorField;
        final Rectangle bkgColorRectangle;
        final Rectangle fillColorRectangle;
        volatile HighlightExpression expression;

        Row( HighlightExpression expression ) {
            this( expression, true );
        }

        Row( HighlightExpression expression, boolean editable ) {
            this.expression = expression;
            setSpacing( 5 );

            expressionField = new TextField( expression.getPattern().pattern() );
            expressionField.setEditable( editable );
            expressionField.setMinWidth( 300 );
            expressionField.setTooltip( new Tooltip( "Enter a regular expression." ) );
            expressionField.textProperty().addListener( event -> updateExpression() );

            bkgColorRectangle = new Rectangle( 20, 20 );
            fillColorRectangle = new Rectangle( 20, 20 );

            bkgColorField = fieldFor( bkgColorRectangle,
                    expression.getBkgColor().toString(), "Enter the background color." );
            fillColorField = fieldFor( fillColorRectangle,
                    expression.getFillColor().toString(), "Enter the text color." );

            setMinWidth( 500 );
            getChildren().addAll( expressionField,
                    bkgColorField, bkgColorRectangle,
                    fillColorField, fillColorRectangle );

            Optional<Node> upDown = upDownButtons();
            upDown.ifPresent( getChildren()::add );

            Optional<Node> remove = removeButton();
            remove.ifPresent( getChildren()::add );
        }

        protected Optional<Node> upDownButtons() {
            VBox upDownArrows = new VBox( 2 );
            upDownArrows.getChildren().addAll(
                    Arrow.arrowButton( UP, moveUpEventHandler( this ), "Move rule up" ),
                    Arrow.arrowButton( DOWN, moveDownEventHandler( this ), "Move rule down" ) );
            return Optional.of( upDownArrows );
        }

        protected Optional<Node> removeButton() {
            Label removeButton = AwesomeIcons.createIconLabel( TRASH );
            removeButton.setTooltip( new Tooltip( "Remove rule" ) );
            removeButton.setOnMouseClicked( event -> {
                observableExpressions.remove( expression );
                HighlightOptions.this.getChildren().remove( this );
            } );
            removeButton.setOnMouseEntered( event -> getScene().setCursor( Cursor.HAND ) );
            removeButton.setOnMouseExited( event -> getScene().setCursor( Cursor.DEFAULT ) );
            return Optional.of( removeButton );
        }

        private void updateExpression() {
            try {
                HighlightExpression newExpression = new HighlightExpression(
                        expressionField.getText(), bkgColorRectangle.getFill(), fillColorRectangle.getFill()
                );
                int index = observableExpressions.indexOf( this.expression );
                observableExpressions.set( index, newExpression );
                this.expression = newExpression;
                expressionField.getStyleClass().remove( "error" );
            } catch ( PatternSyntaxException e ) {
                if ( !expressionField.getStyleClass().contains( "error" ) ) {
                    expressionField.getStyleClass().add( "error" );
                }
                log.warn( "Invalid regular expression: {}", e.toString() );
            }
        }

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
                    updateExpression();
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
        dialog.setTitle( "Highlight Options" );
        dialog.setResizable( false );
        dialog.show();
        return dialog;
    }

}
