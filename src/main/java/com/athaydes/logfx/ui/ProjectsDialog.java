package com.athaydes.logfx.ui;

import com.athaydes.logfx.config.Config;
import com.athaydes.logfx.config.Properties;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static com.athaydes.logfx.config.Properties.DEFAULT_PROJECT_NAME;
import static com.athaydes.logfx.ui.Dialog.MessageLevel.ERROR;
import static com.athaydes.logfx.ui.Dialog.MessageLevel.INFO;
import static com.athaydes.logfx.ui.Dialog.MessageLevel.WARNING;
import static java.util.stream.Collectors.toMap;

public final class ProjectsDialog {
    private static final Logger log = LoggerFactory.getLogger( ProjectsDialog.class );

    private final Config config;

    public ProjectsDialog( Config config ) {
        this.config = config;
    }

    public void showFor( Stage stage ) {
        var buttonBox = new HBox( 10 );
        var optionsBox = new VBox( 2 );
        var optionsBoxScrollPane = new ScrollPane( optionsBox );
        optionsBoxScrollPane.setMinHeight( 150 );
        optionsBoxScrollPane.setMaxHeight( 250 );
        optionsBoxScrollPane.setHbarPolicy( ScrollPane.ScrollBarPolicy.NEVER );
        optionsBoxScrollPane.setVbarPolicy( ScrollPane.ScrollBarPolicy.ALWAYS );
        var dialog = new Dialog( optionsBoxScrollPane, buttonBox );
        dialog.setTitle( "Manage Projects" );
        dialog.makeTransparentWhenLoseFocus();
        dialog.setWidth( 400 );
        dialog.setStyle( StageStyle.UTILITY );
        var currentProject = config.getCurrentConfigPath();
        var currentProjectName = Properties.DEFAULT_LOGFX_CONFIG.equals( currentProject )
                ? DEFAULT_PROJECT_NAME
                : currentProject.getFileName().toString();
        var defaultProjectButtonRef = new AtomicReference<Button>();
        buildProjectMap( stage ).forEach( ( projectName, action ) -> {
            var optionBox = new HBox( 2 );

            var optionButton = new Button( projectName );
            optionButton.setPrefWidth( 305 );
            optionButton.setMaxWidth( 305 );
            var isCurrent = currentProjectName.equals( projectName );
            if ( isCurrent ) {
                markCurrent( optionButton );
            }
            optionButton.setOnAction( ( e ) -> {
                dialog.hide();
                action.run();
            } );
            var deleteButton = AwesomeIcons.createIconButton( AwesomeIcons.TRASH );
            if ( DEFAULT_PROJECT_NAME.equals( projectName ) ) {
                deleteButton.setDisable( true );
                defaultProjectButtonRef.set( optionButton );
            } else {
                deleteButton.setOnAction( ( e ) ->
                        Dialog.showQuestionDialog( "Are you sure you want to delete project " + projectName,
                                Map.of( "Yes", () -> deleteProject( projectName, isCurrent, stage,
                                                () -> {
                                                    optionsBox.getChildren().remove( optionBox );
                                                    markCurrent( defaultProjectButtonRef.get() );
                                                } ),
                                        "No", () -> {
                                        } ) ) );
            }

            optionBox.getChildren().addAll( optionButton, deleteButton );
            optionsBox.getChildren().add( optionBox );
        } );

        var closeButton = new Button( "Close" );
        closeButton.setOnAction( ( e ) -> dialog.hide() );
        buttonBox.getChildren().add( closeButton );

        buildCreateNewProjectMap( stage ).forEach( ( text, action ) -> {
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

    private void markCurrent( Button optionButton ) {
        optionButton.setDisable( true );
        optionButton.getStyleClass().add( "favourite-button" );
    }

    private Map<String, Runnable> buildProjectMap( Stage stage ) {
        return Properties.listProjects().stream().collect(
                toMap( Function.identity(), projectName -> () -> openProject( projectName, stage ), ( a, b ) -> {
                    throw new IllegalStateException( "Cannot have projects with same name" );
                }, LinkedHashMap::new ) );
    }

    private Map<String, Runnable> buildCreateNewProjectMap( Stage stage ) {
        return Map.of( "New Project", () ->
                Dialog.askForInput( stage.getScene(), "Project Name", null, projectName -> {
                    if ( DEFAULT_PROJECT_NAME.equals( projectName ) ) {
                        Platform.runLater( () -> Dialog.showMessage(
                                "Cannot create project named " + DEFAULT_PROJECT_NAME, WARNING ) );
                    } else {
                        log.info( "Creating new project '{}'", projectName );
                        openProject( projectName, stage );
                    }
                } ) );
    }

    @MustCallOnJavaFXThread
    private void openProject( String projectName, Stage stage ) {
        if ( projectName != null && !projectName.isBlank() ) {
            log.info( "Opening project '{}'", projectName );
            Properties.getProjectFile( projectName ).ifPresent( config::loadConfig );
            var title = "LogFX" + ( DEFAULT_PROJECT_NAME.equals( projectName ) ? "" : " (" + projectName + ")" );
            stage.setTitle( title );
        }
    }

    private void deleteProject( String projectName, boolean isCurrent, Stage stage, Runnable onDelete ) {
        if ( projectName != null && !projectName.isBlank() && !DEFAULT_PROJECT_NAME.equals( projectName ) ) {
            log.info( "Deleting project '{}'", projectName );
            Properties.getProjectFile( projectName ).ifPresentOrElse( p -> {
                if ( isCurrent ) {
                    // deleting current project, open the default project first (which can't be deleted)
                    openProject( DEFAULT_PROJECT_NAME, stage );
                }
                var ok = p.toFile().delete();
                if ( ok ) {
                    onDelete.run();
                    Dialog.showMessage( "Project deleted successfully", INFO );
                } else {
                    Dialog.showMessage( "Project could not be deleted", WARNING );
                }
            }, () -> Dialog.showMessage( "Project does not exist", ERROR ) );
        }
    }

}
