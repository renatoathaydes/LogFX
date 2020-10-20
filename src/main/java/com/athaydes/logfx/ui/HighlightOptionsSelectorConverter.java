package com.athaydes.logfx.ui;

import com.athaydes.logfx.config.HighlightGroups;
import com.athaydes.logfx.text.HighlightExpression;
import javafx.collections.ObservableList;
import javafx.util.StringConverter;

import java.util.Collection;

final class HighlightOptionsSelectorConverter extends StringConverter<HighlightOptions> {

    public static final String DEFAULT_GROUP_DISPLAY_NAME = "Default";

    private final HighlightOptions defaultOption;
    private final HighlightGroups groups;
    private final Collection<HighlightOptions> options;

    HighlightOptionsSelectorConverter(
            Collection<HighlightOptions> options,
            HighlightOptions defaultOption,
            HighlightGroups groups ) {
        this.options = options;
        this.defaultOption = defaultOption;
        this.groups = groups;
    }

    @Override
    public String toString( HighlightOptions option ) {
        if ( option == defaultOption ) return DEFAULT_GROUP_DISPLAY_NAME;
        return option.getGroupName();
    }

    @Override
    public HighlightOptions fromString( String groupName ) {
        if ( groupName.equals( DEFAULT_GROUP_DISPLAY_NAME ) ) return defaultOption;
        ObservableList<HighlightExpression> target = groups.getByName( groupName );
        return options.stream().filter( o -> o.getHighlightOptions() == target )
                .findFirst()
                .orElseThrow( () -> new RuntimeException( "Unreachable" ) );
    }
}
