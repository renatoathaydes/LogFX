package com.athaydes.logfx.ui

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.Parent
import spock.lang.Specification
import spock.lang.Unroll

class SelectionHandlerSpec extends Specification {

    @Unroll
    def 'It is possible to select all nodes between two known nodes'() {
        given: 'A mocked parent'
        ObservableList<SelectionHandler.SelectableNode> observableChildren = FXCollections.observableArrayList()
        def root = Mock( Parent )
        root.getChildrenUnmodifiable() >> observableChildren

        and: 'A SelectionHandler is created to handle the parent'
        SelectionHandler handler = new SelectionHandler( root )

        when: 'Several SelectableNodes are added as children to the parent'
        def c0 = Mock( TestSelectableNode )
        def c1 = Mock( TestSelectableNode )
        def c2 = Mock( TestSelectableNode )
        def c3 = Mock( TestSelectableNode )
        def c4 = Mock( TestSelectableNode )

        observableChildren.addAll( c0, c1, c2, c3, c4 )

        and: 'A drag event requests selection between two nodes'
        handler.selectAllBetween( observableChildren[ dragStartIndex ],
                observableChildren[ dragEndIndex ] )

        then: 'The expected items are selected'
        def expectedSelected = observableChildren.withIndex().findAll {
            expectedSelectedIndexes.contains( it.second as int )
        }.collect {
            it.first
        }
        // test sanity check
        expectedSelectedIndexes.size() == expectedSelected.size()

        handler.selectedItems.size() == expectedSelected.size()
        handler.selectedItems.containsAll( expectedSelected )

        where:
        dragStartIndex | dragEndIndex || expectedSelectedIndexes
        0              | 0            || [ 0 ]
        0              | 1            || [ 0, 1 ]
        1              | 0            || [ 0, 1 ]
        1              | 1            || [ 1 ]
        2              | 0            || [ 0, 1, 2 ]
        0              | 2            || [ 0, 1, 2 ]
        1              | 2            || [ 1, 2 ]
        2              | 1            || [ 1, 2 ]
        3              | 0            || [ 0, 1, 2, 3 ]
        0              | 3            || [ 0, 1, 2, 3 ]
        3              | 1            || [ 1, 2, 3 ]
        1              | 3            || [ 1, 2, 3 ]
        0              | 4            || [ 0, 1, 2, 3, 4 ]
        4              | 0            || [ 0, 1, 2, 3, 4 ]
        3              | 4            || [ 3, 4 ]
        4              | 3            || [ 3, 4 ]
        4              | 4            || [ 4 ]

    }

}

abstract class TestSelectableNode extends Node
        implements SelectionHandler.SelectableNode {}