package com.athaydes.logfx.ui;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;

/**
 * Mapping of unicode characters to AwesomeIcons.
 * <p>
 * See http://www.jensd.de/wordpress/?p=132
 * And http://fontawesome.io/
 */
public class AwesomeIcons {

    private AwesomeIcons() {
        // hide it
    }

    public static ToggleButton createToggleButton( String iconName ) {
        return createToggleButton( iconName, 16 );
    }

    public static ToggleButton createToggleButton( String iconName, int iconSize ) {
        Label icon = createIconLabel( iconName );
        icon.setStyle( "-fx-font-size: " + iconSize + "px;" );
        return new ToggleButton( "", icon );
    }

    public static Button createIconButton( String iconName ) {
        return createIconButton( iconName, "", 16 );
    }

    public static Button createIconButton( String iconName, String text ) {
        return createIconButton( iconName, text, 16 );
    }

    public static Button createIconButton( String iconName, int iconSize ) {
        return createIconButton( iconName, "", iconSize );
    }

    public static Button createIconButton( String iconName, String text, int iconSize ) {
        Label icon = createIconLabel( iconName );
        icon.setStyle( "-fx-font-size: " + iconSize + "px;" );
        return new Button( text, icon );
    }

    public static Label createIconLabel( String iconName, String style ) {
        Label label = new Label( iconName );
        label.setStyle( style );
        return label;
    }

    public static Label createIconLabel( String iconName ) {
        return createIconLabel( iconName, 16 );
    }

    public static Label createIconLabel( String iconName, int iconSize ) {
        Label label = new Label( iconName );
        label.setStyle( "-fx-font-size: " + iconSize + "px;" );
        label.getStyleClass().add( "icons" );
        return label;
    }

    public static final String CLOSE = "\uf2d4";
    public static final String ARROW_DOWN = "\uf063";
    public static final String HELP = "\uf059";
    public static final String TRASH = "\uf1f8";
    public static final String CLOCK = "\uf017";
    public static final String PAUSE = "\uf04c";
    public static final String PENCIL = "\uf040";

}
