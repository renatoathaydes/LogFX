package com.athaydes.logfx.ui;

import com.athaydes.logfx.binding.BindableValue;
import com.athaydes.logfx.config.Config;
import com.athaydes.logfx.config.HighlightGroups;
import com.athaydes.logfx.data.LogFile;
import com.athaydes.logfx.text.HighlightExpression;
import javafx.application.Platform;
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

import java.util.Map;

import static com.athaydes.logfx.ui.AwesomeIcons.*;
import static com.athaydes.logfx.ui.HighlightOptions.nextColor;

public class HighlightGroupsView extends BorderPane {
    private HighlightOptions defaultOption;
    private final ChoiceBox<HighlightOptions> optionsChoiceBox;
    private final HighlightGroups groups;
    private HighlightOptionsSelectorConverter selectorConverter;

    public HighlightGroupsView( Config config ) {
        groups = config.getHighlightGroups();

        optionsChoiceBox = new ChoiceBox<>();

        defaultOption = populateChoiceBoxAndReturnDefaultOption( config, optionsChoiceBox.getItems(), groups );

        assert defaultOption != null;

        selectorConverter = new HighlightOptionsSelectorConverter(
                optionsChoiceBox.getItems(), defaultOption, groups );

        optionsChoiceBox.setConverter( selectorConverter );

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

        Button duplicateButton = AwesomeIcons.createIconButton( DUPLICATE );
        duplicateButton.setTooltip( new Tooltip( "Duplicate this group" ) );
        duplicateButton.setOnAction( handleDuplicateGroup( config, groups ) );

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
        selector.getChildren().addAll( groupLabel, optionsChoiceBox, editButton, deleteButton,
                addButton, duplicateButton );

        setTop( selector );
        optionsChoiceBox.getSelectionModel().select( defaultOption );

        config.onReload( () -> Platform.runLater( () -> {
            optionsChoiceBox.getSelectionModel().clearSelection();
            optionsChoiceBox.getItems().clear();
            defaultOption = populateChoiceBoxAndReturnDefaultOption( config, optionsChoiceBox.getItems(), groups );
            selectorConverter = new HighlightOptionsSelectorConverter(
                    optionsChoiceBox.getItems(), defaultOption, groups );
            optionsChoiceBox.setConverter( selectorConverter );
            optionsChoiceBox.getSelectionModel().select( defaultOption );
        } ) );
    }

    public void setGroupFor( LogFile logFile ) {
        HighlightOptions options = selectorConverter.fromString( logFile.getHighlightGroup() );
        optionsChoiceBox.getSelectionModel().select( options );
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

    private ObservableList<HighlightExpression> newGroup( Config config, HighlightGroups groups ) {
        String groupName = generateGroupName( groups );
        ObservableList<HighlightExpression> rules = groups.add( groupName );
        HighlightOptions newOptions = createHighlightOptions( config, groupName, rules );
        optionsChoiceBox.getItems().add( newOptions );
        optionsChoiceBox.getSelectionModel().select( newOptions );
        return rules;
    }

    private EventHandler<ActionEvent> handleAddGroup( Config config, HighlightGroups groups ) {
        return ( ignore ) -> {
            ObservableList<HighlightExpression> rules = newGroup( config, groups );
            rules.add( new HighlightExpression( "", nextColor(), nextColor(), false ) );
        };
    }

    private EventHandler<ActionEvent> handleDuplicateGroup( Config config, HighlightGroups groups ) {
        return ( ignore ) -> {
            String currentGroupName = optionsChoiceBox.getSelectionModel().getSelectedItem().getGroupName();
            ObservableList<HighlightExpression> rules = newGroup( config, groups );
            rules.addAll( groups.getByName( currentGroupName ) );
        };
    }

    private EventHandler<ActionEvent> handleEditGroup( HighlightGroups groups ) {
        return ( ignore ) -> {
            String currentGroupName = optionsChoiceBox.getSelectionModel().getSelectedItem().getGroupName();
            Dialog.askForInput( optionsChoiceBox.getScene(), "New group name:",
                    currentGroupName,
                    ( newName ) -> {
                        newName = newName.trim().replaceAll( "[\\[\\]]", "" );
                        if ( currentGroupName.equals( newName ) ) {
                            // nothing to do
                        } else if ( newName.isEmpty() || newName.equalsIgnoreCase( "default" ) ) {
                            Dialog.showMessage( "Group name not allowed", Dialog.MessageLevel.WARNING );
                        } else if ( groups.groupNames().contains( newName ) ) {
                            Dialog.showMessage( "Group name already exists", Dialog.MessageLevel.WARNING );
                        } else {
                            groups.renameGroup( currentGroupName, newName );

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

            // do not allow deleting default group
            if ( option == defaultOption ) return;

            Dialog.showQuestionDialog( "Are you sure you want to delete group '" + option.getGroupName() + "'?",
                    Map.of( "Yes", () -> {
                        optionsChoiceBox.getItems().remove( option );
                        groups.remove( option.getGroupName() );
                        optionsChoiceBox.getSelectionModel().select( defaultOption );
                    }, "No", optionsChoiceBox::requestFocus ) );
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
