package com.athaydes.logfx.ui;

import com.athaydes.logfx.text.HighlightExpression;
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

import java.util.Optional;

import static com.athaydes.logfx.ui.Arrow.Direction.DOWN;
import static com.athaydes.logfx.ui.Arrow.Direction.UP;
import static java.util.stream.Collectors.toList;

/**
 *
 */
public class HighlightOptions extends VBox {

    private final ObservableList<HighlightExpression> observableExpressions;

    public HighlightOptions( ObservableList<HighlightExpression> observableExpressions ) {
        this.observableExpressions = observableExpressions;
        setSpacing( 5 );
        setPadding( new Insets( 5 ) );
        Label label = new Label( "Enter highlight expressions:" );
        label.setFont( Font.font( "Lucida", FontWeight.BOLD, 14 ) );

        Button newRow = new Button( "Add rule" );
        newRow.setOnAction( this::addRow );
        Button removeRow = new Button( "Remove rule" );
        removeRow.setOnAction( this::removeRow );

        getChildren().add( label );
        getChildren().addAll(
                observableExpressions.subList( 0, observableExpressions.size() - 1 ).stream()
                        .map( Row::new )
                        .collect( toList() ) );
        getChildren().addAll(
                new CatchAllRow( observableExpressions.get( observableExpressions.size() - 1 ) ),
                new HBox( 5, newRow, removeRow ) );
    }


    private void addRow( Object ignore ) {
        int index = 0;
        for ( Node child : getChildren() ) {
            if ( child instanceof CatchAllRow ) {
                break;
            }
            index++;
        }
        HighlightExpression expression = new HighlightExpression( ".*text.*", nextColor(), nextColor() );
        getChildren().add( index, new Row( expression ) );
        observableExpressions.add( observableExpressions.size() - 1, expression );
    }

    private void removeRow( Object ignore ) {
        int index = 0;
        for ( Node child : getChildren() ) {
            if ( child instanceof CatchAllRow ) {
                break;
            }
            index++;
        }
        if ( index > 1 ) {
            Row row = ( Row ) getChildren().remove( index - 1 );
            observableExpressions.remove( row.expression );
        }
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
            if ( childIndex > 1 ) { // 0th child is a label, 1st child cannot be moved up
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
            expressionField.setOnAction( event -> updateExpression() );

            bkgColorRectangle = new Rectangle( 20, 20 );
            fillColorRectangle = new Rectangle( 20, 20 );

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

        private void updateExpression() {
            HighlightExpression newExpression = new HighlightExpression(
                    expressionField.getText(), bkgColorRectangle.getFill(), fillColorRectangle.getFill()
            );
            int index = observableExpressions.indexOf( this.expression );
            observableExpressions.set( index, newExpression );
            this.expression = newExpression;
        }

        private TextField fieldFor( Rectangle colorRectangle, String initialColor ) {
            TextField field = new TextField( initialColor );
            colorRectangle.setFill( Color.valueOf( field.getText() ) );
            field.setMinWidth( 30 );
            field.textProperty().addListener( ( ignore, oldValue, newValue ) -> {
                try {
                    colorRectangle.setFill( Color.valueOf( newValue ) );
                    updateExpression();
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
