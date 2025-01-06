package com.athaydes.logfx.config;

import com.athaydes.logfx.binding.BindableValue;
import com.athaydes.logfx.concurrency.TaskRunner;
import com.athaydes.logfx.data.LogFile;
import com.athaydes.logfx.data.LogLineColors;
import com.athaydes.logfx.text.HighlightExpression;
import com.athaydes.logfx.ui.Dialog;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.geometry.Bounds;
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
import java.util.*;

import static java.util.stream.Collectors.joining;

public class Config {

    private static final Logger log = LoggerFactory.getLogger( Config.class );

    private Path path;
    private final ConfigProperties properties;
    private final List<Runnable> reloadActions = new ArrayList<>( 2 );

    public Config( Path path, TaskRunner taskRunner ) {
        this.path = path;
        this.properties = new ConfigProperties();

        if ( path.toFile().exists() ) {
            readConfigFile();
        } else {
            properties.highlightGroups.getDefault()
                    .add( new HighlightExpression( "WARN", Color.YELLOW, Color.RED, false ) );
        }

        // make this a singleton object so that it can be remembered when we try to run it many times below
        final Runnable updateConfigFile = () -> dumpConfigToFile( taskRunner );

        log.debug( "Listening to changes on observable Lists" );

        InvalidationListener listener = ( event ) ->
                taskRunner.runWithMaxFrequency( updateConfigFile, 2000L, 1000L );

        properties.standardLogColors.addListener( listener );
        properties.highlightGroups.setListener( listener );
        properties.onLogFileChange.addListener( listener );
        properties.observableFiles.addListener( listener );
        properties.panesOrientation.addListener( listener );
        properties.windowBounds.addListener( listener );
        properties.paneDividerPositions.addListener( listener );
        properties.font.addListener( listener );
        properties.enableFilters.addListener( listener );
        properties.displayTimeGaps.addListener( listener );

        // keep track of logFiles's groups
        properties.observableFiles.forEach( f -> f.highlightGroupProperty().addListener( listener ) );

        properties.observableFiles.addListener( ( SetChangeListener<LogFile> ) change -> {
            if ( change.wasAdded() ) {
                change.getElementAdded().highlightGroupProperty().addListener( listener );
            }
            if ( change.wasRemoved() ) {
                change.getElementRemoved().highlightGroupProperty().removeListener( listener );
            }
        } );
    }

    public SimpleObjectProperty<LogLineColors> standardLogColorsProperty() {
        return properties.standardLogColors;
    }

    public HighlightGroups getHighlightGroups() {
        return properties.highlightGroups;
    }

    public ObservableSet<LogFile> getObservableFiles() {
        return properties.observableFiles;
    }

    public SimpleObjectProperty<Orientation> panesOrientationProperty() {
        return properties.panesOrientation;
    }

    public SimpleObjectProperty<Bounds> windowBoundsProperty() {
        return properties.windowBounds;
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

    public BooleanProperty displayTimeGapsProperty() {
        return properties.displayTimeGaps;
    }

    public void loadConfig( Path path ) {
        if ( path.toFile().isFile() ) {
            this.path = path;
            log.info( "Clearing LogFX properties" );
            properties.clear();
            log.info( "Loading config file: {}", path );
            readConfigFile();
            for ( Runnable reloadAction : reloadActions ) {
                reloadAction.run();
            }
        } else {
            var message = "Config file is not a file: " + path;
            log.info( message );
            Dialog.showMessage( message + "\n\n", Dialog.MessageLevel.WARNING );
        }
    }

    public Path getCurrentConfigPath() {
        return path;
    }

    private void readConfigFile() {
        try ( var fileLines = Files.lines( path ) ) {
            Iterator<String> lines = fileLines.iterator();
            new ConfigParser( properties ).parseConfigFile( lines );
        } catch ( Exception e ) {
            log.warn( "Error loading config", e );
            Dialog.showMessage( "Could not read config file: " + path +
                    "\n\n" + e, Dialog.MessageLevel.WARNING );
        }
    }

    /**
     * Add an action to run when config is loaded. This happens when the user changes which project is open.
     *
     * @param reloadAction action to run on config reload
     */
    public void onReload( Runnable reloadAction ) {
        reloadActions.add( reloadAction );
    }

    /**
     * The properties are all set in the JavaFX Thread, therefore we need to make copies of everything
     * in the JavaFX Thread before being able to safely use them in another Thread,
     * where we write the config file.
     *
     * @param taskRunner runner
     */
    private void dumpConfigToFile( TaskRunner taskRunner ) {
        var data = new ConfigData();
        data.path = path.toFile();

        Platform.runLater( () -> data.logLineColors = properties.standardLogColors.get() );
        Platform.runLater( () -> data.highlightExpressions = new HashMap<>( properties.highlightGroups.toMap() ) );
        Platform.runLater( () -> data.enableFilters = properties.enableFilters.getValue() );
        Platform.runLater( () -> data.displayTimeGaps = properties.displayTimeGaps.getValue() );
        Platform.runLater( () -> data.files = new LinkedHashSet<>( properties.observableFiles ) );
        Platform.runLater( () -> data.orientation = properties.panesOrientation.get() );
        Platform.runLater( () -> data.windowBounds = properties.windowBounds.get() );
        Platform.runLater( () -> data.dividerPositions = new ArrayList<>( properties.paneDividerPositions ) );
        Platform.runLater( () -> data.font = properties.font.getValue() );

        // go to the JavaFX Thread to wait for all previous tasks to complete, then dump the file, finally.
        Platform.runLater( () -> taskRunner.runAsync( () -> dumpConfigToFile( data ) ) );
    }

    private static void dumpConfigToFile( ConfigData data ) {
        log.debug( "Writing config to {}", data.path );

        try ( FileWriter writer = new FileWriter( data.path ) ) {

            var versions = ConfigParser.ConfigVersion.values();
            writer.write( "version:\n  " );
            writer.write( versions[ versions.length - 1 ].name() );
            writer.write( "\nstandard-log-colors:\n" );
            writer.write( "  " + data.logLineColors.getBackground() + " " + data.logLineColors.getFill() );
            writer.write( "\n" );

            for ( Map.Entry<String, Collection<HighlightExpression>> entry : data.highlightExpressions.entrySet() ) {
                String group = entry.getKey();
                writer.write( "expressions:\n" );
                if ( !group.isEmpty() ) {
                    writer.write( "  @name@" );
                    writer.write( group );
                    writer.write( '\n' );
                }
                for ( HighlightExpression expression : entry.getValue() ) {
                    writer.write( "  " + expression.getBkgColor() );
                    writer.write( " " + expression.getFillColor() );
                    writer.write( " " + expression.isFiltered() );
                    writer.write( " " + expression.getPattern().pattern() );
                    writer.write( "\n" );
                }
            }

            writer.write( "filters:\n  " );
            writer.write( data.enableFilters ? "enable\n" : "disable\n" );

            writer.write( "time-gaps:\n  " );
            writer.write( data.displayTimeGaps ? "enable\n" : "disable\n" );

            writer.write( "auto_update:\n  " );
            writer.write( data.autoUpdate ? "enable\n" : "disable\n" );

            writer.write( "files:\n" );
            for ( LogFile file : data.files ) {
                String group = file.getHighlightGroup();
                writer.write( "  " );
                if ( !group.isEmpty() ) {
                    writer.write( '[' );
                    writer.write( group );
                    writer.write( ']' );
                }
                if ( file.minTimeGap.get() != LogFile.DEFAULT_MIN_TIME_GAP ) {
                    writer.write( file.minTimeGap.get() + "," );
                }
                writer.write( file.file.getAbsolutePath() );
                writer.write( "\n" );
            }

            writer.write( "gui:\n" );
            writer.write( "  orientation " + data.orientation.name() );
            if ( data.windowBounds != null ) {
                writer.write( String.format( Locale.US, "%n  window %.1f %.1f %.1f %.1f",
                        data.windowBounds.getMinX(), data.windowBounds.getMinY(),
                        data.windowBounds.getWidth(), data.windowBounds.getHeight() ) );
            }
            if ( !data.dividerPositions.isEmpty() ) {
                writer.write( "\n  pane-dividers " );
                writer.write( data.dividerPositions.stream()
                        .map( n -> String.format( Locale.US, "%.2f", n ) )
                        .collect( joining( "," ) ) );
            }
            writer.write( "\n  font " + data.font.getSize() );
            writer.write( " " + data.font.getFamily() );

            writer.write( "\n" );
        } catch ( IOException e ) {
            Dialog.showMessage( "Could not write config file: " + data.path +
                    "\n\n" + e, Dialog.MessageLevel.ERROR );
        }
    }

    private static final class ConfigData {
        volatile LogLineColors logLineColors;
        volatile Map<String, Collection<HighlightExpression>> highlightExpressions;
        volatile boolean enableFilters;
        volatile boolean displayTimeGaps;
        volatile boolean autoUpdate;
        volatile Set<LogFile> files;
        volatile Orientation orientation;
        volatile Bounds windowBounds;
        volatile List<Double> dividerPositions;
        volatile Font font;
        volatile File path;
    }
}
