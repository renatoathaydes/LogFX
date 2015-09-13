package com.athaydes.logfx.ui;

import com.athaydes.logfx.text.HighlightExpression;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.athaydes.logfx.ui.Arrow.Direction.DOWN;
import static com.athaydes.logfx.ui.Arrow.Direction.UP;

/**
 *
 */
public class HighlightOptions extends VBox {

    private final ObservableList<HighlightExpression> observableExpressions;

    public HighlightOptions() {
        setSpacing( 5 );
        setPadding( new Insets( 5 ) );
        Label label = new Label( "Enter highlight expressions:" );
        label.setFont( Font.font( "Lucida", FontWeight.BOLD, 14 ) );

        observableExpressions = FXCollections.observableArrayList(
                new HighlightExpression( ".*WARN.*", Color.YELLOW, Color.RED ),
                new HighlightExpression( ".*", Color.BLACK, Color.LIGHTGREY ) );

        Button newRow = new Button( "Add rule" );
        newRow.setOnAction( this::addRow );

        getChildren().addAll( label, new Row( ".*WARN.*" ), new CatchAllRow( ".*" ), newRow );
    }


    private void addRow( Object ignore ) {
        int index = 0;
        for ( Node child : getChildren() ) {
            if ( child instanceof CatchAllRow ) {
                break;
            }
            index++;
        }
        getChildren().add( index - 1, new Row( ".*text.*" ) );
    }

    private List<HighlightExpression> computeExpressions() {
        return getChildren().stream()
                .filter( it -> it instanceof Row )
                .map( r -> {
                    Row row = ( Row ) r;
                    return new HighlightExpression(
                            row.expressionField.getText(),
                            row.bkgColorRectangle.getFill(),
                            row.fillColorRectangle.getFill() );
                } ).collect( Collectors.toList() );
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

    private void updateExpressions() {
        observableExpressions.setAll( computeExpressions() );
    }

    private EventHandler<ActionEvent> moveUpEventHandler( Row row ) {
        return ( event ) -> {
            int childIndex = getChildren().indexOf( row );
            if ( childIndex > 1 ) { // first child is a label
                Node previousChild = getChildren().remove( childIndex - 1 );
                getChildren().add( childIndex, previousChild );
            }
        };
    }

    private EventHandler<ActionEvent> moveDownEventHandler( Row row ) {
        return ( event ) -> {
            int childIndex = getChildren().indexOf( row );
            Node nextChild = getChildren().get( childIndex + 1 );
            if ( !( nextChild instanceof CatchAllRow ) && ( nextChild instanceof Row ) ) {
                getChildren().remove( nextChild );
                getChildren().add( childIndex, nextChild );
            }
        };
    }

    private class CatchAllRow extends Row {
        CatchAllRow( String text ) {
            super( text, false );
            expressionField.setEditable( false );
            expressionField.setDisable( true );
        }

        @Override
        protected Optional<Node> upDownButtons() {
            return Optional.empty();
        }
    }

    private class Row extends HBox {
        final TextField expressionField;
        final TextField bkgColorField;
        final TextField fillColorField;
        final Rectangle bkgColorRectangle;
        final Rectangle fillColorRectangle;

        Row( String text ) {
            this( text, true );
        }

        Row( String text, boolean editable ) {
            setSpacing( 5 );

            expressionField = new TextField( text );
            expressionField.setEditable( editable );
            expressionField.setMinWidth( 300 );
            expressionField.setOnAction( event -> updateExpressions() );

            bkgColorRectangle = new Rectangle( 20, 20 );
            fillColorRectangle = new Rectangle( 20, 20 );

            HighlightExpression expression = expressionFor( text );
            bkgColorField = fieldFor( bkgColorRectangle, expression.getBkgColor().toString() );
            fillColorField = fieldFor( fillColorRectangle, expression.getFillColor().toString() );

            setMinWidth( 500 );
            getChildren().addAll( expressionField,
                    bkgColorField, bkgColorRectangle,
                    fillColorField, fillColorRectangle );

            Optional<Node> upDown = upDownButtons();
            if ( upDown.isPresent() ) {
                getChildren().add( upDown.get() );
            }
        }

        protected Optional<Node> upDownButtons() {
            VBox upDownArrows = new VBox( 2 );
            upDownArrows.getChildren().addAll(
                    Arrow.arrowButton( UP, moveUpEventHandler( this ) ),
                    Arrow.arrowButton( DOWN, moveDownEventHandler( this ) ) );
            return Optional.of( upDownArrows );
        }

        private TextField fieldFor( Rectangle colorRectangle, String initialColor ) {
            TextField field = new TextField( initialColor );
            colorRectangle.setFill( Color.valueOf( field.getText() ) );
            field.setMinWidth( 30 );
            field.setOnAction( event -> {
                try {
                    colorRectangle.setFill( Color.valueOf( field.getText() ) );
                    updateExpressions();
                } catch ( IllegalArgumentException e ) {
                    System.out.println( "Invalid color entered" );
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
        dialog.setAlwaysOnTop( true );
        dialog.show();
        return dialog;
    }

}
