package com.athaydes.logfx;

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
        // FIXME set the tray on Mac! This worked on Java 8, does not work on Java 9
//        java.awt.Image image = new ImageIcon( LogFX.class.getResource( "/images/favicon-large.png" ) ).getImage();
//
//        try {
//            Class<?> appClass = Class.forName( "com.apple.eawt.Application" );
//            Object app = appClass.getMethod( "getApplication" ).invoke( appClass );
//            Method setter = app.getClass().getMethod( "setDockIconImage", java.awt.Image.class );
//            setter.invoke( app, image );
//        } catch ( Throwable t ) {
//            System.err.println( "Cannot set Mac tray icon due to error: " + t );
//        }
    }
}
