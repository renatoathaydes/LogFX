package com.athaydes.logfx.ui;

import com.athaydes.logfx.config.HighlightGroups;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.scene.control.ChoiceBox;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class HighlightGroupSelector extends ChoiceBox<String> {

    private static final Logger logger = LoggerFactory.getLogger( HighlightGroupSelector.class );

    private final HighlightGroups highlightGroups;
    private final InvalidationListener groupNamesListener;

    public HighlightGroupSelector( HighlightGroups highlightGroups, LogView logView ) {
        this.highlightGroups = highlightGroups;

        // listener for group names changes
        this.groupNamesListener = ignore -> {
            getItems().setAll( sorted( highlightGroups.groupNames() ) );
            Platform.runLater( () ->
                    getSelectionModel().select( logView.getLogFile().getHighlightGroup() ) );
        };

        logView.getLogFile().highlightGroupProperty().addListener( ignore -> {
            getSelectionModel().select( logView.getLogFile().getHighlightGroup() );
        } );

        highlightGroups.addGroupNameListener( groupNamesListener );

        setConverter( new HighlightGroupSelectorConverter() );
        getItems().addAll( sorted( highlightGroups.groupNames() ) );
        getSelectionModel().select( logView.getLogFile().getHighlightGroup() );
        setOnAction( ( event ) -> {
            String newValue = getValue();
            if ( newValue != null ) {
                logger.debug( "Setting group '{}' for file {}",
                        getValue(), logView.getFile() );
                logView.getLogFile().highlightGroupProperty().setValue( newValue );
            }
        } );
    }

    private static List<String> sorted( Set<String> values ) {
        List<String> sortedList = new ArrayList<>( values );
        Collections.sort( sortedList );
        return sortedList;
    }

    public void dispose() {
        highlightGroups.removeGroupNameListener( groupNamesListener );
    }

    private static final class HighlightGroupSelectorConverter extends StringConverter<String> {

        @Override
        public String toString( String option ) {
            if ( option.isEmpty() ) return HighlightOptionsSelectorConverter.DEFAULT_GROUP_DISPLAY_NAME;
            return option;
        }

        @Override
        public String fromString( String groupName ) {
            return groupName;
        }
    }

}
