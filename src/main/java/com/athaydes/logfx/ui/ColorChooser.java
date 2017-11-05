package com.athaydes.logfx.ui;

import javafx.beans.InvalidationListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * LogFX color chooser.
 */
public class ColorChooser {

    private static final Logger log = LoggerFactory.getLogger( ColorChooser.class );

    private final ColorPicker colorPicker;
    private final ManualEditor manualEditor;
    private final HBox box;

    public ColorChooser( String initialColor,
                         String tooltipText ) {
        this.colorPicker = new ColorPicker();
        colorPicker.setTooltip( new Tooltip( tooltipText ) );
        box = new HBox( 2 );

        try {
            colorPicker.setValue( Color.valueOf( initialColor ) );
        } catch ( IllegalArgumentException e ) {
            log.warn( "Unable to set color picker's initial color to invalid color: {}", initialColor );
        }

        manualEditor = new ManualEditor( colorPicker.getValue(), tooltipText, colorPicker::setValue );
        addListener( ( observable -> {
            // only update the text if the currently visible control is the color picker
            if ( box.getChildren().get( 0 ) == colorPicker ) {
                manualEditor.colorField.setText( colorPicker.getValue().toString() );
            }
        } ) );

        ToggleButton editButton = AwesomeIcons.createToggleButton( AwesomeIcons.PENCIL, 10 );

        box.getChildren().addAll( colorPicker, editButton );

        editButton.setOnAction( event -> {
            ObservableList<Node> children = box.getChildren();
            if ( children.get( 0 ) == manualEditor ) {
                box.getChildren().set( 0, colorPicker );
            } else {
                box.getChildren().set( 0, manualEditor );
                manualEditor.colorField.requestFocus();
                manualEditor.colorField.selectAll();
            }
        } );
    }

    Node node() {
        return box;
    }

    public void addListener( InvalidationListener changeListener ) {
        colorPicker.valueProperty().addListener( changeListener );
    }

    public Color getColor() {
        return colorPicker.getValue();
    }

    private static class ManualEditor extends HBox {
        private final TextField colorField;
        private final Rectangle colorRectangle;

        ManualEditor( Color initialColor, String toolTipText, Consumer<Color> onUpdate ) {
            super( 5 );
            this.colorRectangle = new Rectangle( 20, 20 );
            this.colorRectangle.setFill( initialColor );
            this.colorField = fieldFor( colorRectangle, toolTipText, onUpdate );
            getChildren().addAll( colorField, colorRectangle );
        }

        private static TextField fieldFor( Rectangle colorRectangle,
                                           String toolTipText,
                                           Consumer<? super Color> onUpdate ) {
            TextField field = new TextField( colorRectangle.getFill().toString() );
            field.setTooltip( new Tooltip( toolTipText ) );
            field.setMinWidth( 30 );
            field.setMaxWidth( 114 );
            field.textProperty().addListener( ( ignore, oldValue, newValue ) -> {
                try {
                    Color colorValue = Color.valueOf( newValue );
                    colorRectangle.setFill( colorValue );
                    field.getStyleClass().remove( "error" );
                    onUpdate.accept( colorValue );
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

}
