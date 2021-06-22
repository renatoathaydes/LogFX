package com.athaydes.logfx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import java.awt.Taskbar;

import static com.athaydes.logfx.ui.FxUtils.resourceUrl;

/**
 * A class that sets up the tray icon, if possible on the current OS.
 */
class SetupTrayIcon {
    private static final Logger log = LoggerFactory.getLogger( SetupTrayIcon.class );

    /**
     * Set the Mac tray icon. Only works if this is called on a Mac OS system.
     */
    static void run() {
        if ( !Taskbar.isTaskbarSupported() ) {
            log.debug( "Skipping Taskbar setup as it is not supported in this OS" );
            return;
        }

        log.debug( "Setting up Taskbar icon" );
        java.awt.Image image = new ImageIcon( resourceUrl( "images/favicon-large.png" ) ).getImage();

        try {
            Taskbar.getTaskbar().setIconImage( image );
        } catch ( Throwable t ) {
            System.err.println( "Cannot set tray icon due to error: " + t );
        }
    }
}
