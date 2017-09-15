package com.athaydes.logfx.config;

import com.athaydes.logfx.binding.BindableValue;
import com.athaydes.logfx.concurrency.TaskRunner;
import com.athaydes.logfx.text.HighlightExpression;
import com.athaydes.logfx.ui.Dialog;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
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
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class Config {

    private static final Logger log = LoggerFactory.getLogger( Config.class );

    private final Path path;
    private final ObservableList<HighlightExpression> observableExpressions;
    private final ObservableSet<File> observableFiles;
    private final SimpleObjectProperty<Orientation> panesOrientation;
    private final ObservableList<Double> paneDividerPositions;
    private final BindableValue<Font> font;

    public Config( Path path, TaskRunner taskRunner, BindableValue<Font> fontValue ) {
        this.path = path;
        this.font = fontValue;
        observableExpressions = FXCollections.observableArrayList();
        observableFiles = FXCollections.observableSet( new LinkedHashSet<>( 4 ) );
        panesOrientation = new SimpleObjectProperty<>( Orientation.HORIZONTAL );
        paneDividerPositions = FXCollections.observableArrayList();

        if ( path.toFile().exists() ) {
            readConfigFile( path );
        } else {
            observableExpressions.add( new HighlightExpression( "WARN", Color.YELLOW, Color.RED ) );
        }

        // the last item must always be the 'catch-all' item
        observableExpressions.add( new HighlightExpression( "", Color.BLACK, Color.LIGHTGREY ) );

        // make this a singleton object so that it can be remembered when we try to run it many times below
        final Runnable updateConfigFile = this::dumpConfigToFile;

        log.debug( "Listening to changes on observable Lists" );

        InvalidationListener listener = ( event ) ->
                taskRunner.runWithMaxFrequency( updateConfigFile, 2000L );

        observableExpressions.addListener( listener );
        observableFiles.addListener( listener );
        panesOrientation.addListener( listener );
        paneDividerPositions.addListener( listener );
        font.addListener( listener );
    }

    public ObservableList<HighlightExpression> getObservableExpressions() {
        return observableExpressions;
    }

    public ObservableSet<File> getObservableFiles() {
        return observableFiles;
    }

    public SimpleObjectProperty<Orientation> panesOrientationProperty() {
        return panesOrientation;
    }

    public ObservableList<Double> getPaneDividerPositions() {
        return paneDividerPositions;
    }

    private void readConfigFile( Path path ) {
        try {
            Iterator<String> lines = Files.lines( path ).iterator();
            parseConfigFile( null, lines );
        } catch ( IOException e ) {
            Dialog.showConfirmDialog( "Could not read config file: " + path +
                    "\n\n" + e );
        }
    }

    private void parseConfigFile( String currentLine, Iterator<String> lines ) {
        String line;
        if ( currentLine != null ) {
            line = currentLine;
        } else if ( lines.hasNext() ) {
            line = lines.next();
        } else {
            return; // no lines left
        }

        switch ( line.trim() ) {
            case "expressions:":
                parseExpressions( lines );
            case "files:":
                parseFiles( lines );
            case "gui:":
                parseGuiSection( lines );
        }
    }

    private void parseExpressions( Iterator<String> lines ) {
        while ( lines.hasNext() ) {
            String line = lines.next();
            if ( line.startsWith( " " ) ) {
                try {
                    observableExpressions.add( parseHighlightExpression( line.trim() ) );
                } catch ( IllegalArgumentException e ) {
                    logInvalidProperty( "expressions", "highlight", line, e.toString() );
                }
            } else if ( !line.trim().isEmpty() ) {
                parseConfigFile( line, lines );
                break;
            }
        }
    }

    private void parseFiles( Iterator<String> lines ) {
        while ( lines.hasNext() ) {
            String line = lines.next();
            if ( line.startsWith( " " ) ) {
                observableFiles.add( new File( line.trim() ) );
            } else if ( !line.trim().isEmpty() ) {
                parseConfigFile( line, lines );
                break;
            }
        }
    }

    private void parseGuiSection( Iterator<String> lines ) {
        while ( lines.hasNext() ) {
            String line = lines.next();
            if ( line.startsWith( " " ) ) {
                String[] parts = line.trim().split( "\\s+" );
                if ( parts.length == 0 ) {
                    logInvalidProperty( "gui", "?", "empty line", null );
                } else switch ( parts[ 0 ] ) {
                    case "orientation":
                        if ( parts.length != 2 ) {
                            logInvalidProperty( "gui", "orientation", line,
                                    "Expected 2 parts, got " + parts.length );
                        } else try {
                            panesOrientation.set( Orientation.valueOf( parts[ 1 ] ) );
                        } catch ( IllegalArgumentException e ) {
                            logInvalidProperty( "gui", "orientation", parts[ 1 ],
                                    "Invalid value for Orientation: " + e );
                        }
                        break;
                    case "pane-dividers":
                        if ( parts.length != 2 ) {
                            logInvalidProperty( "gui", "pane-dividers", line,
                                    "Expected 2 parts, got " + parts.length );
                        } else try {
                            String[] separators = parts[ 1 ].split( "," );
                            paneDividerPositions.addAll( toDoubles( separators ) );
                        } catch ( IllegalArgumentException e ) {
                            logInvalidProperty( "gui", "pane-dividers", parts[ 1 ],
                                    e.toString() );
                        }
                        break;
                    case "font":
                        if ( parts.length < 3 ) {
                            logInvalidProperty( "gui", "font", line,
                                    "Expected 3 or more parts, got " + parts.length );
                        } else {
                            double size = 12.0;
                            try {
                                size = Double.parseDouble( parts[ 1 ] );
                            } catch ( NumberFormatException e ) {
                                logInvalidProperty( "gui", "font", parts[ 1 ],
                                        "Font size is invalid: " + parts[ 1 ] );
                            }

                            String[] fontNameParts = Arrays.copyOfRange( parts, 2, parts.length );
                            String fontName = String.join( " ", fontNameParts );
                            try {
                                font.setValue( Font.font( fontName, size ) );
                            } catch ( IllegalArgumentException e ) {
                                logInvalidProperty( "gui", "font", parts[ 0 ],
                                        "Invalid font name: " + fontName );
                            }
                        }
                        break;
                }
            } else if ( !line.trim().isEmpty() ) {
                parseConfigFile( line, lines );
                break;
            }
        }
    }

    private static List<Double> toDoubles( String[] numbers ) {
        return Stream.of( numbers )
                .map( String::trim )
                .map( Double::parseDouble )
                .collect( toList() );
    }

    private static void logInvalidProperty( String section, String name,
                                            String invalidValue, String error ) {
        log.warn( "Invalid value for {} in section {}: '{}'",
                name, section, invalidValue );
        if ( error != null ) {
            log.warn( "Error: {}", error );
        }
    }

    /**
     * The properties are all set in the JavaFX Thread, therefore we need to make copies of everything
     * in the JavaFX Thread before being able to safely use them in another Thread,
     * where we write the config file.
     */
    private void dumpConfigToFile() {
        CompletableFuture<List<HighlightExpression>> expressionsFuture = new CompletableFuture<>();
        Platform.runLater( () -> expressionsFuture.complete( new ArrayList<>(
                observableExpressions.subList( 0, observableExpressions.size() - 1 ) ) ) );

        CompletableFuture<Set<File>> filesFuture = new CompletableFuture<>();
        Platform.runLater( () -> filesFuture.complete( new LinkedHashSet<>( observableFiles ) ) );

        CompletableFuture<Orientation> panesOrientationFuture = new CompletableFuture<>();
        Platform.runLater( () -> panesOrientationFuture.complete( panesOrientation.get() ) );

        CompletableFuture<List<Double>> paneDividersFuture = new CompletableFuture<>();
        Platform.runLater( () -> paneDividersFuture.complete( new ArrayList<>( paneDividerPositions ) ) );

        CompletableFuture<Font> fontFuture = new CompletableFuture<>();
        Platform.runLater( () -> fontFuture.complete( font.getValue() ) );

        expressionsFuture.thenAccept( expressions ->
                filesFuture.thenAccept( files ->
                        panesOrientationFuture.thenAccept( orientation ->
                                paneDividersFuture.thenAccept( dividers ->
                                        fontFuture.thenAccept( font ->
                                                dumpConfigToFile(
                                                        expressions, files, orientation,
                                                        dividers, font, path.toFile() ) ) ) ) ) );
    }

    private static void dumpConfigToFile( List<HighlightExpression> highlightExpressions,
                                          Set<File> files,
                                          Orientation orientation,
                                          List<Double> dividerPositions,
                                          Font font,
                                          File path ) {
        log.debug( "Writing config to {}", path );

        try ( FileWriter writer = new FileWriter( path ) ) {
            writer.write( "expressions:\n" );
            for ( HighlightExpression expression : highlightExpressions ) {
                writer.write( "  " + expression.getBkgColor() );
                writer.write( " " + expression.getFillColor() );
                writer.write( " " + expression.getPattern().pattern() );
                writer.write( "\n" );
            }

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

    protected static HighlightExpression parseHighlightExpression( String line )
            throws IllegalArgumentException {
        if ( line.isEmpty() ) {
            throw highlightParseError( "empty highlight expression", line );
        }
        if ( line.startsWith( " " ) || line.endsWith( " " ) ) {
            throw highlightParseError( "highlight expression contains invalid spaces at start/end", line );
        }

        int firstSpaceIndex = line.indexOf( ' ' );
        if ( firstSpaceIndex <= 0 ) {
            throw highlightParseError( "fill color and regular expression not specified", line );
        }
        String bkgColorString = line.substring( 0, firstSpaceIndex );

        Color bkgColor;
        try {
            bkgColor = Color.valueOf( bkgColorString );
        } catch ( IllegalArgumentException e ) {
            throw highlightParseError( "invalid background color: '" + bkgColorString + "'", line );
        }

        if ( line.length() > firstSpaceIndex + 1 ) {
            line = line.substring( firstSpaceIndex + 1 ).trim();
        } else {
            throw highlightParseError( "fill color and regular expression not specified", line );
        }

        int secondSpaceIndex = line.indexOf( ' ' );
        if ( secondSpaceIndex <= 0 ) {
            throw highlightParseError( "regular expression not specified", line );
        }

        String fillColorString = line.substring( 0, secondSpaceIndex );
        Color fillColor;
        try {
            fillColor = Color.valueOf( fillColorString );
        } catch ( IllegalArgumentException e ) {
            throw highlightParseError( "invalid fill color: '" + fillColorString + "'", line );
        }

        if ( line.length() > secondSpaceIndex + 1 ) {
            line = line.substring( secondSpaceIndex + 1 ).trim();
        } else {
            line = "";
        }

        if ( line.isEmpty() ) {
            throw highlightParseError( "regular expression not specified", line );
        }

        String expression = line;
        try {
            return new HighlightExpression( expression, bkgColor, fillColor );
        } catch ( PatternSyntaxException e ) {
            throw highlightParseError( "cannot parse regular expression '" + expression + "'", line );
        }
    }

    private static IllegalArgumentException highlightParseError( String error, String line ) {
        return new IllegalArgumentException( "Invalid highlight expression [" + error + "]: " + line );
    }


}
