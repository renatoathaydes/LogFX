package com.athaydes.logfx.ui;

import com.athaydes.logfx.config.HighlightGroups;
import javafx.scene.control.ChoiceBox;
import javafx.util.StringConverter;

public final class HighlightGroupSelector extends ChoiceBox<String> {

    public HighlightGroupSelector( HighlightGroups highlightGroups, LogView logView ) {
        setConverter( new HighlightGroupSelectorConverter() );
        getItems().addAll( highlightGroups.groupNames() );
        getSelectionModel().select( logView.getLogFile().getHighlightGroup() );
        setOnAction( ( event ) -> logView.getLogFile().highlightGroupProperty().setValue( getValue() ) );
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
