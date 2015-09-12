package com.athaydes.logfx.ui;

import com.athaydes.logfx.text.HighlightExpression;
import com.sun.javafx.collections.ObservableListWrapper;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
public class HighlightOptions extends VBox {

    private final ObservableListWrapper<HighlightExpression> observableExpressions;

    public HighlightOptions() {
        setSpacing( 5 );
        setPadding( new Insets( 5 ) );
        Label label = new Label( "Enter highlight expressions:" );
        label.setFont( Font.font( "Lucida", FontWeight.BOLD, 14 ) );

        observableExpressions = new ObservableListWrapper<>( new ArrayList<>(
                Arrays.asList(
                        new HighlightExpression( ".*WARN.*", Color.YELLOW, Color.RED ),
                        new HighlightExpression( ".*", Color.BLACK, Color.LIGHTGREY )
                ) ) );

        getChildren().addAll( label, new Row( ".*WARN.*" ), new Row( ".*", false ) );
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

    public ObservableListWrapper<HighlightExpression> getObservableExpressions() {
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
        Dialog dialog = new Dialog( highlightOptions );
        dialog.setTitle( "Highlight Options" );
        dialog.setAlwaysOnTop( true );
        dialog.show();
        return dialog;
    }

}
