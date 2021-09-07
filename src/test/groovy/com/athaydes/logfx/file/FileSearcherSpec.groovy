package com.athaydes.logfx.file

import spock.lang.Specification

class FileSearcherSpec extends Specification {

    def 'Can find easy target down'() {
        given: 'A simple file reader'
        def reader = Mock( FileContentReader ) {
            1 * refresh() >>> [ Optional.of( [ 'abc', 'def', 'ghi', 'jkl' ] ), Optional.empty() ]
            movePageDown() >>> Optional.empty()
            movePageUp() >>> [ Optional.of( [ 'aa', 'aaa' ] ), Optional.empty() ]
        }

        and: 'A file searcher'
        def searcher = new FileSearcher( reader )

        when: 'We search for something'
        def result = searcher.search { line ->
            FileSearcher.Comparison.of( target <=> line )
        }

        then: 'We find the expected result'
        result == Optional.ofNullable( expected )

        where:
        target || expected
        'abc'  || new FileSearcher.SearchResult( 0, FileSearcher.ResultCase.AT )
        'def'  || new FileSearcher.SearchResult( 1, FileSearcher.ResultCase.AT )
        'ghi'  || new FileSearcher.SearchResult( 2, FileSearcher.ResultCase.AT )
        'jkl'  || new FileSearcher.SearchResult( 3, FileSearcher.ResultCase.AT )
        'mno'  || new FileSearcher.SearchResult( 1, FileSearcher.ResultCase.AFTER )
    }

    def 'Can find easy target up'() {
        given: 'A simple file reader'
        def reader = Mock( FileContentReader ) {
            1 * refresh() >>> [ Optional.of( [ 'abc', 'def', 'ghi', 'jkl' ] ), Optional.empty() ]
            movePageDown() >>> Optional.empty()
            movePageUp() >>> [ Optional.of( [ 'aa', 'aaa' ] ), Optional.empty() ]
        }

        and: 'A file searcher'
        def searcher = new FileSearcher( reader )

        when: 'We search for something'
        def result = searcher.search { line ->
            FileSearcher.Comparison.of( target <=> line )
        }

        then: 'We find the expected result'
        result == Optional.ofNullable( expected )

        where:
        target || expected
        'aaa'  || new FileSearcher.SearchResult( 1, FileSearcher.ResultCase.AT )
        'aa'   || new FileSearcher.SearchResult( 0, FileSearcher.ResultCase.AT )
        'a'    || new FileSearcher.SearchResult( 0, FileSearcher.ResultCase.BEFORE )
    }

    def 'Can find target down a few pages'() {
        given: 'A simple file reader'
        def reader = Mock( FileContentReader ) {
            1 * refresh() >> Optional.of( [ 'abc', 'def' ] )
            pagesDown * movePageDown() >>> [ Optional.of( [ 'ghi', 'jkl' ] ),
                                             Optional.of( [ 'mno', 'pqr' ] ),
                                             Optional.empty() ]
            movePageUp() >>> [ Optional.of( [ 'aa', 'aaa', 'aaaa' ] ), Optional.empty() ]
        }

        and: 'A file searcher'
        def searcher = new FileSearcher( reader )

        when: 'We search for something'
        def result = searcher.search { line ->
            FileSearcher.Comparison.of( target <=> line )
        }

        then: 'We find the expected result'
        result == Optional.ofNullable( expected )

        where:
        target || pagesDown | expected
        'ghi'  || 1         | new FileSearcher.SearchResult( 0, FileSearcher.ResultCase.AT )
        'jkl'  || 1         | new FileSearcher.SearchResult( 1, FileSearcher.ResultCase.AT )
        'mno'  || 2         | new FileSearcher.SearchResult( 0, FileSearcher.ResultCase.AT )
        'pqr'  || 2         | new FileSearcher.SearchResult( 1, FileSearcher.ResultCase.AT )
        'z'    || 3         | new FileSearcher.SearchResult( 2, FileSearcher.ResultCase.AFTER )

    }

    def 'Can find target up a few pages'() {
        given: 'A simple file reader'
        def reader = Mock( FileContentReader ) {
            1 * refresh() >> Optional.of( [ 'pqr', 'stu' ] )
            movePageDown() >>> [ Optional.of( [ 'a', 'aa' ] ) ]
            pagesUp * movePageUp() >>> [
                    Optional.of( [ 'ghi', 'jkl', 'mno' ] ),
                    Optional.of( [ 'abc', 'def' ] ),
                    Optional.empty()
            ]
        }

        and: 'A file searcher'
        def searcher = new FileSearcher( reader )

        when: 'We search for something'
        def result = searcher.search { line ->
            FileSearcher.Comparison.of( target <=> line )
        }

        then: 'We find the expected result'
        result == Optional.ofNullable( expected )

        where:
        target || pagesUp | expected
        'mno'  || 1       | new FileSearcher.SearchResult( 2, FileSearcher.ResultCase.AT )
        'jkl'  || 1       | new FileSearcher.SearchResult( 1, FileSearcher.ResultCase.AT )
        'ghi'  || 1       | new FileSearcher.SearchResult( 0, FileSearcher.ResultCase.AT )
        'def'  || 2       | new FileSearcher.SearchResult( 1, FileSearcher.ResultCase.AT )
        'abc'  || 2       | new FileSearcher.SearchResult( 0, FileSearcher.ResultCase.AT )
        'a'    || 3       | new FileSearcher.SearchResult( 0, FileSearcher.ResultCase.BEFORE )

    }

    def 'Can find target down after unknown line'() {
        given: 'A simple file reader'
        def lines = [ 'abc', 'def', 'ghi', 'jkl' ]
        def index = 0
        def reader = Mock( FileContentReader ) {
            1 * refresh() >>> [ Optional.of( lines[ index..<lines.size() ] ), Optional.empty() ]
            movePageDown() >> Optional.empty()
            moveDown( _ ) >> { int i ->
                println "MoveDown $i"
                index += i
                def range = Math.max( 0, index )..<Math.min( lines.size(), index + lines.size() )
                range.isReverse() ? Optional.empty() : Optional.of( lines[ range ] )
            }
            moveUp( _ ) >> { int i ->
                index -= i
                def range = Math.max( 0, index )..<Math.min( lines.size(), index + lines.size() )
                range.isReverse() ? Optional.empty() : Optional.of( lines[ range ] )
            }
            movePageUp() >> {
                println "MovePageUp"
                index -= lines.size()
                def range = Math.max( 0, index )..<Math.min( lines.size(), index + lines.size() )
                range.isReverse() ? Optional.empty() : Optional.of( lines[ range ] )
            }
        }

        and: 'A file searcher'
        def searcher = new FileSearcher( reader )

        when: 'We search for something'
        def result = searcher.search { line ->
            line in unknownLines
                    ? FileSearcher.Comparison.UNKNOWN
                    : FileSearcher.Comparison.of( target <=> line )
        }

        then: 'We find the expected result'
        result == Optional.ofNullable( expected )

        where:
        target | unknownLines                   || expected
        'abc'  | [ 'abc' ]                      || new FileSearcher.SearchResult( 0, FileSearcher.ResultCase.BEFORE )
        'def'  | [ 'abc' ]                      || new FileSearcher.SearchResult( 1, FileSearcher.ResultCase.AT )
        'def'  | [ 'def' ]                      || new FileSearcher.SearchResult( 2, FileSearcher.ResultCase.BEFORE )
        'def'  | [ 'abc', 'def' ]               || new FileSearcher.SearchResult( 0, FileSearcher.ResultCase.BEFORE )
        'ghi'  | [ 'abc' ]                      || new FileSearcher.SearchResult( 2, FileSearcher.ResultCase.AT )
        'ghi'  | [ 'abc', 'def', 'ghi', 'jkl' ] || null
        'ghi'  | [ 'ghi', 'jkl' ]               || null
    }

    def 'Can find target up after unknown line'() {
        given: 'A simple file reader'
        def lines = [ 'abc', 'def', 'ghi', 'jkl' ]
        def index = 2
        def reader = Mock( FileContentReader ) {
            1 * refresh() >>> [ Optional.of( lines[ index..<lines.size() ] ), Optional.empty() ]
            movePageDown() >> Optional.empty()
            moveDown( _ ) >> { int i ->
                println "MoveDown $i"
                index += i
                def range = Math.max( 0, index )..<Math.min( lines.size(), index + lines.size() )
                range.isReverse() ? Optional.empty() : Optional.of( lines[ range ] )
            }
            0 * moveUp( _ ) >> { int i -> Optional.empty() }
            movePageUp() >> {
                println "MovePageUp"
                index -= lines.size()
                def range = Math.max( 0, index )..<Math.min( lines.size(), index + lines.size() )
                range.isReverse() ? Optional.empty() : Optional.of( lines[ range ] )
            }
        }

        and: 'A file searcher'
        def searcher = new FileSearcher( reader )

        when: 'We search for something'
        def result = searcher.search { line ->
            line in unknownLines
                    ? FileSearcher.Comparison.UNKNOWN
                    : FileSearcher.Comparison.of( target <=> line )
        }

        then: 'We find the expected result'
        result == Optional.ofNullable( expected )

        where:
        target | unknownLines     || expected
        'abc'  | [ 'ghi' ]        || new FileSearcher.SearchResult( 0, FileSearcher.ResultCase.AT )
        'abc'  | [ 'ghi', 'jkl' ] || new FileSearcher.SearchResult( 0, FileSearcher.ResultCase.AT )
        'abc'  | [ 'abc', 'ghi' ] || new FileSearcher.SearchResult( 0, FileSearcher.ResultCase.BEFORE )
        'abc'  | [ 'def', 'ghi' ] || new FileSearcher.SearchResult( 0, FileSearcher.ResultCase.AT )
        'def'  | [ 'abc', 'ghi' ] || new FileSearcher.SearchResult( 1, FileSearcher.ResultCase.AT )
        'def'  | [ 'def', 'ghi' ] || new FileSearcher.SearchResult( 0, FileSearcher.ResultCase.AFTER )
    }

}
