package com.athaydes.logfx.ui;

import com.athaydes.logfx.binding.BindableValue;
import com.athaydes.logfx.config.Config;
import com.athaydes.logfx.config.HighlightGroups;
import com.athaydes.logfx.text.HighlightExpression;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

import java.io.File;
import java.util.Map;

import static com.athaydes.logfx.ui.AwesomeIcons.TRASH;

public class HighlightGroupsView extends BorderPane {

    private final ChoiceBox<HighlightOptions> optionsChoiceBox;
    private final HighlightOptions defaultOption;
    private final ObservableList<HighlightOptions> options = FXCollections.observableArrayList();

    public HighlightGroupsView( Config config ) {
        HighlightGroups groups = config.getHighlightGroups();
        BindableValue<HighlightOptions> defaultHighlightOptions = new BindableValue<>( null );

        groups.toMap().forEach( ( group, rules ) -> {
            HighlightOptions opt = createHighlightOptions( config, group, rules );
            if ( group.equals( "" ) ) defaultHighlightOptions.setValue( opt );
            options.add( opt );
        } );

        defaultOption = defaultHighlightOptions.getValue();
        assert defaultOption != null;

        optionsChoiceBox = new ChoiceBox<>( options );
        optionsChoiceBox.setConverter( new ChoiceBoxConverter( groups.toMap() ) );

        Button deleteButton = AwesomeIcons.createIconButton( TRASH );
        deleteButton.setTooltip( new Tooltip( "Delete the selected group of highlight rules" ) );
        deleteButton.setDisable( true );
        deleteButton.setOnAction( ( ignore ) -> {
            HighlightOptions option = optionsChoiceBox.getSelectionModel().getSelectedItem();
            if ( option == defaultOption ) return;
            Platform.runLater( () -> setCenter( defaultOption ) );
            options.remove( option );
            groups.remove( option.getGroupName() );
        } );

        Button addButton = new Button( "New Group" );
        addButton.setTooltip( new Tooltip( "Create a new group of highlight rules. " +
                "Each log file can be associated with a group" ) );
        addButton.setOnAction( ( ignore ) -> {
            String groupName = generateGroupName( groups );
            groups.add( groupName );
            HighlightOptions newOptions = createHighlightOptions( config, groupName, groups.getByName( groupName ) );
            options.add( newOptions );
            optionsChoiceBox.setValue( newOptions );
        } );

        optionsChoiceBox.setOnAction( ( ignore ) -> {
            HighlightOptions selectedItem = optionsChoiceBox.getSelectionModel().getSelectedItem();
            if ( selectedItem == null ) return;
            deleteButton.setDisable( selectedItem == defaultOption );
            setCenter( selectedItem );
        } );

        setPadding( new Insets( 5 ) );

        HBox selector = new HBox( 10 );
        Label groupLabel = new Label( "Select group to edit:" );
        selector.getChildren().addAll( groupLabel, optionsChoiceBox, deleteButton, addButton );

        setTop( selector );
        setCenter( defaultOption );

        optionsChoiceBox.setValue( defaultOption );
    }

    public HighlightOptions optionsFor( File file ) {
        // TODO map files to options
        return defaultOption;
    }

    void onShow() {
        HighlightOptions option = ( HighlightOptions ) getCenter();
        option.onShow();
    }

    private HighlightOptions createHighlightOptions( Config config,
                                                     String groupName,
                                                     ObservableList<HighlightExpression> rules ) {
        return new HighlightOptions(
                groupName,
                config.standardLogColorsProperty(),
                rules,
                config.filtersEnabledProperty() );
    }

    private String generateGroupName( HighlightGroups groups ) {
        int groupIndex = options.size() + 1;
        String groupName = "Group " + groupIndex;
        while ( groups.groupNames().contains( groupName ) ) {
            groupIndex++;
            groupName = "Group " + groupIndex;
        }
        return groupName;
    }

    private final class ChoiceBoxConverter extends StringConverter<HighlightOptions> {
        final Map<String, ObservableList<HighlightExpression>> groups;

        private ChoiceBoxConverter( Map<String, ObservableList<HighlightExpression>> groups ) {
            this.groups = groups;
        }

        @Override
        public String toString( HighlightOptions option ) {
            if ( option == defaultOption ) return "Default";
            ObservableList<HighlightExpression> target = option.getHighlightOptions();
            for ( Map.Entry<String, ObservableList<HighlightExpression>> entry : groups.entrySet() ) {
                if ( entry.getValue() == target ) return entry.getKey();
            }
            throw new RuntimeException( "Unreachable" );
        }

        @Override
        public HighlightOptions fromString( String groupName ) {
            if ( groupName.equals( "Default" ) ) return defaultOption;
            ObservableList<HighlightExpression> target = groups.get( groupName );
            return options.stream().filter( o -> o.getHighlightOptions() == target )
                    .findFirst()
                    .orElseThrow( () -> new RuntimeException( "Unreachable" ) );
        }
    }
}
