package com.athaydes.logfx.ui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class Arrow extends Canvas {

    private static final double[] upDownXs = { 0, 5, 10 };
    private static final double[] upYs = { 6, 0, 6 };
    private static final double[] downYs = { 0, 6, 0 };

    enum Direction {
        UP, DOWN
    }

    public Arrow( Direction direction ) {
        super( 10, 6 );
        GraphicsContext graphics = getGraphicsContext2D();
        graphics.setFill( Color.BLACK );
        fillPolygon( graphics, direction );
    }

    private static void fillPolygon( GraphicsContext graphics, Direction direction ) {
        switch ( direction ) {
            case UP:
                graphics.fillPolygon( upDownXs, upYs, 3 );
                break;
            case DOWN:
                graphics.fillPolygon( upDownXs, downYs, 3 );
                break;
        }
    }

    public static Button arrowButton( Direction direction,
                                      EventHandler<ActionEvent> eventEventHandler,
                                      String toolTipText ) {
        Button button = new Button( "", new Arrow( direction ) );
        button.setFont( Font.font( 4.0 ) );
        button.setMinWidth( 16 );
        button.setMinHeight( 8 );
        button.setTooltip( new Tooltip( toolTipText ) );
        button.getTooltip().setFont( Font.font( 12.0 ) );
        button.setOnAction( eventEventHandler );
        return button;
    }

}
