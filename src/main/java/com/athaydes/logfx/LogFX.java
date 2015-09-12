package com.athaydes.logfx;

import com.athaydes.logfx.binding.BindableValue;
import com.athaydes.logfx.file.FileReader;
import com.athaydes.logfx.ui.HighlightOptions;
import com.athaydes.logfx.ui.LogView;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import static com.athaydes.logfx.ui.Dialog.setPrimaryStage;
import static com.athaydes.logfx.ui.FontPicker.showFontPicker;

/**
 *
 */
public class LogFX extends Application {

    private final BindableValue<Font> fontValue = new BindableValue<>( Font.getDefault() );
    private Stage stage;
    private final VBox root = new VBox( 10 );
    private volatile FileReader fileReader;
    private final HighlightOptions highlightOptions = new HighlightOptions();
    private final LogView view = new LogView( fontValue, root.widthProperty(), highlightOptions );
    private final Group headerGroup = new Group();

    @Override
    public void start( Stage primaryStage ) throws Exception {
        this.stage = primaryStage;
        setPrimaryStage( primaryStage );

        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().addAll( fileMenu(), editMenu(), new Menu( "About" ) );

        ScrollPane viewPane = new ScrollPane( view );
        viewPane.prefViewportHeightProperty().bind( root.heightProperty() );

        root.getChildren().addAll( menuBar, headerGroup, viewPane );

        Scene scene = new Scene( root, 800, 600, Color.RED );
        primaryStage.setScene( scene );
        primaryStage.centerOnScreen();
        primaryStage.show();

        primaryStage.setOnHidden( event -> {
            if ( fileReader != null ) fileReader.stop();
        } );
    }

    private Menu fileMenu() {
        Menu menu = new Menu( "_File" );
        menu.setMnemonicParsing( true );

        MenuItem open = new MenuItem( "_Open File" );
        open.setMnemonicParsing( true );
        open.setOnAction( ( event ) -> {
            System.out.println( "Opening" );
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle( "Select a file" );
            File file = fileChooser.showOpenDialog( stage );
            System.out.println( "Selected " + file );
            if ( file != null ) {
                updateFile( file );
            }
        } );
        MenuItem close = new MenuItem( "E_xit" );
        close.setMnemonicParsing( true );
        close.setOnAction( ( event ) -> stage.close() );
        menu.getItems().addAll( open, close );
        return menu;
    }

    private void updateFile( File file ) {
        final FileReader oldFileReader = fileReader;
        fileReader = new FileReader( file, view::showLines );
        fileReader.start( accepted -> {
            if ( accepted && oldFileReader != null ) {
                oldFileReader.stop();
            }
        } );
    }

    private Menu editMenu() {
        Menu menu = new Menu( "_Edit" );
        menu.setMnemonicParsing( true );

        CheckMenuItem highlight = new CheckMenuItem( "_Highlight Options" );
        highlight.setMnemonicParsing( true );

        highlight.setOnAction( ( event ) -> {
            if ( highlight.isSelected() )
                headerGroup.getChildren().add( highlightOptions );
            else
                headerGroup.getChildren().remove( highlightOptions );
        } );
        MenuItem font = new MenuItem( "Fon_t" );
        font.setMnemonicParsing( true );

        AtomicReference<com.athaydes.logfx.ui.Dialog> fontPicker = new AtomicReference<>();

        font.setOnAction( ( event ) -> {
            if ( fontPicker.get() == null || !fontPicker.get().isVisible() ) {
                fontPicker.set( showFontPicker( fontValue.getValue(), selectedFont ->
                        fontValue.setValue( selectedFont ) ) );
            }
        } );

        menu.getItems().addAll( highlight, font );
        return menu;
    }

    public static void main( String[] args ) {
        Application.launch( LogFX.class );
    }

}
