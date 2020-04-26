package com.athaydes.logfx.update;

import com.athaydes.keepup.api.Keepup;
import com.athaydes.keepup.api.UpgradeInstaller;
import com.athaydes.logfx.concurrency.TaskRunner;
import com.athaydes.logfx.config.Properties;
import com.athaydes.logfx.ui.Dialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static com.athaydes.logfx.Constants.LOGFX_VERSION;
import static com.athaydes.logfx.ui.Dialog.MessageLevel.ERROR;

public final class LogFXUpdater {
    private static final Logger log = LoggerFactory.getLogger( LogFXUpdater.class );

    private static final String LOGFX_UPDATE_CHECK = "logfx-update-check";

    private static boolean shouldCheckForUpdates() {
        Path updatesPath = Properties.LOGFX_DIR.resolve( LOGFX_UPDATE_CHECK );
        File updatesFile = updatesPath.toFile();
        if ( updatesFile.exists() ) {
            var lastCheck = Instant.ofEpochMilli( updatesFile.lastModified() );
            var now = Instant.now();
            var diff = Duration.between( lastCheck, now );
            var minDiffBetweenChecks = Duration.ofSeconds( Properties.UPDATE_CHECK_PERIOD_SECONDS );
            var willCheck = diff.compareTo( minDiffBetweenChecks ) > 0;
            if ( log.isDebugEnabled() ) {
                log.debug( "Last update check at {}, currently {}, diff = {} seconds ({})",
                        lastCheck, now, diff.getSeconds(),
                        willCheck ?
                                "will check for updates as the minimum period is " + minDiffBetweenChecks.getSeconds() :
                                "next check will be after " + lastCheck.plus( minDiffBetweenChecks ) );
            }
            if ( willCheck ) {
                try {
                    Files.setLastModifiedTime( updatesPath, FileTime.from( now ) );
                } catch ( IOException e ) {
                    log.warn( "Could not update logfx-update-check file timestamp" );
                }
            }
            return willCheck;
        } else {
            log.info( "Creating {}.", updatesPath );
            try {
                Files.createFile( updatesPath );
            } catch ( IOException e ) {
                log.warn( "Could not create " + updatesPath, e );
            }
            return false;
        }
    }

    private static boolean isRejectedVersion( String newVersion ) {
        Path updatesPath = Properties.LOGFX_DIR.resolve( LOGFX_UPDATE_CHECK );
        if ( !updatesPath.toFile().isFile() ) {
            return false;
        }
        try {
            var rejected = Files.readAllLines( updatesPath, StandardCharsets.UTF_8 ).contains( newVersion );
            if ( rejected ) {
                log.info( "Version {} has been rejected", newVersion );
            }
            return rejected;
        } catch ( IOException e ) {
            log.warn( "Unable to read updates file", e );
            return false;
        }
    }

    private static Function<String, CompletionStage<Boolean>> askUserWhetherToUpdate() {
        return ( newVersion ) -> {
            log.info( "Latest LogFX version is {}, current version is {}", newVersion, LOGFX_VERSION );
            if ( !LOGFX_VERSION.equals( newVersion ) && !isRejectedVersion( newVersion ) ) {
                var future = new CompletableFuture<Boolean>();
                Dialog.askForDecision( "A new LogFX version is available: " + newVersion + ".\n" +
                        "Would you like to update?", Map.of(
                        "Yes", () -> future.complete( true ),
                        "No, skip this version", () -> {
                            Path updatesPath = Properties.LOGFX_DIR.resolve( LOGFX_UPDATE_CHECK );
                            try {
                                Files.writeString( updatesPath, newVersion + "\n",
                                        StandardOpenOption.CREATE, StandardOpenOption.APPEND );
                                log.info( "Permanently skipped update to version {}", newVersion );
                            } catch ( IOException e ) {
                                log.warn( "Unable to write to updates file", e );
                            }
                            future.complete( false );
                        },
                        "Ask later", () -> {
                            log.debug( "Will ask to update again later" );
                            future.complete( false );
                        }
                ) );
                return future;
            } else {
                return CompletableFuture.completedFuture( false );
            }
        };
    }

    public static void checkAndDownloadUpdateIfAvailable( TaskRunner taskRunner ) {
        if ( shouldCheckForUpdates() ) {
            var keepup = new Keepup( new LogFXKeepupConfig(
                    taskRunner.getExecutor(),
                    askUserWhetherToUpdate() ) );
            keepup.onNoUpdateRequired( () -> log.info( "No update available" ) )
                    .onError( ( error ) -> {
                        log.warn( "Problem updating LogFX", error );
                        Dialog.showMessage( "Problem updating LogFX: " + error, ERROR );
                    } )
                    .onDone( Keepup.NO_OP, LogFXUpdater::askUserToInstallNewVersion )
                    .createUpdater().checkForUpdate();
        } else {
            log.debug( "Will not check for newer LogFX versions" );
        }
    }

    private static void askUserToInstallNewVersion( UpgradeInstaller installer ) {
        log.info( "Update obtained successfully." );
        Dialog.askForDecision( "LogFX has been updated!\nWhat would you like to do?", Map.of(
                "Restart LogFX", installer::quitAndLaunchUpdatedApp,
                "I will restart later", installer::installUpdateOnExit ) );
    }
}
