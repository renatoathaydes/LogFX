package com.athaydes.logfx.ui

import com.athaydes.logfx.iterable.ObservableListView
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
        def root = Mock( SelectableContainer ) {
            getSelectables() >> new ObservableListView<Node, Node>( Node, observableChildren )
            getNode() >> mockNode()
        }

        and: 'A SelectionHandler is created to handle the parent'
        SelectionHandler handler = new SelectionHandler( root )

        when: 'Several SelectableNodes are added as children to the parent'
        def c0 = mockNode()
        def c1 = mockNode()
        def c2 = mockNode()
        def c3 = mockNode()
        def c4 = mockNode()

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

    private TestSelectableNode mockNode() {
        Mock( TestSelectableNode ) {
            getNode() >> Mock( TestSelectableNode )
        }
    }

}

abstract class TestSelectableNode extends Node
        implements SelectionHandler.SelectableNode {}