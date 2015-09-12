package com.athaydes.logfx.ui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;

import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static javafx.collections.FXCollections.observableList;

public class FontPicker {

    public static void showFontPicker( Font currentFont, Consumer<Font> selectionCallback ) {
        ComboBox<String> fontNames = new ComboBox<>(
                observableList( Font.getFontNames() ) );
        fontNames.getSelectionModel().select( currentFont.getName() );

        ComboBox<Double> fontSizes = new ComboBox<>(
                observableList( rangeClosed( 6, 42 )
                        .asDoubleStream().boxed()
                        .collect( toList() ) )
        );
        fontSizes.getSelectionModel().select( currentFont.getSize() );

        EventHandler<ActionEvent> eventHandler = ( event ) ->
                selectionCallback.accept( new Font( fontNames.getValue(), fontSizes.getValue() ) );

        fontNames.setOnAction( eventHandler );
        fontSizes.setOnAction( eventHandler );

        GridPane grid = new GridPane();
        grid.setVgap( 5 );
        grid.add( new Label( "Font name:" ), 0, 0 );
        grid.add( fontNames, 1, 0 );
        grid.add( new Label( "Font size:" ), 0, 1 );
        grid.add( fontSizes, 1, 1 );

        Dialog dialog = new Dialog( grid );
        dialog.setTitle( "Select a font" );
        dialog.setAlwaysOnTop( true );

        dialog.show();
    }

}
