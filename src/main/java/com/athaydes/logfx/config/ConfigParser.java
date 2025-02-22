package com.athaydes.logfx.config;

import com.athaydes.logfx.data.LogFile;
import com.athaydes.logfx.data.LogLineColors;
import com.athaydes.logfx.text.HighlightExpression;
import com.athaydes.logfx.ui.FileOpener;
import javafx.geometry.BoundingBox;
import javafx.geometry.Orientation;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

final class ConfigParser {

    private static final Pattern FILE_LINE_PATTERN_V3 = Pattern.compile(
            "\\s+(\\[(?<group>.*)])?\\s*(?<name>.+)\\s*" );

    private static final Pattern FILE_LINE_PATTERN_V4 = Pattern.compile(
            "\\s+(\\[(?<group>.*)])?" +
                    "(?<timegap>\\d+,)?\\s*" +
                    "(?<name>.+)\\s*" );

    enum ConfigVersion {
        V1, V2, V3, V4;

        public boolean isAfter( ConfigVersion version ) {
            return ordinal() > version.ordinal();
        }
    }

    private static final Logger log = LoggerFactory.getLogger( ConfigParser.class );

    private final ConfigProperties properties;
    private ConfigVersion version = ConfigVersion.V1;

    ConfigParser( ConfigProperties properties ) {
        this.properties = properties;
    }

    void parseConfigFile( Iterator<String> lines ) {
        String currentLine = null;
        while ( currentLine != null || lines.hasNext() ) {
            String line = currentLine == null ? lines.next() : currentLine;
            switch ( line.trim() ) {
                case "version:":
                    parseVersion( lines );
                    currentLine = null;
                    break;
                case "standard-log-colors:":
                    currentLine = parseStandardLogColors( lines );
                    break;
                case "expressions:":
                    currentLine = parseExpressions( lines );
                    break;
                case "filters:":
                    parseFilters( lines );
                    currentLine = null;
                    break;
                case "time-gaps:":
                    parseTimeGaps( lines );
                    currentLine = null;
                    break;
                case "files:":
                    currentLine = parseFiles( lines );
                    break;
                case "gui:":
                    currentLine = parseGuiSection( lines );
                    break;
                case "auto_update:":
                    // not currently used, skip it
                    if ( lines.hasNext() ) {
                        lines.next();
                    }
                    currentLine = null;
                    break;
                default:
                    throw new IllegalArgumentException( "Non-existing config section: '" + line + "'" );
            }
        }
    }

    private void parseVersion( Iterator<String> lines ) {
        if ( lines.hasNext() ) {
            String line = lines.next();
            if ( line.startsWith( " " ) ) {
                try {
                    version = ConfigVersion.valueOf( line.trim() );
                } catch ( IllegalArgumentException e ) {
                    logInvalidProperty( "version", "version", line.trim(), "Unknown version" );
                }
            } else {
                invalidLine( line, "version" );
            }
        }
    }

    private String parseStandardLogColors( Iterator<String> lines ) {
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
                return line;
            }
        }
        return null;
    }

    private String parseExpressions( Iterator<String> lines ) {
        boolean firstLine = true;
        String group = "";
        while ( lines.hasNext() ) {
            String line = lines.next();
            if ( line.startsWith( " " ) ) {
                line = line.trim();
                if ( firstLine && line.startsWith( "@name@" ) ) {
                    group = line.substring( "@name@".length() ).trim();
                    properties.highlightGroups.add( group );
                } else try {
                    properties.highlightGroups.add( group )
                            .addAll( parseHighlightExpression( line.trim(), version ) );
                } catch ( IllegalArgumentException e ) {
                    logInvalidProperty( "expressions", "highlight", line, e.toString() );
                }
                firstLine = false;
            } else if ( !line.trim().isEmpty() ) {
                return line;
            }
        }
        return null;
    }

    private void parseFilters( Iterator<String> lines ) {
        if ( lines.hasNext() ) {
            String line = lines.next();
            if ( line.startsWith( " " ) ) {
                switch ( line.trim() ) {
                    case "enable" -> properties.enableFilters.set( true );
                    case "disable" -> properties.enableFilters.set( false );
                    default -> logInvalidProperty( "filters", "filters", line, "value must be 'enable' or 'disable'" );
                }
            } else {
                invalidLine( line, "filters" );
            }
        }
    }

    private void parseTimeGaps( Iterator<String> lines ) {
        if ( lines.hasNext() ) {
            String line = lines.next();
            if ( line.startsWith( " " ) ) {
                String lineTrimmed = line.trim();
                switch ( lineTrimmed ) {
                    case "enable" -> properties.displayTimeGaps.set( true );
                    case "disable" -> properties.displayTimeGaps.set( false );
                    default ->
                            logInvalidProperty( "time-gaps", "time-gaps", line, "value must be 'enable' or 'disable'" );
                }
            } else {
                invalidLine( line, "time-gaps" );
            }
        }
    }

    private String parseFiles( Iterator<String> lines ) {
        while ( lines.hasNext() ) {
            String line = lines.next();
            if ( line.startsWith( " " ) ) {
                if ( properties.observableFiles.size() >= FileOpener.MAX_OPEN_FILES ) {
                    log.warn( "Cannot open file {}. Too many open files.", line );
                } else {
                    properties.observableFiles.add( parseLogFileLine( line, version ) );
                }
            } else if ( !line.trim().isEmpty() ) {
                return line;
            }
        }
        return null;
    }

    static LogFile parseLogFileLine( String line, ConfigVersion version ) {
        boolean v4OrHigher = version.isAfter( ConfigVersion.V3 );
        Matcher matcher = v4OrHigher
                ? FILE_LINE_PATTERN_V4.matcher( line )
                : FILE_LINE_PATTERN_V3.matcher( line );
        if ( matcher.matches() ) {
            String timeGapGroup = v4OrHigher ? matcher.group( "timegap" ) : null;
            String group = matcher.group( "group" );
            String name = matcher.group( "name" );
            if ( group == null ) group = "";
            if ( timeGapGroup != null ) {
                // remove the trailing comma
                long timeGap = Long.parseLong( timeGapGroup.substring( 0, timeGapGroup.length() - 1 ) );
                return new LogFile( new File( name ), group, timeGap );
            }
            return new LogFile( new File( name ), group );
        }
        return new LogFile( new File( line.trim() ) );
    }

    private String parseGuiSection( Iterator<String> lines ) {
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
                    case "window":
                        if ( parts.length == 5 ) try {
                            properties.windowBounds.set( new BoundingBox(
                                    Double.parseDouble( parts[ 1 ] ), Double.parseDouble( parts[ 2 ] ),
                                    Double.parseDouble( parts[ 3 ] ), Double.parseDouble( parts[ 4 ] )
                            ) );
                            break;
                        } catch ( NumberFormatException e ) {
                            // proceed to log error below
                        }
                        logInvalidProperty( "gui", "window", line.trim(),
                                "Invalid value for window (should be 'window x y width height')" );
                        break;
                    case "pane-dividers":
                        if ( parts.length != 2 ) {
                            logInvalidProperty( "gui", "pane-dividers", line,
                                    "Expected 2 parts, got " + parts.length );
                        } else try {
                            String[] separators = parts[ 1 ].split( "," );
                            properties.paneDividerPositions.setAll( toDoubles( separators ) );
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
                return line;
            }
        }
        return null;
    }

    static HighlightExpression parseHighlightExpression( String line, ConfigVersion version )
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

        boolean isFiltered = false;

        if ( version.isAfter( ConfigVersion.V1 ) ) {
            if ( line.length() > secondSpaceIndex + 1 ) {
                line = line.substring( secondSpaceIndex + 1 ).trim();
            } else {
                throw highlightParseError( "filter and regular expression not specified", line );
            }

            int thirdSpaceIndex = line.indexOf( ' ' );
            String filteredString;
            if ( thirdSpaceIndex <= 0 ) {
                filteredString = line;
            } else {
                filteredString = line.substring( 0, thirdSpaceIndex );
            }
            isFiltered = switch ( filteredString.toLowerCase() ) {
                case "true" -> true;
                case "false" -> false;
                default -> throw highlightParseError( "invalid value for filtered property", line );
            };

            if ( thirdSpaceIndex > 0 && line.length() > thirdSpaceIndex + 1 ) {
                line = line.substring( thirdSpaceIndex + 1 ).trim();
            } else {
                line = "";
            }
        } else {
            if ( line.length() > secondSpaceIndex + 1 ) {
                line = line.substring( secondSpaceIndex + 1 ).trim();
            } else {
                line = "";
            }
        }

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

    private static void invalidLine( String line, String section ) {
        throw new IllegalArgumentException( "Expected indented line after '" + section + "' but got '" + line + "'" );
    }
}
