package com.athaydes.logfx.ui;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;

public class AwesomeIcons {

    private AwesomeIcons() {
        // hide it
    }

    public static ToggleButton createToggleButton( String iconName ) {
        Node icon = createIcon( iconName );
        return new ToggleButton( "", icon );
    }

    public static Button createIconButton( String iconName ) {
        return createIconButton( iconName, "" );
    }

    public static Button createIconButton( String iconName, String text ) {
        Node icon = createIcon( iconName );
        return new Button( text, icon );
    }

    public static Label createIcon( String iconName ) {
        Label icon = new Label( iconName );
        icon.getStyleClass().add( "icons" );
        return icon;
    }

    public static final String CLOSE = "\ue646";
    public static final String ARROW_DOWN = "\ue6a7";
    public static final String HELP = "\ue718";
    public static final String TRASH = "\ue605";
    public static final String CLOCK = "\ue66e";
    public static final String PAUSE = "\ue6ae";
    public static final String PENCIL = "\ue61c";
    public static final String PLUS = "\ue61a";
    public static final String LIST_UL = "\ue6c3";

}
