package com.athaydes.logfx.ui;

import com.athaydes.logfx.config.Config;
import com.athaydes.logfx.text.HighlightExpression;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.geometry.Insets;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.StringConverter;

import java.io.File;
import java.util.Map;

public class HighlightGroups extends VBox {

    private final ChoiceBox<HighlightOptions> optionsChoiceBox;
    private final ObservableList<HighlightOptions> options = FXCollections.observableArrayList();

    public HighlightGroups( Config config ) {
        ObservableMap<String, ObservableList<HighlightExpression>> groups = config.getHighlightGroups();

        groups.forEach( ( group, rules ) -> {
            options.add( new HighlightOptions(
                    config.standardLogColorsProperty(),
                    rules,
                    config.filtersEnabledProperty() ) );
        } );

        optionsChoiceBox = new ChoiceBox<>( options );
        optionsChoiceBox.setConverter( new ChoiceBoxConverter( groups ) );
        optionsChoiceBox.setValue( options.get( 0 ) );

        setSpacing( 5 );
        setPadding( new Insets( 5 ) );

        HBox selector = new HBox( 10 );
        selector.getChildren().addAll( new Text( "Select a group:" ), optionsChoiceBox );

        getChildren().addAll( selector, options.get( 0 ) );
    }

    public HighlightOptions optionsFor( File file ) {
        // TODO map files to options
        return options.get( 0 );
    }

    private final class ChoiceBoxConverter extends StringConverter<HighlightOptions> {
        final ObservableMap<String, ObservableList<HighlightExpression>> groups;

        private ChoiceBoxConverter( ObservableMap<String, ObservableList<HighlightExpression>> groups ) {
            this.groups = groups;
        }

        @Override
        public String toString( HighlightOptions option ) {
            ObservableList<HighlightExpression> target = option.getHighlightOptions();
            for ( Map.Entry<String, ObservableList<HighlightExpression>> entry : groups.entrySet() ) {
                // FIXME default group shows as the empty String
                if ( entry.getValue() == target ) return entry.getKey();
            }
            throw new RuntimeException( "Unreachable" );
        }

        @Override
        public HighlightOptions fromString( String groupName ) {
            ObservableList<HighlightExpression> target = groups.get( groupName );
            return options.stream().filter( o -> o.getHighlightOptions() == target )
                    .findFirst()
                    .orElseThrow( () -> new RuntimeException( "Unreachable" ) );
        }
    }
}
