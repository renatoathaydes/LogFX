package com.athaydes.logfx.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public final class FileSearcher {
    private static final Logger log = LoggerFactory.getLogger( FileSearcher.class );

    private final FileContentReader fileReader;

    public FileSearcher( FileContentReader fileReader ) {
        this.fileReader = fileReader;
    }

    public Optional<SearchResult> search( SearchFunction searchFunction ) {
        log.trace( "Starting search" );
        var lines = fileReader.refresh();
        if ( lines.isEmpty() || lines.get().isEmpty() ) {
            // cannot find anything as there's no lines
            return Optional.empty();
        }
        var currentPage = lines.get();
        var nextLine = currentPage.get( 0 );
        var comparison = searchFunction.test( nextLine );
        return switch ( comparison ) {
            case BEFORE -> {
                // the target can't be in the current page, so get the next one
                var pageUp = fileReader.movePageUp();
                if ( pageUp.isEmpty() || pageUp.get().isEmpty() ) {
                    // result was on previous page, we need to go back down and return report
                    // the target is before the first line
                    fileReader.movePageDown();
                    yield Optional.of( new SearchResult( 0, ResultCase.BEFORE ) );
                }
                yield searchUp( pageUp.get(), searchFunction );
            }
            case UNKNOWN -> {
                var downResult = searchDown( currentPage, searchFunction );
                if ( downResult.isPresent() ) {
                    var result = downResult.get();
                    yield switch ( result.resultCase() ) {
                        // if it's before, we need to searchUp as well from the location where we found it
                        case BEFORE -> fileReader.moveDown( result.lineNumber() )
                                .flatMap( page -> searchUp( page, searchFunction ) );
                        case AFTER, AT -> downResult;
                    };
                }

                yield Optional.empty();
            }
            case AFTER -> // target might be in the current page, we need to go down
                    searchDown( currentPage, searchFunction );
            // lucky, found target on the very first line
            case EQUAL -> Optional.of( new SearchResult( 0, ResultCase.AT ) );
        };
    }

    private Optional<SearchResult> searchUp( List<String> currentPage, SearchFunction searchFunction ) {
        log.debug( "Searching up one page" );

        int mustBeAfterLine = -1;

        while ( !currentPage.isEmpty() ) {
            pageLoop: for ( var i = 0; i < currentPage.size(); i++ ) {
                var line = currentPage.get( i );
                var comparison = searchFunction.test( line );
                switch ( comparison ) {
                    case BEFORE -> {
                      break pageLoop;
                    }
                    case EQUAL -> {
                        log.debug( "Found result at line {}", i );
                        return Optional.of( new SearchResult( i, ResultCase.AT ) );
                    }
                    case AFTER -> {
                        mustBeAfterLine = i;
                    }
                    case UNKNOWN -> {}
                }
            }
            if ( mustBeAfterLine >= 0 ) {
                log.debug( "Found result after line {}", mustBeAfterLine );
                return Optional.of( new SearchResult( mustBeAfterLine, ResultCase.AFTER ) );
            }

            log.debug( "Search continuing on next page up" );

            // it's not on this page, try another page up
            var nextPage = fileReader.movePageUp();
            currentPage = nextPage.isEmpty() ? List.of() : nextPage.get();
        }

        log.debug( "Could not find anything" );

        // ran out of pages
        fileReader.movePageDown();
        return Optional.of( new SearchResult( 0, ResultCase.BEFORE ) );
    }

    private Optional<SearchResult> searchDown( List<String> currentPage, SearchFunction searchFunction ) {
        log.debug( "Searching down current page" );

        int mustBeBeforeLine = -1;

        while ( !currentPage.isEmpty() ) {
            linesLoop:
            for ( var i = 0; i < currentPage.size(); i++ ) {
                var line = currentPage.get( i );
                var comparison = searchFunction.test( line );
                switch ( comparison ) {
                    case AFTER, UNKNOWN -> {
                    }
                    case EQUAL -> {
                        log.debug( "Found result at line {}", i );
                        return Optional.of( new SearchResult( i, ResultCase.AT ) );
                    }
                    case BEFORE -> {
                        mustBeBeforeLine = i;
                        break linesLoop;
                    }
                }
            }
            if ( mustBeBeforeLine >= 0 ) {
                log.debug( "Found result before line {}", mustBeBeforeLine );

                return Optional.of( new SearchResult( mustBeBeforeLine, ResultCase.BEFORE ) );
            }

            log.debug( "Search continuing on next page up" );

            // it's not on this page, try another page down
            var nextPage = fileReader.movePageDown();
            currentPage = nextPage.isEmpty() ? List.of() : nextPage.get();
        }

        log.debug( "Could not find anything searching down, will try searching up" );

        // ran out of pages
        var previousPage = fileReader.movePageUp();
        return previousPage.isEmpty() || previousPage.get().isEmpty()
                ? Optional.empty()
                : searchUp( previousPage.get(), searchFunction );
    }

    public enum Comparison {
        BEFORE, EQUAL, AFTER, UNKNOWN;

        public static Comparison of( int comparatorResult ) {
            return comparatorResult < 0
                    ? BEFORE
                    : comparatorResult > 0
                    ? AFTER
                    : EQUAL;
        }
    }

    public interface SearchFunction {
        Comparison test( String line );
    }

    public enum ResultCase {
        BEFORE, AFTER, AT
    }

    public record SearchResult(int lineNumber, ResultCase resultCase) {
    }
}
