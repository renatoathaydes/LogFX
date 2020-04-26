package com.athaydes.logfx.update;

import com.athaydes.keepup.api.AppDistributor;
import com.athaydes.keepup.api.KeepupConfig;
import com.athaydes.keepup.github.GitHubAppDistributor;
import com.athaydes.keepup.github.GitHubAsset;
import com.athaydes.keepup.github.GitHubResponse;
import com.athaydes.logfx.ui.FxUtils;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LogFXKeepupConfig implements KeepupConfig {

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
        return new GitHubAppDistributor( "6d8c675e14b885e57d336b1c5f867e73b51476c5", "renatoathaydes", "LogFX",
                3, acceptVersion, this::selectAsset );
    }

    private GitHubAsset selectAsset( GitHubResponse response ) {
        var os = FxUtils.getOs().toLowerCase();
        LoggerFactory.getLogger( LogFXUpdater.class ).info( "GitHub response: version={}, assets={}",
                response.getLatestVersion(),
                response.getAssets().stream()
                        .map( GitHubAsset::getName )
                        .collect( Collectors.joining( ", " ) ) );

        return response.getAssets().stream()
                .filter( asset -> asset.getName().contains( os ) )
                .findFirst().orElseThrow( () -> new RuntimeException( "Unable to idenfity GitHub asset" ) );
    }

    @Override
    public ExecutorService executor() {
        return executor;
    }
}
