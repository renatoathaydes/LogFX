package com.athaydes.logfx.ui;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.text.Text;
import org.kordamp.ikonli.javafx.FontIcon;

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

    public static Text createIcon( String iconName ) {
        FontIcon icon = new FontIcon( iconName );
        icon.getStyleClass().add( "icons" );
        return icon;
    }

    public static final String CLOSE = "ti-close";
    public static final String ARROW_DOWN = "ti-download";
    public static final String HELP = "ti-help-alt";
    public static final String TRASH = "ti-trash";
    public static final String CLOCK = "ti-time";
    public static final String PAUSE = "ti-control-pause";
    public static final String PENCIL = "ti-pencil";
    public static final String PLUS = "ti-plus";
    public static final String LIST_UL = "ti-align-justify";

}
