package com.athaydes.logfx.config;

import com.athaydes.logfx.text.HighlightExpression;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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

    Map<String, Collection<HighlightExpression>> toMap() {
        Map<String, Collection<HighlightExpression>> result = new HashMap<>( configByGroupName.size() );
        for ( Map.Entry<String, ObservableList<HighlightExpression>> entry : configByGroupName.entrySet() ) {
            result.put( entry.getKey(), new ArrayList<>( entry.getValue() ) );
        }
        return result;
    }

    void addListener( InvalidationListener listener ) {
        configByGroupName.addListener( listener );
    }
}