package com.athaydes.logfx.config;

import com.athaydes.logfx.text.HighlightExpression;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

import java.util.HashMap;

final class HighlightGroups {
    private final ObservableMap<String, ObservableList<HighlightExpression>> configByGroupName =
            FXCollections.observableMap( new HashMap<>( 4 ) );

    ObservableList<HighlightExpression> getByName( String name ) {
        return configByGroupName.get( name );
    }

    ObservableList<HighlightExpression> getDefault() {
        return configByGroupName.computeIfAbsent( "", ( ignore ) -> FXCollections.observableArrayList() );
    }

    void add( String groupName, HighlightExpression expression ) {
        configByGroupName.computeIfAbsent( groupName,
                ( ignore ) -> FXCollections.observableArrayList() )
                .add( expression );
    }

    ObservableMap<String, ObservableList<HighlightExpression>> toMap() {
        return configByGroupName;
    }

    void addListener( InvalidationListener listener ) {
        configByGroupName.addListener( listener );
    }
}