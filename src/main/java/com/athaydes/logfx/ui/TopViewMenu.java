package com.athaydes.logfx.ui;

import com.athaydes.logfx.config.Config;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.athaydes.logfx.ui.FontPicker.showFontPicker;
import static com.athaydes.logfx.ui.HighlightOptions.showHighlightOptionsDialog;

public final class TopViewMenu extends Menu {

    private final CheckMenuItem highlight;

    public TopViewMenu( LogViewPane logsPane, Config config ) {
        super( "_View" );
        setMnemonicParsing( true );

        highlight = new CheckMenuItem( "_Highlight Options" );
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

        getItems().addAll( highlight, orientation, font, filter, showContextMenu );
    }

    public Consumer<LogView> getGroupCreateCallback() {
        return ( logView ) -> highlight.selectedProperty().setValue( true );
    }

    @MustCallOnJavaFXThread
    private static void bindMenuItemToDialog( CheckMenuItem menuItem, Callable<Dialog> dialogCreator ) {
        AtomicReference<Dialog> dialogRef = new AtomicReference<>();

        menuItem.selectedProperty().addListener( ( ignore ) -> {
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
                dialogRef.getAndSet( null ).hide();
            }
        } );
    }
}
