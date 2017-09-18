package com.athaydes.logfx.data;

import javafx.scene.paint.Color;

/**
 * A tuple of 2 colors representing the standard colors for a log file in the UI.
 */
public class StandardLogColors {

    private final Color background;
    private final Color fill;

    public StandardLogColors( Color background, Color fill ) {
        this.background = background;
        this.fill = fill;
    }

    public Color getBackground() {
        return background;
    }

    public Color getFill() {
        return fill;
    }
}
