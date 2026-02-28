package com.athaydes.logfx.build;

import com.athaydes.logfx.splash.SplashMaker;
import jbuild.api.*;
import jbuild.api.config.JbConfig;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.spi.ToolProvider;

import static jbuild.api.JBuildException.ErrorCause.ACTION_ERROR;

@JbTaskInfo( description = "Package LogFX using jlink", name = "jlink" )
public class Jlink implements JbTask {

    private final JbConfig config;
    private final JBuildLogger log;

    public Jlink( JbConfig config, JBuildLogger log ) {
        this.config = config;
        this.log = log;
    }

    @Override
    public List<String> dependsOn() {
        return List.of( "installRuntimeDependencies", "compile" );
    }

    @Override
    public void run( String... args ) throws IOException {
        log.verbosePrintln( "Starting jlink task" );

        var imagePath = Path.of( "build", "image" );

        // delete existing image directory if present
        if ( Files.exists( imagePath ) ) {
            log.verbosePrintln( "Deleting existing image directory" );
            deleteRecursively( imagePath );
        }

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

        // generate splash screen image in the image's bin/ directory
        log.verbosePrintln( "Generating splash screen image" );
        new SplashMaker().run( "build/image/bin/logfx-logo" );

        // zip the image
        var zipPath = Path.of( "build", "logfx-" + config.version + ".zip" );
        log.verbosePrintln( () -> "Creating zip: " + zipPath );
        createZip( imagePath, zipPath );
    }

    private static void createZip( Path sourceDir, Path zipPath ) throws IOException {
        Files.deleteIfExists( zipPath );
        var env = new HashMap<String, String>();
        env.put( "create", "true" );
        var zipUri = URI.create( "jar:" + zipPath.toUri() );

        try ( var zipFs = FileSystems.newFileSystem( zipUri, env ) ) {
            Files.walkFileTree( sourceDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException {
                    var entryPath = zipFs.getPath( sourceDir.relativize( file ).toString() );
                    if ( entryPath.getParent() != null ) {
                        Files.createDirectories( entryPath.getParent() );
                    }
                    Files.copy( file, entryPath, StandardCopyOption.REPLACE_EXISTING );
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attrs ) throws IOException {
                    var entryPath = zipFs.getPath( sourceDir.relativize( dir ).toString() );
                    Files.createDirectories( entryPath );
                    return FileVisitResult.CONTINUE;
                }
            } );
        }
    }

    private static void deleteRecursively( Path path ) throws IOException {
        Files.walkFileTree( path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException {
                file.toFile().setWritable( true );
                Files.delete( file );
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory( Path dir, IOException exc ) throws IOException {
                dir.toFile().setWritable( true );
                Files.delete( dir );
                return FileVisitResult.CONTINUE;
            }
        } );
    }
}
