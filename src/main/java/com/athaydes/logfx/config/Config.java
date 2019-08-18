package com.athaydes.logfx.config;

import com.athaydes.logfx.binding.BindableValue;
import com.athaydes.logfx.concurrency.TaskRunner;
import com.athaydes.logfx.data.LogLineColors;
import com.athaydes.logfx.text.HighlightExpression;
import com.athaydes.logfx.ui.Dialog;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.geometry.Orientation;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.joining;

public class Config {

    private static final Logger log = LoggerFactory.getLogger( Config.class );

    private final Path path;
    private final ConfigProperties properties;

    public Config( Path path, TaskRunner taskRunner ) {
        this.path = path;
        this.properties = new ConfigProperties();

        if ( path.toFile().exists() ) {
            readConfigFile( path );
        } else {
            properties.observableExpressions.add( new HighlightExpression( "WARN", Color.YELLOW, Color.RED, false ) );
        }

        // make this a singleton object so that it can be remembered when we try to run it many times below
        final Runnable updateConfigFile = this::dumpConfigToFile;

        log.debug( "Listening to changes on observable Lists" );

        InvalidationListener listener = ( event ) ->
                taskRunner.runWithMaxFrequency( updateConfigFile, 2000L );

        properties.standardLogColors.addListener( listener );
        properties.observableExpressions.addListener( listener );
        properties.observableFiles.addListener( listener );
        properties.panesOrientation.addListener( listener );
        properties.paneDividerPositions.addListener( listener );
        properties.font.addListener( listener );
        properties.enableFilters.addListener( listener );
    }

    public SimpleObjectProperty<LogLineColors> standardLogColorsProperty() {
        return properties.standardLogColors;
    }

    public ObservableList<HighlightExpression> getObservableExpressions() {
        return properties.observableExpressions;
    }

    public ObservableSet<File> getObservableFiles() {
        return properties.observableFiles;
    }

    public SimpleObjectProperty<Orientation> panesOrientationProperty() {
        return properties.panesOrientation;
    }

    public ObservableList<Double> getPaneDividerPositions() {
        return properties.paneDividerPositions;
    }

    public BindableValue<Font> fontProperty() {
        return properties.font;
    }

    public BooleanProperty filtersEnabledProperty() {
        return properties.enableFilters;
    }

    private void readConfigFile( Path path ) {
        try {
            Iterator<String> lines = Files.lines( path ).iterator();
            new ConfigParser( properties ).parseConfigFile( null, lines );
        } catch ( Exception e ) {
            log.warn( "Error loading config", e );
            Dialog.showConfirmDialog( "Could not read config file: " + path +
                    "\n\n" + e );
        }
    }

    /**
     * The properties are all set in the JavaFX Thread, therefore we need to make copies of everything
     * in the JavaFX Thread before being able to safely use them in another Thread,
     * where we write the config file.
     */
    private void dumpConfigToFile() {
        CompletableFuture<LogLineColors> standardLogColorsFuture = new CompletableFuture<>();
        Platform.runLater( () -> standardLogColorsFuture.complete( properties.standardLogColors.get() ) );

        CompletableFuture<List<HighlightExpression>> expressionsFuture = new CompletableFuture<>();
        Platform.runLater( () -> expressionsFuture.complete( new ArrayList<>( properties.observableExpressions ) ) );

        CompletableFuture<Boolean> enableFiltersFuture = new CompletableFuture<>();
        Platform.runLater( () -> enableFiltersFuture.complete( properties.enableFilters.getValue() ) );

        CompletableFuture<Set<File>> filesFuture = new CompletableFuture<>();
        Platform.runLater( () -> filesFuture.complete( new LinkedHashSet<>( properties.observableFiles ) ) );

        CompletableFuture<Orientation> panesOrientationFuture = new CompletableFuture<>();
        Platform.runLater( () -> panesOrientationFuture.complete( properties.panesOrientation.get() ) );

        CompletableFuture<List<Double>> paneDividersFuture = new CompletableFuture<>();
        Platform.runLater( () -> paneDividersFuture.complete( new ArrayList<>( properties.paneDividerPositions ) ) );

        CompletableFuture<Font> fontFuture = new CompletableFuture<>();
        Platform.runLater( () -> fontFuture.complete( properties.font.getValue() ) );


        standardLogColorsFuture.thenAccept( logLineColors ->
                expressionsFuture.thenAccept( expressions ->
                        enableFiltersFuture.thenAccept( enableFilters ->
                                filesFuture.thenAccept( files ->
                                        panesOrientationFuture.thenAccept( orientation ->
                                                paneDividersFuture.thenAccept( dividers ->
                                                        fontFuture.thenAccept( font ->
                                                                dumpConfigToFile(
                                                                        logLineColors, expressions, enableFilters,
                                                                        files, orientation, dividers, font, path.toFile()
                                                                ) ) ) ) ) ) ) );
    }

    private static void dumpConfigToFile( LogLineColors logLineColors,
                                          List<HighlightExpression> highlightExpressions,
                                          boolean enableFilters,
                                          Set<File> files,
                                          Orientation orientation,
                                          List<Double> dividerPositions,
                                          Font font,
                                          File path ) {
        log.debug( "Writing config to {}", path );

        try ( FileWriter writer = new FileWriter( path ) ) {

            writer.write( "version:\n  " );
            writer.write( ConfigParser.ConfigVersion.V2.name() );
            writer.write( "\nstandard-log-colors:\n" );
            writer.write( "  " + logLineColors.getBackground() + " " + logLineColors.getFill() );
            writer.write( "\n" );

            if ( !highlightExpressions.isEmpty() ) {
                writer.write( "expressions:\n" );
                for ( HighlightExpression expression : highlightExpressions ) {
                    writer.write( "  " + expression.getBkgColor() );
                    writer.write( " " + expression.getFillColor() );
                    writer.write( " " + expression.isFiltered() );
                    writer.write( " " + expression.getPattern().pattern() );
                    writer.write( "\n" );
                }
            }

            writer.write( "filters:\n  " );
            writer.write( enableFilters ? "enable\n" : "disable\n" );

            writer.write( "files:\n" );
            for ( File file : files ) {
                writer.write( "  " + file.getAbsolutePath() );
                writer.write( "\n" );
            }

            writer.write( "gui:\n" );
            writer.write( "  orientation " + orientation.name() );
            if ( !dividerPositions.isEmpty() ) {
                writer.write( "\n  pane-dividers " );
                writer.write( dividerPositions.stream()
                        .map( n -> Double.toString( n ) )
                        .collect( joining( "," ) ) );
            }
            writer.write( "\n  font " + font.getSize() );
            writer.write( " " + font.getFamily() );

            writer.write( "\n" );
        } catch ( IOException e ) {
            Dialog.showConfirmDialog( "Could not write config file: " + path +
                    "\n\n" + e );
        }
    }

}
