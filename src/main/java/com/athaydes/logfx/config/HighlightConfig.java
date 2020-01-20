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

final class HighlightConfig {
    final ObservableList<HighlightExpression> observableExpressions;

    public HighlightConfig() {
        observableExpressions = FXCollections.observableArrayList();
    }
}

final class HighlightGroups {
    private final ObservableMap<String, HighlightConfig> configByGroupName =
            FXCollections.observableMap( new HashMap<>( 4 ) );

    HighlightConfig getByName( String name ) {
        return configByGroupName.get( name );
    }

    HighlightConfig getDefault() {
        return configByGroupName.get( "" );
    }

    void add( String groupName, HighlightExpression expression ) {
        configByGroupName.computeIfAbsent( groupName, ( ignore ) -> new HighlightConfig() )
                .observableExpressions.add( expression );
    }

    Map<String, Collection<HighlightExpression>> toMap() {
        Map<String, Collection<HighlightExpression>> result = new HashMap<>( configByGroupName.size() );
        for ( Map.Entry<String, HighlightConfig> entry : configByGroupName.entrySet() ) {
            result.put( entry.getKey(), new ArrayList<>( entry.getValue().observableExpressions ) );
        }
        return result;
    }

    public void addListener( InvalidationListener listener ) {
        configByGroupName.addListener( listener );
    }
}