package com.athaydes.logfx.update;

import com.athaydes.logfx.config.Properties;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.athaydes.logfx.update.LogFXUpdater.LOGFX_UPDATE_ZIP;

final class LogFXReplacer {

    private static void replaceSelf( File destinationDir, File newVersionZipFile ) throws IOException {
        String topEntryName = null;
        try ( var zip = new ZipInputStream(
                new BufferedInputStream(
                        new FileInputStream( newVersionZipFile ), 4096 ) ) ) {
            var buffer = new byte[ 4096 ];
            var zipEntry = zip.getNextEntry();
            while ( zipEntry != null ) {
                if ( topEntryName == null ) {
                    topEntryName = zipEntry.getName();
                    continue;
                }
                var file = fileFor( zipEntry, destinationDir, topEntryName );
                if ( isDirectory( zipEntry ) ) {
                    file.mkdirs();
                } else {
                    try ( var out = new FileOutputStream( file ) ) {
                        int len;
                        while ( ( len = zip.read( buffer ) ) > 0 ) {
                            out.write( buffer, 0, len );
                        }
                    }
                }
                zipEntry = zip.getNextEntry();
            }
        }
    }

    private static File fileFor( ZipEntry zipEntry, File destinationDir, String topEntryName ) {
        return new File( destinationDir, zipEntry.getName().substring( topEntryName.length() ) );
    }

    private static boolean isDirectory( ZipEntry zipEntry ) {
        String name = zipEntry.getName();
        return name.endsWith( "/" ) || name.endsWith( "\\" );
    }

    public static void main( String[] args ) throws IOException {
        var destinationDir = Files.createTempDirectory( "logfx-tmp" ).toFile();
        var updatePath = Properties.LOGFX_DIR.resolve( LOGFX_UPDATE_ZIP );
        replaceSelf( destinationDir, updatePath.toFile() );
        System.out.print( destinationDir.getAbsolutePath() );
    }
}
