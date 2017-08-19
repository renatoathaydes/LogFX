package com.athaydes.logfx.ui;


import javafx.scene.control.Button;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;

/**
 * A simple Button for the LogFX App.
 */
public class LogFxButton extends Button {

    static final Background onBkgrd = FxUtils.simpleBackground( Color.LIGHTGRAY );
    static final Background offBkgrd = FxUtils.simpleBackground( Color.DARKGRAY );

    private LogFxButton( String text, Background off, Background on ) {
        super( text );
        setBackground( off );
        setOnMouseEntered( ( event ) -> setBackground( on ) );
        setOnMouseExited( ( event ) -> setBackground( off ) );
    }

    public static LogFxButton closeButton( Runnable action ) {
        LogFxButton button = new LogFxButton( "Close", offBkgrd, onBkgrd );
        button.setOnAction( event -> action.run() );
        return button;
    }

    public static LogFxButton defaultLabel( String text ) {
        return new LogFxButton( text, onBkgrd, onBkgrd );
    }

}
