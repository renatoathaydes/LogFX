package com.athaydes.logfx.build;

import jbuild.api.JBuildException;
import jbuild.api.JBuildLogger;
import jbuild.api.JbTask;
import jbuild.api.JbTaskInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.spi.ToolProvider;

import static jbuild.api.JBuildException.ErrorCause.ACTION_ERROR;

@JbTaskInfo( description = "Package LogFX using jlink", name = "jlink" )
public class Jlink implements JbTask {

    private final JBuildLogger log;

    public Jlink( JBuildLogger log ) {
        this.log = log;
    }

    @Override
    public List<String> dependsOn() {
        return List.of( "installRuntimeDependencies", "compile" );
    }

    @Override
    public void run( String... args ) throws IOException {
        log.verbosePrintln( "Starting jlink task" );

        var maybeJlink = ToolProvider.findFirst( "jlink" );
        if ( maybeJlink.isEmpty() ) {
            throw new JBuildException( "jlink tool is not available", ACTION_ERROR );
        }
        var jlink = maybeJlink.get();
        var code = jlink.run( System.out, System.err,
                "--module-path", String.join( File.pathSeparator, "build/runtime-libs", "build/LogFX.jar" ),
                "--add-modules", "java.base,java.desktop,javafx.controls,jdk.unsupported,jdk.crypto.ec,org.slf4j," +
                        "com.athaydes.logfx",
                "--output", "build/image",
                "--strip-debug", "--no-header-files", "--no-man-pages" );

        if ( code == 0 ) {
            log.verbosePrintln( "jlink executed without errors" );
        } else {
            throw new JBuildException( "jlink command failed with code: " + code, ACTION_ERROR );
        }

        var isWindows = System.getProperty( "os.name", "" ).toLowerCase( Locale.ROOT ).contains( "windows" );
        var logfxScript = "logfx" + ( isWindows ? ".bat" : "" );

        log.verbosePrintln( () -> "Copying script " + logfxScript + " to the image" );

        Files.copy(
                Paths.get( "src", "main", "sh", logfxScript ),
                Paths.get( "build", "image", "bin", logfxScript ) );
    }
}
