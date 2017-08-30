package com.athaydes.logfx;

import com.athaydes.logfx.binding.BindableValue;
import com.athaydes.logfx.concurrency.TaskRunner;
import com.athaydes.logfx.config.Config;
import com.athaydes.logfx.file.FileContentReader;
import com.athaydes.logfx.file.FileReader;
import com.athaydes.logfx.ui.Dialog;
import com.athaydes.logfx.ui.HighlightOptions;
import com.athaydes.logfx.ui.LogView;
import com.athaydes.logfx.ui.LogViewPane;
import com.athaydes.logfx.ui.MustCallOnJavaFXThread;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static com.athaydes.logfx.ui.Dialog.setPrimaryStage;
import static com.athaydes.logfx.ui.FontPicker.showFontPicker;
import static com.athaydes.logfx.ui.HighlightOptions.showHighlightOptionsDialog;

/**
 * The LogFX JavaFX Application.
 */
public class LogFX extends Application {

    private static final Logger log = LoggerFactory.getLogger( LogFX.class );

    private static final String TITLE = "LogFX";

    private final BindableValue<Font> fontValue = new BindableValue<>( Font.font( "Monaco" ) );
    private Stage stage;
    private final VBox root = new VBox( 10 );
    private final Config config;
    private final HighlightOptions highlightOptions;
    private final LogViewPane logsPane;

    private final TaskRunner taskRunner = new TaskRunner();

    @MustCallOnJavaFXThread
    public LogFX() {
        String userHome = System.getProperty( "user.home" );
        if ( userHome == null ) {
            throw new IllegalStateException( "Cannot start LogFX, user.home property is not defined" );
        }

        Path configFile = Paths.get( userHome, ".logfx" );
        this.config = new Config( configFile, taskRunner );
        this.highlightOptions = new HighlightOptions( config.getObservableExpressions() );

        this.logsPane = new LogViewPane();
        logsPane.orientationProperty().bindBidirectional( config.panesOrientationProperty() );

        openFilesFromConfig();
    }

    @Override
    @MustCallOnJavaFXThread
    public void start( Stage primaryStage ) throws Exception {
        this.stage = primaryStage;
        setPrimaryStage( primaryStage );

        MenuBar menuBar = new MenuBar();
        menuBar.useSystemMenuBarProperty().set( true );
        menuBar.getMenus().addAll( fileMenu(), viewMenu(), new Menu( "About" ) );

        logsPane.prefHeightProperty().bind( root.heightProperty() );

        root.getChildren().addAll( menuBar, logsPane.getNode() );

        Scene scene = new Scene( root, 800, 600, Color.RED );
        scene.getStylesheets().add( "css/LogFX.css" );

        primaryStage.setScene( scene );
        primaryStage.centerOnScreen();
        primaryStage.setTitle( TITLE );
        primaryStage.show();

        primaryStage.setOnHidden( event -> {
            logsPane.close();
            taskRunner.shutdown();
        } );
    }

    @MustCallOnJavaFXThread
    private Menu fileMenu() {
        Menu menu = new Menu( "_File" );
        menu.setMnemonicParsing( true );

        MenuItem open = new MenuItem( "_Open File" );
        open.setAccelerator( new KeyCodeCombination( KeyCode.O, KeyCombination.META_DOWN ) );
        open.setMnemonicParsing( true );
        open.setOnAction( ( event ) -> {
            log.debug( "Opening file" );
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle( "Select a file" );
            File file = fileChooser.showOpenDialog( stage );
            log.debug( "Selected file {}", file );
            if ( file != null ) {
                open( file );
            }
        } );

        MenuItem close = new MenuItem( "E_xit" );
        close.setAccelerator( new KeyCodeCombination( KeyCode.W,
                KeyCombination.SHIFT_DOWN, KeyCombination.META_DOWN ) );
        close.setMnemonicParsing( true );
        close.setOnAction( ( event ) -> stage.close() );
        menu.getItems().addAll( open, close );

        return menu;
    }

    @MustCallOnJavaFXThread
    private void openFilesFromConfig() {
        for ( File file : config.getObservableFiles() ) {
            Platform.runLater( () -> openViewFor( file ) );
        }
    }

    @MustCallOnJavaFXThread
    private void open( File file ) {
        if ( config.getObservableFiles().contains( file ) ) {
            log.debug( "Tried to open file that is already opened, will focus on it" );
            logsPane.focusOn( file );
        } else {
            openViewFor( file );
            config.getObservableFiles().add( file );
        }
    }

    @MustCallOnJavaFXThread
    private void openViewFor( File file ) {
        log.debug( "Creating file reader and view for file {}", file );

        FileContentReader fileReader = new FileReader( file, LogView.MAX_LINES );
        LogView view = new LogView( fontValue, root.widthProperty(),
                highlightOptions, fileReader, taskRunner );

        logsPane.add( view, () -> config.getObservableFiles().remove( file ) );
    }

    @MustCallOnJavaFXThread
    private Menu viewMenu() {
        Menu menu = new Menu( "_View" );
        menu.setMnemonicParsing( true );

        CheckMenuItem highlight = new CheckMenuItem( "_Highlight Options" );
        highlight.setAccelerator( new KeyCodeCombination( KeyCode.H, KeyCombination.META_DOWN ) );
        highlight.setMnemonicParsing( true );
        bindMenuItemToDialog( highlight, () ->
                showHighlightOptionsDialog( highlightOptions ) );

        MenuItem orientation = new MenuItem( "Switch Pane Orientation" );
        orientation.setAccelerator( new KeyCodeCombination( KeyCode.S,
                KeyCombination.SHIFT_DOWN, KeyCombination.META_DOWN ) );
        orientation.setOnAction( event -> logsPane.switchOrientation() );

        CheckMenuItem font = new CheckMenuItem( "Fon_t" );
        font.setAccelerator( new KeyCodeCombination( KeyCode.F,
                KeyCombination.SHIFT_DOWN, KeyCombination.META_DOWN ) );
        font.setMnemonicParsing( true );
        bindMenuItemToDialog( font, () ->
                showFontPicker( fontValue.getValue(), fontValue::setValue ) );

        menu.getItems().addAll( highlight, orientation, font );
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

    public static void main( String[] args ) {
        Font.loadFont( LogFX.class.getResource( "/fonts/fontawesome-webfont.ttf" ).toExternalForm(), 12 );
        Application.launch( LogFX.class );
    }

}
