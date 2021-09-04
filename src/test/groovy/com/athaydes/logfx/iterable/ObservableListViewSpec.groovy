package com.athaydes.logfx.iterable


import spock.lang.Specification

import static javafx.collections.FXCollections.observableArrayList

class ObservableListViewSpec extends Specification {

    def 'Can iterate over all items'() {
        given: 'a view is created'
        def view = new ObservableListView<Integer, Number>( Integer, observableArrayList( 0.1, 1, 2, 3.14 ) )

        when: 'we iterate over all items'
        def items = [ ]
        for ( item in view.iterable ) {
            items << item
        }

        then: 'we get the expected items'
        items == [ 1, 2 ]
    }

}
