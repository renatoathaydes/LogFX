package com.athaydes.logfx.update;

import com.athaydes.keepup.api.AppDistributor;
import com.athaydes.keepup.api.KeepupConfig;
import com.athaydes.keepup.bintray.BintrayAppDistributor;

import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public class LogFXKeepupConfig implements KeepupConfig {

    private static final String OS_FAMILY;

    static {
        String osName = System.getProperty( "os.name" ).toLowerCase( Locale.ENGLISH );
        OS_FAMILY = osName.contains( "windows" )
                ? "win"
                : ( osName.contains( "mac" ) || osName.contains( "darwin" )
                ? "mac"
                : "linux" );
    }

    private final ExecutorService executor;
    private final Function<String, CompletionStage<Boolean>> acceptVersion;

    public LogFXKeepupConfig( ExecutorService executor,
                              Function<String, CompletionStage<Boolean>> acceptVersion ) {
        this.executor = executor;
        this.acceptVersion = acceptVersion;
    }

    @Override
    public String appName() {
        return "logfx";
    }

    @Override
    public AppDistributor<?> distributor() {
        return new BintrayAppDistributor(
                "renatoathaydes",
                OS_FAMILY,
                "logfx",
                "com.athaydes.logfx",
                "logfx",
                acceptVersion );
    }

    @Override
    public ExecutorService executor() {
        return executor;
    }
}
