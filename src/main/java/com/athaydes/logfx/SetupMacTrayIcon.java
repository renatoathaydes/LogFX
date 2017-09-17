package com.athaydes.logfx;

import javax.swing.ImageIcon;

/**
 * A class that sets up the Mac tray icon.
 * <p>
 * This is not required in other OS's.
 */
class SetupMacTrayIcon {

    /**
     * Set the Mac tray icon. Only works if this is called on a Mac OS system.
     */
    static void run() {
        java.awt.Image image = new ImageIcon( LogFX.class.getResource( "/images/favicon-large.png" ) ).getImage();
        com.apple.eawt.Application.getApplication().setDockIconImage( image );
    }
}
