package com.athaydes.logfx.ui;

import com.athaydes.logfx.binding.BindableValue;
import com.athaydes.logfx.config.Config;
import com.athaydes.logfx.config.HighlightGroups;
import com.athaydes.logfx.text.HighlightExpression;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import static com.athaydes.logfx.ui.AwesomeIcons.PENCIL;
import static com.athaydes.logfx.ui.AwesomeIcons.PLUS;
import static com.athaydes.logfx.ui.AwesomeIcons.TRASH;

public class HighlightGroupsView extends BorderPane {
    private final ChoiceBox<HighlightOptions> optionsChoiceBox;
    private final HighlightOptions defaultOption;
    private final HighlightGroups groups;

    public HighlightGroupsView( Config config ) {
        groups = config.getHighlightGroups();

        optionsChoiceBox = new ChoiceBox<>();

        defaultOption = populateChoiceBoxAndReturnDefaultOption( config, optionsChoiceBox.getItems(), groups );
        assert defaultOption != null;

        optionsChoiceBox.setConverter( new HighlightOptionsSelectorConverter(
                optionsChoiceBox.getItems(), defaultOption, groups ) );

        Button deleteButton = AwesomeIcons.createIconButton( TRASH );
        deleteButton.setTooltip( new Tooltip( "Delete the selected group of highlight rules" ) );
        deleteButton.setDisable( true );
        deleteButton.setOnAction( handleDeleteGroup( groups ) );

        Button editButton = AwesomeIcons.createIconButton( PENCIL );
        editButton.setTooltip( new Tooltip( "Edit the selected group" ) );
        editButton.setOnAction( handleEditGroup( groups ) );

        Button addButton = AwesomeIcons.createIconButton( PLUS );
        addButton.setTooltip( new Tooltip( "Create a new group of highlight rules. " +
                "Each log file can be associated with a group" ) );
        addButton.setOnAction( handleAddGroup( config, groups ) );

        optionsChoiceBox.setOnAction( ( ignore ) -> {
            HighlightOptions selectedItem = optionsChoiceBox.getSelectionModel().getSelectedItem();
            if ( selectedItem == null ) return;
            editButton.setDisable( selectedItem == defaultOption );
            deleteButton.setDisable( selectedItem == defaultOption );
            setCenter( selectedItem );
        } );

        setPadding( new Insets( 5 ) );

        HBox selector = new HBox( 10 );
        Label groupLabel = new Label( "Select group to edit:" );
        selector.getChildren().addAll( groupLabel, optionsChoiceBox, editButton, deleteButton, addButton );

        setTop( selector );
        optionsChoiceBox.getSelectionModel().select( defaultOption );
    }

    void onShow() {
        HighlightOptions option = ( HighlightOptions ) getCenter();
        option.onShow();
    }

    static HighlightOptions populateChoiceBoxAndReturnDefaultOption(
            Config config,
            ObservableList<HighlightOptions> options,
            HighlightGroups groups ) {
        BindableValue<HighlightOptions> defaultHighlightOptions = new BindableValue<>( null );

        groups.toMap().forEach( ( group, rules ) -> {
            HighlightOptions opt = createHighlightOptions( config, group, rules );
            if ( group.equals( "" ) ) defaultHighlightOptions.setValue( opt );
            options.add( opt );
        } );

        return defaultHighlightOptions.getValue();
    }

    private EventHandler<ActionEvent> handleAddGroup( Config config, HighlightGroups groups ) {
        return ( ignore ) -> {
            String groupName = generateGroupName( groups );
            ObservableList<HighlightExpression> rules = groups.add( groupName );
            HighlightOptions newOptions = createHighlightOptions( config, groupName, rules );
            optionsChoiceBox.getItems().add( newOptions );
            optionsChoiceBox.getSelectionModel().select( newOptions );
        };
    }

    private EventHandler<ActionEvent> handleEditGroup( HighlightGroups groups ) {
        return ( ignore ) -> {
            String currentGroupName = optionsChoiceBox.getSelectionModel().getSelectedItem().getGroupName();
            Dialog.askForInput( optionsChoiceBox.getScene(), "New group name:",
                    currentGroupName,
                    ( newName ) -> {
                        newName = newName.trim();
                        if ( currentGroupName.equals( newName ) ) {
                            // nothing to do
                        } else if ( newName.isEmpty() || newName.equalsIgnoreCase( "default" ) ) {
                            Dialog.showMessage( "Group name not allowed", Dialog.MessageLevel.WARNING );
                        } else if ( groups.groupNames().contains( newName ) ) {
                            Dialog.showMessage( "Group name already exists", Dialog.MessageLevel.WARNING );
                        } else {
                            ObservableList<HighlightExpression> rules = groups.remove( currentGroupName );
                            groups.add( newName ).addAll( rules );

                            HighlightOptions opt = optionsChoiceBox.getSelectionModel().getSelectedItem();
                            opt.setGroupName( newName );

                            // force the selection box to update the group name in the view as well
                            int index = optionsChoiceBox.getSelectionModel().getSelectedIndex();
                            optionsChoiceBox.getItems().remove( index );
                            optionsChoiceBox.getItems().add( index, opt );
                        }
                    } );
        };
    }

    private EventHandler<ActionEvent> handleDeleteGroup( HighlightGroups groups ) {
        return ( ignore ) -> {
            HighlightOptions option = optionsChoiceBox.getSelectionModel().getSelectedItem();
            if ( option == defaultOption ) return;
            optionsChoiceBox.getItems().remove( option );
            groups.remove( option.getGroupName() );
            optionsChoiceBox.getSelectionModel().select( defaultOption );
        };
    }

    private static HighlightOptions createHighlightOptions( Config config,
                                                            String groupName,
                                                            ObservableList<HighlightExpression> rules ) {
        return new HighlightOptions(
                groupName,
                config.standardLogColorsProperty(),
                rules,
                config.filtersEnabledProperty() );
    }

    private String generateGroupName( HighlightGroups groups ) {
        int groupIndex = optionsChoiceBox.getItems().size() + 1;
        String groupName = "Group " + groupIndex;
        while ( groups.groupNames().contains( groupName ) ) {
            groupIndex++;
            groupName = "Group " + groupIndex;
        }
        return groupName;
    }

}
