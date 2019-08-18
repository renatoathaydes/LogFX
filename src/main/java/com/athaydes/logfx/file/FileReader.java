package com.athaydes.logfx.file;

import com.athaydes.logfx.config.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.athaydes.logfx.file.FileReader.LoadMode.MOVE;
import static com.athaydes.logfx.file.FileReader.LoadMode.REFRESH;

/**
 * Standard implementation of {@link FileContentReader}.
 */
public class FileReader implements FileContentReader {

    private static final Logger log = LoggerFactory.getLogger( FileReader.class );

    private static final Predicate<String> NO_FILTER = ( line ) -> true;

    enum LoadMode {
        MOVE, REFRESH
    }

    private enum SearchDirection {
        UP, DOWN, ANY
    }

    private enum Comparison {
        BEFORE, EQUAL, AFTER;

        static <T> Comparison of( Comparable<T> a, T b ) {
            int compareResult = a.compareTo( b );
            return ( compareResult == 0 ) ? EQUAL :
                    ( compareResult < 0 ) ? BEFORE : AFTER;
        }
    }

    private static class EarlyExitFromFileWindow {
        private final FileQueryResult fileQueryResult;
        private final boolean skipWindow;

        EarlyExitFromFileWindow( FileQueryResult fileQueryResult ) {
            this.fileQueryResult = fileQueryResult;
            this.skipWindow = false;
        }

        EarlyExitFromFileWindow( boolean skipWindow ) {
            this.fileQueryResult = null;
            this.skipWindow = skipWindow;
        }

        Optional<FileQueryResult> getFileQueryResult() {
            return Optional.ofNullable( fileQueryResult );
        }

        boolean canSkipWindow() {
            return skipWindow;
        }
    }

    private final File file;
    private final int fileWindowSize;
    private final int bufferSize;
    private final FileLineStarts lineStarts;
    private final int maxLineParseFailuresAllowed;

    private Predicate<String> lineFilter = NO_FILTER;

    // state to avoid reading a file when it is not required...
    // e.g. moving down when the last moveDown returned no lines and:
    //   the file has not been refreshed and
    //   we did not move up
    private boolean noLinesUp = true;
    private boolean noLinesDown = true;

    public FileReader( File file, int fileWindowSize ) {
        this( file, fileWindowSize, 4096 );
    }

    FileReader( File file, int fileWindowSize, int bufferSize ) {
        this.file = file;
        this.fileWindowSize = fileWindowSize;
        this.bufferSize = bufferSize;
        this.maxLineParseFailuresAllowed = Math.min( 50, fileWindowSize );

        if ( log.isDebugEnabled() ) {
            String logfxLog = Properties.LOGFX_DIR.resolve( "logfx.log" ).toFile().getAbsolutePath();
            if ( file.getAbsolutePath().equals( logfxLog ) ) {
                throw new IllegalStateException( "Cannot open LogFX's own log when log level is set to DEBUG or finer.\n" +
                        "That would case an infinite loop when LogFX refreshes the view with the file contents, " +
                        "which causes the file contents to change with new log messages, which causes the view " +
                        "to be refreshed, and so on." );
            }
        }

        // 1 extra line is needed because we need to know the boundaries between lines
        this.lineStarts = new FileLineStarts( fileWindowSize + 1 );
    }

    @Override
    public void setLineFilter( Predicate<String> lineFilter ) {
        if ( lineFilter == null ) {
            this.lineFilter = NO_FILTER;
        } else {
            this.lineFilter = lineFilter;
        }
    }

    @Override
    public Optional<LinkedList<String>> moveUp( int lines ) {
        log.trace( "Moving up {} lines", lines );
        if ( lines < 1 ) {
            return Optional.of( new LinkedList<>() );
        }

        noLinesDown = false;

        if ( noLinesUp ) {
            return Optional.of( new LinkedList<>() );
        }

        Optional<LinkedList<String>> result = loadFromBottom( lineStarts.getFirst() - 1L, lines, MOVE );

        if ( result.isPresent() && result.get().isEmpty() ) {
            noLinesUp = true;
        }

        return result;
    }

    @Override
    public Optional<LinkedList<String>> moveDown( int lines ) {
        log.trace( "Moving down {} lines", lines );

        if ( lines < 1 ) {
            return Optional.of( new LinkedList<>() );
        }

        noLinesUp = false;

        if ( noLinesDown ) {
            return Optional.of( new LinkedList<>() );
        }

        Optional<LinkedList<String>> result = loadFromTop( lineStarts.getLast(), lines, MOVE );

        if ( result.isPresent() && result.get().isEmpty() ) {
            noLinesDown = true;
        }

        return result;
    }

    @SuppressWarnings( { "UnnecessaryLabelOnBreakStatement", "UnusedLabel", "UnnecessaryLabelOnContinueStatement" } )
    @Override
    public FileQueryResult moveTo( ZonedDateTime dateTime,
                                   Function<String, Optional<ZonedDateTime>> dateExtractor ) {
        log.trace( "Moving to date: {}", dateTime );
        Optional<LinkedList<String>> maybeLines = refresh();
        SearchDirection direction = SearchDirection.ANY;

        // if the file is larger than the fileWindowSize, we need to adjust the line number according to
        // in which part of the file we find the date-time, otherwise there's no need
        boolean requireFileLineAdjustment;

        if ( !maybeLines.isPresent() || maybeLines.get().isEmpty() ) {
            log.debug( "No lines found in file, cannot move to given date: {}", dateTime );
            requireFileLineAdjustment = false;
        } else {
            requireFileLineAdjustment = maybeLines.get().size() >= fileWindowSize;
        }


        mainLoop:
        while ( maybeLines.isPresent() ) {
            LinkedList<String> nextLines = maybeLines.get();

            if ( nextLines.isEmpty() ) {
                // reached file end or start
                switch ( direction ) {
                    case UP:
                        log.debug( "Reached the top of the file without finding date {}", dateTime );
                        return OutsideRangeQueryResult.BEFORE;
                    case DOWN:
                        log.debug( "Reached the bottom of the file without finding date {}", dateTime );
                        return OutsideRangeQueryResult.AFTER;
                    case ANY:
                        break mainLoop;
                }
            }

            if ( direction != SearchDirection.ANY ) {
                EarlyExitFromFileWindow earlyExit = checkLastValidLine( nextLines, dateTime, dateExtractor, direction );
                if ( earlyExit.getFileQueryResult().isPresent() ) {
                    return earlyExit.getFileQueryResult().get();
                } else if ( earlyExit.canSkipWindow() ) {
                    maybeLines = moveDown( fileWindowSize );
                    continue mainLoop;
                }
            }

            int failedLines = 0;
            int lineNumber = 0;
            boolean isFirstLine = true;

            Iterator<String> nextLinesIter = nextLines.iterator();
            Optional<ZonedDateTime> lineDateTime = Optional.empty();

            inspectFileWindow:
            while ( nextLinesIter.hasNext() ) {
                String line = "";

                findNextValidDateTime:
                while ( nextLinesIter.hasNext() && failedLines < maxLineParseFailuresAllowed ) {
                    line = nextLinesIter.next();
                    lineDateTime = dateExtractor.apply( line );
                    lineNumber++;

                    if ( lineDateTime.isPresent() ) {
                        failedLines = 0;
                        break findNextValidDateTime;
                    }
                    failedLines++;
                }

                if ( lineDateTime.isPresent() ) {
                    Comparison lineComparison = Comparison.of(
                            lineDateTime.get(), dateTime );

                    if ( direction == SearchDirection.ANY ) {
                        if ( lineComparison == Comparison.BEFORE ) {
                            direction = SearchDirection.DOWN;
                        } else {
                            direction = SearchDirection.UP;
                        }
                    }

                    if ( isFirstLine && direction == SearchDirection.UP && lineComparison == Comparison.AFTER ) {
                        log.trace( "Searching UP and current line is AFTER target, skipping window" );
                        break inspectFileWindow;
                    }

                    if ( lineComparison != Comparison.BEFORE ) {
                        log.debug( "Found line, date comparison = {}, target date = {}, line: {}",
                                lineComparison, dateTime, line );

                        if ( requireFileLineAdjustment ) {
                            return adjustFileWindowToStartAt( nextLines.size(),
                                    ( lineComparison == Comparison.EQUAL ) ? lineNumber : lineNumber - 1 );
                        } else {
                            return new SuccessfulQueryResult( lineNumber );
                        }
                    }

                }

                if ( failedLines >= maxLineParseFailuresAllowed ) {
                    log.info( "Too many log lines do not contain a valid date in file: {}. Cannot move to date {}",
                            getFile(), dateTime );
                    break mainLoop;
                }

                isFirstLine = false;
            }

            switch ( direction ) {
                case UP:
                    maybeLines = moveUp( fileWindowSize );
                    break;
                case ANY:
                    direction = SearchDirection.DOWN; // and fall-through
                case DOWN:
                    maybeLines = moveDown( fileWindowSize );
                    break;
            }

        }

        return UnsuccessfulQueryResult.INSTANCE;
    }

    private EarlyExitFromFileWindow checkLastValidLine( List<String> nextLines,
                                                        ZonedDateTime dateTime,
                                                        Function<String, Optional<ZonedDateTime>> dateExtractor,
                                                        SearchDirection direction ) {
        int failedLines = 0;
        ListIterator<String> reversedIterator = nextLines.listIterator( nextLines.size() );
        Optional<ZonedDateTime> lastLineDateTime = Optional.empty();

        while ( reversedIterator.hasPrevious() && failedLines < maxLineParseFailuresAllowed ) {
            lastLineDateTime = dateExtractor.apply( reversedIterator.previous() );
            if ( lastLineDateTime.isPresent() ) {
                break;
            }
            failedLines++;
        }

        if ( lastLineDateTime.isPresent() ) {
            Comparison lastLineComparison = Comparison.of( lastLineDateTime.get(), dateTime );
            if ( direction == SearchDirection.UP && lastLineComparison != Comparison.AFTER ) {
                log.trace( "Found line in the border between windows while searching UP" );
                return new EarlyExitFromFileWindow(
                        adjustFileWindowToStartAt( nextLines.size(), fileWindowSize - failedLines ) );
            }

            if ( direction == SearchDirection.DOWN && lastLineComparison == Comparison.BEFORE ) {
                log.trace( "Skipping window, last line date = {}, target date = {}", lastLineDateTime.get(), dateTime );
                return new EarlyExitFromFileWindow( true );
            }
        }

        return new EarlyExitFromFileWindow( false );
    }

    FileQueryResult adjustFileWindowToStartAt( int nextLinesSize,
                                               int lineNumber ) {
        log.debug( "Adjusting file window to line {}, next lines count: {}", lineNumber, nextLinesSize );

        boolean isOnFileEdge = nextLinesSize < fileWindowSize;

        if ( lineNumber == 0 ) {
            if ( isOnFileEdge ) {
                return new SuccessfulQueryResult( fileWindowSize - nextLinesSize );
            } else {
                Optional<LinkedList<String>> linesUp = moveUp( 1 );
                if ( linesUp.isPresent() && !linesUp.get().isEmpty() ) {
                    return new SuccessfulQueryResult( 1 );
                } else {
                    return linesUp.isPresent() ?
                            OutsideRangeQueryResult.BEFORE :
                            UnsuccessfulQueryResult.INSTANCE;
                }
            }
        }

        if ( isOnFileEdge ) {
            return new SuccessfulQueryResult( lineNumber + fileWindowSize - nextLinesSize );
        } else {
            Optional<? extends List<String>> nextLines = moveDown( lineNumber - 1 );
            if ( nextLines.isPresent() ) {
                int toAdjust = lineNumber - nextLines.get().size();
                return new SuccessfulQueryResult( toAdjust );
            } else {
                return UnsuccessfulQueryResult.INSTANCE;
            }
        }
    }

    @Override
    public void top() {
        noLinesDown = false;
        noLinesUp = true;
        lineStarts.clear();
        lineStarts.addFirst( 0L );
    }

    @Override
    public void tail() {
        noLinesDown = true;
        noLinesUp = false;
        lineStarts.clear();
        // FIXME if filter is enabled, we need to find the last line that's filtered
        lineStarts.addFirst( file.length() + 1 );
    }

    @Override
    public Optional<LinkedList<String>> refresh() {
        noLinesDown = false;
        noLinesUp = false;

        long initialLine = lineStarts.getFirst();
        log.debug( "Refreshing file from line {}", initialLine );
        Optional<LinkedList<String>> fromTop = loadFromTop( initialLine, fileWindowSize, REFRESH );
        if ( fromTop.isPresent() ) {
            LinkedList<String> topList = fromTop.get();

            if ( topList.size() < fileWindowSize ) {
                log.trace( "Trying to get more lines after a refresh from the top did not give enough lines" );
                loadFromBottom( initialLine - 1L, fileWindowSize - topList.size(), MOVE )
                        .ifPresent( extraLines -> topList.addAll( 0, extraLines ) );
            }
        }
        return fromTop;
    }

    @Override
    public File getFile() {
        return file;
    }

    private Optional<LinkedList<String>> loadFromTop( Long firstLineStartIndex,
                                                      final int lines,
                                                      final LoadMode mode ) {
        if ( !file.isFile() ) {
            return Optional.empty();
        }

        log.trace( "Loading {} lines from the top, file: {}", lines, file );

        if ( firstLineStartIndex >= file.length() - 1 ) {
            log.trace( "Already at the top of the file, nothing to return" );
            return Optional.of( new LinkedList<>() );
        }

        byte[] buffer = new byte[ bufferSize ];
        LinkedList<String> result = new LinkedList<>();
        byte[] topBytes = new byte[ 0 ];

        try ( RandomAccessFile reader = new RandomAccessFile( file, "r" ) ) {
            if ( mode == LoadMode.REFRESH ) {
                lineStarts.clear();
                firstLineStartIndex = seekLineStartBefore( firstLineStartIndex, reader );
            }

            lineStarts.addLast( firstLineStartIndex );

            log.trace( "Seeking position {}", firstLineStartIndex );
            reader.seek( firstLineStartIndex );

            readerMainLoop:
            while ( true ) {
                final long startIndex = reader.getFilePointer();
                final long lastIndex = reader.length() - 1;
                long fileIndex = startIndex;

                log.trace( "Reading chunk {}..{}",
                        startIndex, startIndex + bufferSize );

                final int bytesRead = reader.read( buffer );
                int lineStartIndex = 0;

                if ( log.isTraceEnabled() && bytesRead > 0 && bytesRead < bufferSize ) {
                    log.trace( "Did not read full buffer, chunk that got read is {}..{}", startIndex, startIndex + bytesRead );
                }

                for ( int i = 0; i < bytesRead; i++ ) {
                    byte b = buffer[ i ];
                    boolean isNewLine = ( b == '\n' );
                    boolean isLastByte = ( fileIndex == lastIndex );

                    if ( isNewLine || isLastByte ) {
                        // if the byte is a new line, don't include it in the result
                        int lineEndIndex = isNewLine ? i - 1 : i;

                        if ( isNewLine && i > 0 && buffer[ i - 1 ] == '\r' ) {
                            // do not include the return character in the line
                            lineEndIndex--;
                        }

                        int lineLength = lineEndIndex - lineStartIndex + 1;

                        byte[] lineBytes = new byte[ lineLength + topBytes.length ];
                        log.trace( "Found line, copying [{}:{}] bytes from buffer + {} from top",
                                lineStartIndex, lineLength, topBytes.length );
                        System.arraycopy( topBytes, 0, lineBytes, 0, topBytes.length );
                        System.arraycopy( buffer, lineStartIndex, lineBytes, topBytes.length, lineLength );

                        String line = new String( lineBytes, StandardCharsets.UTF_8 );

                        if ( lineFilter.test( line ) ) {
                            lineStarts.addLast( startIndex + i + 1 );
                            result.addLast( line );
                            log.trace( "Added line: {}", line );
                            if ( result.size() >= lines ) {
                                log.trace( "Got enough lines, breaking out of reader loop" );
                                break readerMainLoop;
                            }
                        }

                        topBytes = new byte[ 0 ];
                        lineStartIndex = isNewLine ? i + 1 : i;
                    }

                    fileIndex++;
                }

                if ( bytesRead < 0L ) {
                    log.trace( "Reached file end, breaking out of reader loop" );
                    break;
                }

                // remember the current buffer bytes as the next top bytes
                int bytesToCopy = bufferSize - lineStartIndex;
                byte[] newTop = new byte[ topBytes.length + bytesToCopy ];
                System.arraycopy( buffer, lineStartIndex, newTop, 0, bytesToCopy );
                System.arraycopy( topBytes, 0, newTop, bytesToCopy, topBytes.length );
                log.trace( "Updated top bytes, now top has {} bytes", newTop.length );
                topBytes = newTop;
            }

            log.debug( "Loaded {} lines from file {}", result.size(), file );
            log.trace( "Line starts: {}", lineStarts );
            return Optional.of( result );
        } catch ( IOException e ) {
            log.warn( "Error reading file [{}]: {}", file, e );
            return Optional.empty();
        }
    }

    private Optional<LinkedList<String>> loadFromBottom( final Long firstLineStartIndex,
                                                         final int lines,
                                                         final LoadMode mode ) {
        if ( !file.isFile() ) {
            return Optional.empty();
        }

        log.trace( "Loading {} lines from the bottom of chunk, file: {}", lines, file );

        if ( firstLineStartIndex <= 0L ) {
            log.trace( "Already at the bottom of the file, nothing to return" );
            return Optional.of( new LinkedList<>() );
        }

        byte[] buffer = new byte[ bufferSize ];
        LinkedList<String> result = new LinkedList<>();
        byte[] tailBytes = new byte[ 0 ];
        long bufferStartIndex = firstLineStartIndex;

        try ( RandomAccessFile reader = new RandomAccessFile( file, "r" ) ) {
            if ( mode == LoadMode.REFRESH ) {
                lineStarts.clear();
                bufferStartIndex = seekLineStartBefore( firstLineStartIndex, reader );
                lineStarts.addLast( Math.max( 0L, bufferStartIndex - 1L ) );
            }

            readerMainLoop:
            while ( true ) {
                long previousStartIndex = bufferStartIndex;

                // start reading from the bottom section of the file above the previous position that fits into the buffer
                bufferStartIndex = Math.max( 0, bufferStartIndex - bufferSize );

                log.trace( "Seeking position {}", bufferStartIndex );
                reader.seek( bufferStartIndex );

                log.trace( "Reading chunk {}:{}, previous start: {}",
                        bufferStartIndex, bufferStartIndex + bufferSize, previousStartIndex );

                final int bytesRead = bufferStartIndex == 0L && previousStartIndex > 0 ?
                        reader.read( buffer, 0, ( int ) previousStartIndex ) :
                        reader.read( buffer );

                int lastByteIndex = bytesRead - 1;

                for ( int i = lastByteIndex; i >= 0; i-- ) {
                    byte b = buffer[ i ];
                    boolean isNewLine = ( b == '\n' );
                    boolean firstFileByte = ( bufferStartIndex == 0 && i == 0 );

                    if ( isNewLine || firstFileByte ) {

                        // if the byte is a new line, don't include it in the result
                        int lineStartIndex = isNewLine ? i + 1 : i;
                        int tailBytesLength = tailBytes.length;
                        int bufferBytesToAdd = lastByteIndex - lineStartIndex + 1;

                        if ( tailBytesLength > 0 ) {
                            if ( tailBytes[ tailBytesLength - 1 ] == '\r' ) {
                                // do not include the return character in the line
                                tailBytesLength--;
                            }
                        } else if ( buffer[ lastByteIndex ] == '\r' ) {
                            // no tail, so the return character is removed from the buffer
                            bufferBytesToAdd--;
                        }

                        byte[] lineBytes = new byte[ bufferBytesToAdd + tailBytesLength ];
                        log.trace( "Found line, copying {} bytes from buffer + {} from tail", bufferBytesToAdd, tailBytes.length );
                        System.arraycopy( buffer, lineStartIndex, lineBytes, 0, bufferBytesToAdd );
                        System.arraycopy( tailBytes, 0, lineBytes, bufferBytesToAdd, tailBytesLength );

                        String line = new String( lineBytes, StandardCharsets.UTF_8 );

                        if ( lineFilter.test( line ) ) {
                            result.addFirst( line );
                            log.trace( "Added line: {}", line );

                            if ( isNewLine ) {
                                lineStarts.addFirst( bufferStartIndex + i + 1 );
                            } else { // this must be the first file byte, remember it
                                lineStarts.addFirst( 0 );
                            }

                            if ( result.size() >= lines ) {
                                log.trace( "Got enough lines, breaking out of the reader loop" );
                                break readerMainLoop;
                            }
                        }

                        tailBytes = new byte[ 0 ];
                        lastByteIndex = i - 1;
                        log.trace( "Last byte index is now {}", lastByteIndex );
                    }
                }

                if ( bufferStartIndex == 0 ) {
                    log.trace( "Reached file start, breaking out of the reader loop" );
                    break;
                }

                if ( lastByteIndex < 0 ) {
                    log.trace( "No bytes were read, skipping copying of tail bytes" );
                    continue;
                }

                // remember the current buffer bytes as the next tail bytes
                byte[] newTail = new byte[ tailBytes.length + lastByteIndex + 1 ];
                System.arraycopy( buffer, 0, newTail, 0, lastByteIndex + 1 );
                System.arraycopy( tailBytes, 0, newTail, lastByteIndex + 1, tailBytes.length );
                log.trace( "Updated tail, now tail has {} bytes", newTail.length );
                tailBytes = newTail;
            }

            log.debug( "Loaded {} lines from file {}", result.size(), file );
            log.trace( "Line starts: {}", lineStarts );
            return Optional.of( result );
        } catch ( IOException e ) {
            log.warn( "Error reading file [{}]: {}", file, e );
            return Optional.empty();
        }
    }

    private long seekLineStartBefore( Long firstLineStartIndex, RandomAccessFile reader )
            throws IOException {
        log.trace( "Seeking line start before or at {}", firstLineStartIndex );
        if ( firstLineStartIndex == 0L ) {
            return 0L;
        }

        if ( firstLineStartIndex >= reader.length() ) {
            log.trace( "Line start found at EOF, file length = {}", reader.length() );
            return reader.length();
        }

        long index = Math.min( firstLineStartIndex - 1, reader.length() - 1 );
        while ( index > 0 ) {
            reader.seek( index );
            int c = reader.read();
            if ( c == '\n' ) {
                break;
            } else {
                index--;
            }
        }

        long result = index == 0L ? 0L : index + 1L;

        log.trace( "Line start before {} found at {}", firstLineStartIndex, result );

        return result;
    }

}
