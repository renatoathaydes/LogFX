package com.athaydes.logfx;

import javafx.application.HostServices;

import java.util.concurrent.atomic.AtomicReference;

public final class LogFXHostServices {
    private static final AtomicReference<HostServices> hostServices = new AtomicReference<>();

    public static void set( HostServices hostServices ) {
        if ( !LogFXHostServices.hostServices.compareAndSet( null, hostServices ) ) {
            throw new IllegalStateException( "HostServices was already set" );
        }
    }

    public static HostServices get() {
        var services = hostServices.get();
        if ( services == null ) {
            throw new IllegalStateException( "HostServices was never set" );
        }
        return services;
    }
}
