package com.athaydes.logfx;

import javax.swing.ImageIcon;
import java.lang.reflect.Method;

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

        try {
            Class<?> appClass = Class.forName( "com.apple.eawt.Application" );
            Object app = appClass.getMethod( "getApplication" ).invoke( appClass );
            Method setter = app.getClass().getMethod( "setDockIconImage", java.awt.Image.class );
            setter.invoke( app, image );
        } catch ( Throwable t ) {
            System.err.println( "Cannot set Mac tray icon due to error: " + t );
        }
    }
}
