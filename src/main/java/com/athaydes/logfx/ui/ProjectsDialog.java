package com.athaydes.logfx.ui;

import com.athaydes.logfx.config.Config;
import com.athaydes.logfx.config.Properties;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import static com.athaydes.logfx.config.Properties.DEFAULT_PROJECT_NAME;
import static com.athaydes.logfx.ui.Dialog.MessageLevel.INFO;
import static com.athaydes.logfx.ui.Dialog.MessageLevel.WARNING;
import static java.util.stream.Collectors.toMap;

public final class ProjectsDialog {
    private static final Logger log = LoggerFactory.getLogger( ProjectsDialog.class );

    private final Config config;

    public ProjectsDialog( Config config ) {
        this.config = config;
    }

    public void showNear( Node root ) {
        var label = new Label( "Projects" );
        var buttonBox = new HBox( 10 );
        var optionsBox = new VBox( 2 );
        var optionsBoxScrollPane = new ScrollPane( optionsBox );
        optionsBoxScrollPane.setMinHeight( 150 );
        optionsBoxScrollPane.setMaxHeight( 250 );
        optionsBoxScrollPane.setHbarPolicy( ScrollPane.ScrollBarPolicy.NEVER );
        var dialog = new Dialog( label, optionsBoxScrollPane, buttonBox );
        dialog.closeWhenLoseFocus();
        dialog.setWidth( 400 );
        dialog.setStyle( StageStyle.UNDECORATED );
        buildProjectMap().forEach( ( projectName, action ) -> {
            var optionBox = new HBox( 2 );

            var optionButton = new Button( projectName );
            if ( DEFAULT_PROJECT_NAME.equals( projectName ) ) {
                optionButton.getStyleClass().add( "favourite-button" );
            }
            optionButton.setOnAction( ( e ) -> {
                dialog.hide();
                action.run();
            } );
            var deleteButton = AwesomeIcons.createIconButton( AwesomeIcons.TRASH );
            deleteButton.setOnAction( ( e ) -> {
                dialog.hide();
                Dialog.showQuestionDialog( "Are you sure you want to delete project " + projectName,
                        Map.of( "Yes", () -> deleteProject( projectName ),
                                "No", () -> {
                                } ) );
            } );

            optionBox.getChildren().addAll( optionButton, deleteButton );
            HBox.setHgrow( optionButton, Priority.ALWAYS );
            optionsBox.getChildren().add( new BorderPane( optionBox ) );
        } );

        var cancelButton = new Button( "Cancel" );
        cancelButton.setOnAction( ( e ) -> dialog.hide() );
        buttonBox.getChildren().add( cancelButton );

        buildCreateNewProjectMap( root ).forEach( ( text, action ) -> {
            var button = new Button( text );
            button.getStyleClass().add( "favourite-button" );
            button.setOnAction( e -> {
                dialog.hide();
                action.run();
            } );
            buttonBox.getChildren().add( button );
        } );

        dialog.show();
    }

    private Map<String, Runnable> buildProjectMap() {
        return Properties.listProjects().stream().collect(
                toMap( Function.identity(), projectName -> () -> openProject( projectName ), ( a, b ) -> {
                    throw new IllegalStateException( "Cannot have projects with same name" );
                }, LinkedHashMap::new ) );
    }

    private Map<String, Runnable> buildCreateNewProjectMap( Node root ) {
        return Map.of( "New Project", () ->
                Dialog.askForInput( root.getScene(), "Project Name", null, projectName -> {
                    if ( DEFAULT_PROJECT_NAME.equals( projectName ) ) {
                        Platform.runLater( () -> Dialog.showMessage(
                                "Cannot create project named " + DEFAULT_PROJECT_NAME, WARNING ) );
                    } else {
                        log.info( "Creating new project '{}'", projectName );
                        openProject( projectName );
                    }
                } ) );
    }

    private void openProject( String projectName ) {
        if ( projectName != null && !projectName.isBlank() ) {
            log.info( "Opening project '{}'", projectName );
            Properties.getProjectFile( projectName ).ifPresent( config::loadConfig );
        }
    }

    private void deleteProject( String projectName ) {
        if ( projectName != null && !projectName.isBlank() ) {
            log.info( "Opening project '{}'", projectName );
            Properties.getProjectFile( projectName ).ifPresent( p -> {
                var ok = p.toFile().delete();
                if ( ok ) {
                    Dialog.showMessage( "Project deleted successfully", INFO );
                } else {
                    Dialog.showMessage( "Project could not be deleted", WARNING );
                }
            } );
        }
    }

}
