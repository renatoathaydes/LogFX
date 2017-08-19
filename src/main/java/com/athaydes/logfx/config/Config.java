package com.athaydes.logfx.config;

import com.athaydes.logfx.text.HighlightExpression;
import com.athaydes.logfx.ui.Dialog;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.PatternSyntaxException;

public class Config {

    private static final Logger log = LoggerFactory.getLogger( Config.class );

    private final Path path;
    private final ObservableList<HighlightExpression> observableExpressions;

    public Config( Path path ) {
        this.path = path;
        observableExpressions = FXCollections.observableArrayList();
        if ( path.toFile().exists() ) {
            readConfigFile( path );
        } else {
            observableExpressions.add( new HighlightExpression( ".*WARN.*", Color.YELLOW, Color.RED ) );
        }

        // the last item must always be the 'catch-all' item
        observableExpressions.add( new HighlightExpression( ".*", Color.BLACK, Color.LIGHTGREY ) );

        log.debug( "Listening to changes on list" );
        observableExpressions.addListener( ( InvalidationListener ) event -> {
            dumpConfigToFile();
        } );
    }

    public ObservableList<HighlightExpression> getObservableExpressions() {
        return observableExpressions;
    }

    private void readConfigFile( Path path ) {
        try {
            Iterator<String> lines = Files.readAllLines( path ).iterator();
            while ( lines.hasNext() ) {
                String line = lines.next();
                switch ( line.trim() ) {
                    case "expressions:":
                        observableExpressions.addAll( parseExpressions( lines ) );
                }
            }
        } catch ( IOException e ) {
            Dialog.showConfirmDialog( "Could not read config file: " + path +
                    "\n\n" + e );
        }
    }

    private List<HighlightExpression> parseExpressions( Iterator<String> lines ) {
        List<HighlightExpression> result = new ArrayList<>();
        while ( lines.hasNext() ) {
            String line = lines.next();
            if ( line.startsWith( " " ) ) {
                result.add( parseHighlightExpression( line.trim() ) );
            } else {
                break;
            }
        }
        return result;
    }

    private void dumpConfigToFile() {
        log.debug( "Writing config to " + path );
        try ( FileWriter writer = new FileWriter( path.toFile() ) ) {
            writer.write( "expressions:\n" );
            for ( HighlightExpression expression : observableExpressions.subList( 0, observableExpressions.size() - 1 ) ) {
                writer.write( "  " + expression.getBkgColor() );
                writer.write( " " + expression.getFillColor() );
                writer.write( " " + expression.getPattern().pattern() );
                writer.write( "\n" );
            }
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
