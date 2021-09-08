package com.athaydes.logfx.config;

import com.athaydes.logfx.data.LogFile;
import com.athaydes.logfx.text.HighlightExpression;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class HighlightGroups {
    private final ObservableSet<LogFile> observableFiles;
    private final ObservableMap<String, ObservableList<HighlightExpression>> configByGroupName;

    private InvalidationListener invalidationListener;

    public HighlightGroups( ObservableSet<LogFile> observableFiles ) {
        this.observableFiles = observableFiles;
        configByGroupName = FXCollections.observableMap( new LinkedHashMap<>( 4 ) );
    }

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
        observableFiles.stream()
                .filter( it -> groupName.equals( it.getHighlightGroup() ) )
                .forEach( file -> file.highlightGroupProperty().setValue( "" ) );
    }

    public void renameGroup( String oldName, String newName ) {
        configByGroupName.put( newName, configByGroupName.remove( oldName ) );
        observableFiles.stream()
                .filter( it -> oldName.equals( it.getHighlightGroup() ) )
                .forEach( file -> file.highlightGroupProperty().setValue( newName ) );
    }

    public Set<String> groupNames() {
        return Collections.unmodifiableSet( configByGroupName.keySet() );
    }

    public Map<String, ObservableList<HighlightExpression>> toMap() {
        return Collections.unmodifiableMap( configByGroupName );
    }

    public void addGroupNameListener( InvalidationListener listener ) {
        configByGroupName.addListener( listener );
    }

    public void removeGroupNameListener( InvalidationListener listener ) {
        configByGroupName.removeListener( listener );
    }

    public void clear() {
        configByGroupName.clear();
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