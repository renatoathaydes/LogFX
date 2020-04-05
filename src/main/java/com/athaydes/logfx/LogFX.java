package com.athaydes.logfx;

import com.athaydes.logfx.concurrency.TaskRunner;
import com.athaydes.logfx.config.Config;
import com.athaydes.logfx.config.Properties;
import com.athaydes.logfx.data.LogFile;
import com.athaydes.logfx.data.NaNChecker.NaNException;
import com.athaydes.logfx.file.FileContentReader;
import com.athaydes.logfx.file.FileReader;
import com.athaydes.logfx.log.LogConfigFile;
import com.athaydes.logfx.ui.AboutLogFXView;
import com.athaydes.logfx.ui.BottomMessagePane;
import com.athaydes.logfx.ui.Dialog;
import com.athaydes.logfx.ui.FileDragAndDrop;
import com.athaydes.logfx.ui.FileOpener;
import com.athaydes.logfx.ui.FxUtils;
import com.athaydes.logfx.ui.HighlightGroupsView;
import com.athaydes.logfx.ui.LogView;
import com.athaydes.logfx.ui.LogViewPane;
import com.athaydes.logfx.ui.MustCallOnJavaFXThread;
import com.athaydes.logfx.ui.StartUpView;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.SplashScreen;
import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static com.athaydes.logfx.data.NaNChecker.checkNaN;
import static com.athaydes.logfx.ui.Dialog.setPrimaryStage;
import static com.athaydes.logfx.ui.FontPicker.showFontPicker;
import static com.athaydes.logfx.ui.HighlightOptions.showHighlightOptionsDialog;

/**
 * The LogFX JavaFX Application.
 */
public class LogFX extends Application {

    // NOT static because it would cause initialization problems if it were
    private final Logger log = LoggerFactory.getLogger( LogFX.class );

    private static final String TITLE = "LogFX";

    private static final AtomicReference<HostServices> hostServices = new AtomicReference<>();

    private Stage stage;
    private final Pane root = new Pane();
    private final Rectangle overlay = new Rectangle( 0, 0 );
    private final Config config;
    private final LogViewPane logsPane;
    private final BottomMessagePane bottomMessagePane = BottomMessagePane.warningIfFiltersEnabled();

    private final TaskRunner taskRunner = new TaskRunner( false );

    @MustCallOnJavaFXThread
    public LogFX() {
        hostServices.set( getHostServices() );
        Path configFile = Properties.LOGFX_DIR.resolve( "config" );
        this.config = new Config( configFile, taskRunner );

        this.logsPane = new LogViewPane( taskRunner, () ->
                new StartUpView( stage, this::open ),
                config.getHighlightGroups(),
                config.getObservableFiles().isEmpty() );

        logsPane.orientationProperty().bindBidirectional( config.panesOrientationProperty() );

        openFilesFromConfig();
    }

    @Override
    @MustCallOnJavaFXThread
    public void start( Stage primaryStage ) {
        this.stage = primaryStage;
        setPrimaryStage( primaryStage );
        setIconsOn( primaryStage );
        setupResizeListenersAndResize( primaryStage );
        hackToCauseJavaFXToRedrawWholeWindowDueToJavaFXBug( primaryStage );

        MenuBar menuBar = new MenuBar();
        menuBar.useSystemMenuBarProperty().set( true );
        menuBar.getMenus().addAll( fileMenu(), viewMenu(), helpMenu() );

        VBox mainBox = new VBox( 0 );
        logsPane.prefHeightProperty().bind( mainBox.heightProperty() );
        mainBox.getChildren().addAll( menuBar, logsPane.getNode() );

        Platform.runLater( () -> updateBottomMessagePane( mainBox ) );
        config.filtersEnabledProperty().addListener( ( o ) -> updateBottomMessagePane( mainBox ) );

        root.getChildren().addAll( mainBox, overlay );

        Scene scene = new Scene( root, 800, 600, Color.BLACK );

        root.prefHeightProperty().bind( scene.heightProperty() );
        root.prefWidthProperty().bind( scene.widthProperty() );

        mainBox.prefHeightProperty().bind( scene.heightProperty() );
        mainBox.prefWidthProperty().bind( scene.widthProperty() );

        primaryStage.setScene( scene );
        primaryStage.setTitle( TITLE );

        SplashScreen splashScreen = SplashScreen.getSplashScreen();

        if ( splashScreen == null ) {
            primaryStage.show();
        }

        primaryStage.setOnHidden( event -> {
            logsPane.close();
            taskRunner.shutdown();
        } );

        Platform.runLater( () -> {
            log.debug( "Setting divider positions to {}", config.getPaneDividerPositions() );
            logsPane.setDividerPositions( config.getPaneDividerPositions() );
            logsPane.panesDividersProperty().addListener( observable ->
                    config.getPaneDividerPositions().setAll( logsPane.getSeparatorsPositions() ) );

            // all done, show the stage if necessary, then hide the splash screen
            if ( splashScreen != null ) {
                primaryStage.show();
                splashScreen.close();
            }
        } );

        FxUtils.setupStylesheet( scene );
    }

    private void setupResizeListenersAndResize( Stage stage ) {
        class WindowBoundsUpdater implements Runnable {
            @Override
            public void run() {
                Platform.runLater( () -> {
                    // JavaFX sometimes gives NaN, avoid updating in such cases
                    try {
                        config.windowBoundsProperty().set(
                                new BoundingBox( checkNaN( stage.getX() ), checkNaN( stage.getY() ),
                                        checkNaN( stage.getWidth() ), checkNaN( stage.getHeight() ) ) );
                    } catch ( NaNException e ) {
                        log.warn( "Unable to update window coordinates due to one or more dimensions being " +
                                "unavailable" );
                    }
                } );
            }
        }

        Bounds bounds = config.windowBoundsProperty().get();
        if ( bounds != null ) {
            stage.setX( bounds.getMinX() );
            stage.setY( bounds.getMinY() );
            stage.setWidth( bounds.getWidth() );
            stage.setHeight( bounds.getHeight() );
        } else {
            stage.centerOnScreen();
        }

        WindowBoundsUpdater windowBoundsUpdater = new WindowBoundsUpdater();

        InvalidationListener updateBounds = ( ignore ) ->
                taskRunner.runWithMaxFrequency( windowBoundsUpdater, 1000L );

        stage.widthProperty().addListener( updateBounds );
        stage.heightProperty().addListener( updateBounds );
        stage.xProperty().addListener( updateBounds );
        stage.yProperty().addListener( updateBounds );
    }

    // fixes JavaFX bug: https://bugs.openjdk.java.net/browse/JDK-8189092
    private void hackToCauseJavaFXToRedrawWholeWindowDueToJavaFXBug( Stage stage ) {
        double h = stage.getHeight();
        taskRunner.runDelayed( () -> {
            Platform.runLater( () -> stage.setHeight( h + 1 ) );
            taskRunner.runDelayed( () -> {
                Platform.runLater( () -> stage.setHeight( h - 1 ) );
            }, Duration.ofSeconds( 1 ) );
        }, Duration.ofSeconds( 2 ) );
    }

    private void updateBottomMessagePane( VBox mainBox ) {
        boolean enable = config.filtersEnabledProperty().get();
        // we need to remove / add the pane because it will still show up even when its height is set to 0 on Mac
        if ( enable ) {
            mainBox.getChildren().add( bottomMessagePane );
        }
        bottomMessagePane.setShow( enable, () -> {
            if ( !enable ) {
                mainBox.getChildren().remove( bottomMessagePane );
            }
        } );
    }

    private void setIconsOn( Stage primaryStage ) {
        taskRunner.runAsync( () -> {
            final List<String> images = Arrays.asList(
                    FxUtils.resourcePath( "images/favicon-large.png" ),
                    FxUtils.resourcePath( "images/favicon-small.png" ) );

            Platform.runLater( () -> images.stream()
                    .map( Image::new )
                    .forEach( primaryStage.getIcons()::add ) );
        } );
    }

    @MustCallOnJavaFXThread
    private Menu fileMenu() {
        Menu menu = new Menu( "_File" );
        menu.setMnemonicParsing( true );

        MenuItem open = new MenuItem( "_Open File" );
        open.setAccelerator( new KeyCodeCombination( KeyCode.O, KeyCombination.SHORTCUT_DOWN ) );
        open.setMnemonicParsing( true );
        open.setOnAction( ( event ) -> new FileOpener( stage, this::open ) );

        MenuItem showLogFxLog = new MenuItem( "Open LogFX Log" );
        showLogFxLog.setAccelerator( new KeyCodeCombination( KeyCode.O,
                KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN ) );
        showLogFxLog.setOnAction( ( event ) ->
                open( LogConfigFile.INSTANCE.logFilePath.toFile() ) );

        MenuItem close = new MenuItem( "E_xit" );
        close.setAccelerator( new KeyCodeCombination( KeyCode.W,
                KeyCombination.SHIFT_DOWN, KeyCombination.SHORTCUT_DOWN ) );
        close.setMnemonicParsing( true );
        close.setOnAction( ( event ) -> stage.close() );
        menu.getItems().addAll( open, showLogFxLog, close );

        return menu;
    }

    @MustCallOnJavaFXThread
    private Menu helpMenu() {
        Menu menu = new Menu( "_Help" );
        menu.setMnemonicParsing( true );

        MenuItem about = new MenuItem( "_About LogFX" );
        about.setOnAction( ( event ) -> new AboutLogFXView().show() );

        menu.getItems().addAll( about );

        return menu;
    }

    private void openFilesFromConfig() {
        List<LogFile> files = new ArrayList<>( config.getObservableFiles() );
        for ( LogFile file : files ) {
            Platform.runLater( () -> {
                boolean accepted = openViewFor( file, -1 );
                if ( !accepted ) {
                    config.getObservableFiles().remove( file );
                }
            } );
        }
    }

    @MustCallOnJavaFXThread
    private void open( File file ) {
        open( file, -1 );
    }

    @MustCallOnJavaFXThread
    private void open( File file, int index ) {
        LogFile logFile = new LogFile( file );
        if ( config.getObservableFiles().contains( logFile ) ) {
            log.debug( "Tried to open file that is already opened, will focus on it" );
            logsPane.focusOn( file );
        } else {
            boolean accepted = openViewFor( logFile, index );
            if ( accepted ) {
                config.getObservableFiles().add( logFile );
            }
        }
    }

    @MustCallOnJavaFXThread
    private boolean openViewFor( LogFile logFile, int index ) {
        log.debug( "Creating file reader and view for file {}", logFile.file );

        FileContentReader fileReader;
        try {
            fileReader = new FileReader( logFile.file, LogView.MAX_LINES );
        } catch ( IllegalStateException e ) {
            Dialog.showMessage( e.getMessage(), Dialog.MessageLevel.ERROR );
            return false;
        }

        LogView view = new LogView( config, root.widthProperty(), logFile, fileReader, taskRunner );

        FileDragAndDrop.install( view, logsPane, overlay, ( droppedFile, target ) -> {
            int droppedOnPaneIndex = logsPane.indexOf( view );
            if ( droppedOnPaneIndex < 0 ) {
                open( droppedFile );
            } else {
                switch ( target ) {
                    case BEFORE:
                        open( droppedFile, droppedOnPaneIndex );
                        break;
                    case AFTER:
                        open( droppedFile, droppedOnPaneIndex + 1 );
                        break;
                    default:
                        throw new IllegalStateException( "Unknown target: " + target.name() );
                }
            }
        } );

        logsPane.add( view, () -> config.getObservableFiles().remove( logFile ), index );

        return true;
    }

    @MustCallOnJavaFXThread
    private Menu viewMenu() {
        Menu menu = new Menu( "_View" );
        menu.setMnemonicParsing( true );

        CheckMenuItem highlight = new CheckMenuItem( "_Highlight Options" );
        highlight.setAccelerator( new KeyCodeCombination( KeyCode.H,
                // on Mac, Cmd+H hides the window, so let it use Ctrl+H instead
                FxUtils.isMac() ? KeyCombination.CONTROL_DOWN : KeyCombination.SHORTCUT_DOWN ) );
        highlight.setMnemonicParsing( true );
        bindMenuItemToDialog( highlight, () ->
                showHighlightOptionsDialog( new HighlightGroupsView( config ) ) );

        MenuItem orientation = new MenuItem( "Switch Pane Orientation" );
        orientation.setAccelerator( new KeyCodeCombination( KeyCode.S,
                KeyCombination.SHIFT_DOWN, KeyCombination.SHORTCUT_DOWN ) );
        orientation.setOnAction( event -> logsPane.switchOrientation() );

        CheckMenuItem font = new CheckMenuItem( "Fon_t" );
        font.setAccelerator( new KeyCodeCombination( KeyCode.F,
                KeyCombination.SHIFT_DOWN, KeyCombination.SHORTCUT_DOWN ) );
        font.setMnemonicParsing( true );
        bindMenuItemToDialog( font, () -> showFontPicker( config.fontProperty() ) );

        CheckMenuItem filter = new CheckMenuItem( "Enable _filters" );
        filter.setAccelerator( new KeyCodeCombination( KeyCode.F,
                KeyCombination.SHORTCUT_DOWN ) );
        filter.setMnemonicParsing( true );
        filter.selectedProperty().bindBidirectional( config.filtersEnabledProperty() );

        MenuItem showContextMenu = new MenuItem( "Show Context Menu" );
        showContextMenu.setAccelerator( new KeyCodeCombination( KeyCode.E, KeyCombination.SHORTCUT_DOWN ) );
        showContextMenu.setOnAction( event -> logsPane.showContextMenu() );

        menu.getItems().addAll( highlight, orientation, font, filter, showContextMenu );
        return menu;
    }

    @MustCallOnJavaFXThread
    private static void bindMenuItemToDialog( CheckMenuItem menuItem, Callable<Dialog> dialogCreator ) {
        AtomicReference<Dialog> dialogRef = new AtomicReference<>();

        menuItem.setOnAction( ( event ) -> {
            if ( menuItem.isSelected() ) {
                if ( dialogRef.get() == null || !dialogRef.get().isVisible() ) {
                    try {
                        Dialog dialog = dialogCreator.call();
                        dialogRef.set( dialog );
                        dialog.setOnHidden( e -> {
                            menuItem.setSelected( false );
                            dialogRef.set( null );
                        } );
                    } catch ( Exception e ) {
                        e.printStackTrace();
                    }
                }
            } else if ( dialogRef.get() != null ) {
                dialogRef.get().hide();
            }
        } );
    }

    public static HostServices hostServices() {
        return hostServices.get();
    }

    public static void main( String[] args ) {
        SetupTrayIcon.run();

        Font.loadFont( LogFX.class.getResource( "/com/athaydes/logfx/fonts/themify-1.0.1.ttf" ).toExternalForm(), 12 );

        Application.launch( LogFX.class );
    }

}
