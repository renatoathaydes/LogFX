package com.athaydes.logfx.config;

import com.athaydes.logfx.text.HighlightExpression;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class HighlightGroups {
    private InvalidationListener invalidationListener;
    private final ObservableMap<String, ObservableList<HighlightExpression>> configByGroupName =
            FXCollections.observableMap( new HashMap<>( 4 ) );

    public ObservableList<HighlightExpression> getByName( String name ) {
        return configByGroupName.get( name );
    }

    public ObservableList<HighlightExpression> getDefault() {
        return configByGroupName.computeIfAbsent( "", this::createNewExpressions );
    }

    public ObservableList<HighlightExpression> add( String groupName ) {
        return configByGroupName.computeIfAbsent( groupName,
                this::createNewExpressions );
    }

    public void remove( String groupName ) {
        configByGroupName.remove( groupName );
    }

    public Set<String> groupNames() {
        return configByGroupName.keySet();
    }

    public Map<String, ObservableList<HighlightExpression>> toMap() {
        return configByGroupName;
    }

    void setListener( InvalidationListener listener ) {
        if ( invalidationListener != null ) throw new RuntimeException( "invalidation listener has already been set" );
        invalidationListener = listener;
        configByGroupName.addListener( listener );
        configByGroupName.values().forEach( ( v ) -> v.addListener( listener ) );
    }

    private ObservableList<HighlightExpression> createNewExpressions( Object ignore ) {
        ObservableList<HighlightExpression> newList = FXCollections.observableArrayList();
        if ( invalidationListener != null ) newList.addListener( invalidationListener );
        return newList;
    }
}