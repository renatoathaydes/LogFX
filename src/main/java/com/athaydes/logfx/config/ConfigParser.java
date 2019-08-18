package com.athaydes.logfx.config;

import com.athaydes.logfx.data.LogLineColors;
import com.athaydes.logfx.text.HighlightExpression;
import javafx.geometry.Orientation;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

final class ConfigParser {

    private static final Logger log = LoggerFactory.getLogger( ConfigParser.class );

    private final ConfigProperties properties;

    ConfigParser( ConfigProperties properties ) {
        this.properties = properties;
    }

    void parseConfigFile( String currentLine, Iterator<String> lines ) {
        String line;
        if ( currentLine != null ) {
            line = currentLine;
        } else if ( lines.hasNext() ) {
            line = lines.next();
        } else {
            return; // no lines left
        }

        switch ( line.trim() ) {
            case "standard-log-colors:":
                parseStandardLogColors( lines );
            case "expressions:":
                parseExpressions( lines );
            case "files:":
                parseFiles( lines );
            case "gui:":
                parseGuiSection( lines );
        }
    }

    private void parseStandardLogColors( Iterator<String> lines ) {
        while ( lines.hasNext() ) {
            String line = lines.next();
            if ( line.startsWith( " " ) ) {
                String[] components = line.trim().split( " " );
                if ( components.length == 2 ) {
                    try {
                        Color bkg = Color.valueOf( components[ 0 ] );
                        Color fill = Color.valueOf( components[ 1 ] );
                        properties.standardLogColors.set( new LogLineColors( bkg, fill ) );
                    } catch ( IllegalArgumentException e ) {
                        logInvalidProperty( "standard-log-colors", "color", line, e.toString() );
                    }
                } else {
                    logInvalidProperty( "standard-log-colors", "number of colors", line, null );
                }
            } else if ( !line.trim().isEmpty() ) {
                parseConfigFile( line, lines );
                break;
            }
        }
    }

    private void parseExpressions( Iterator<String> lines ) {
        while ( lines.hasNext() ) {
            String line = lines.next();
            if ( line.startsWith( " " ) ) {
                try {
                    properties.observableExpressions.add( parseHighlightExpression( line.trim() ) );
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
                properties.observableFiles.add( new File( line.trim() ) );
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
                            properties.panesOrientation.set( Orientation.valueOf( parts[ 1 ] ) );
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
                            properties.paneDividerPositions.addAll( toDoubles( separators ) );
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
                                properties.font.setValue( Font.font( fontName, size ) );
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

    static HighlightExpression parseHighlightExpression( String line )
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

        // TODO add to config
        boolean isFiltered = false;

        String expression = line;
        try {
            return new HighlightExpression( expression, bkgColor, fillColor, isFiltered );
        } catch ( PatternSyntaxException e ) {
            throw highlightParseError( "cannot parse regular expression '" + expression + "'", line );
        }
    }

    private static IllegalArgumentException highlightParseError( String error, String line ) {
        return new IllegalArgumentException( "Invalid highlight expression [" + error + "]: " + line );
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
}