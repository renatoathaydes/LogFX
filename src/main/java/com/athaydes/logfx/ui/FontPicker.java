package com.athaydes.logfx.ui;

import com.athaydes.logfx.binding.BindableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static javafx.collections.FXCollections.observableList;

public class FontPicker {

    public static Dialog showFontPicker(BindableValue<Font> fontProperty) {
        ComboBox<String> fontNames = new ComboBox<>(
                observableList(Font.getFontNames().stream().distinct().collect(toList())));
        fontNames.getSelectionModel().select(fontProperty.getValue().getName());

        ComboBox<Double> fontSizes = new ComboBox<>(
                observableList(rangeClosed(6, 42)
                        .asDoubleStream().boxed()
                        .collect(toList()))
        );
        fontSizes.getSelectionModel().select(fontProperty.getValue().getSize());

        EventHandler<ActionEvent> eventHandler = (event) ->
                fontProperty.setValue(new Font(fontNames.getValue(), fontSizes.getValue()));

        fontNames.setOnAction(eventHandler);
        fontSizes.setOnAction(eventHandler);

        GridPane grid = new GridPane();
        grid.setVgap(5);
        grid.add(new Label("Font name:"), 0, 0);
        grid.add(fontNames, 1, 0);
        grid.add(new Label("Font size:"), 0, 1);
        grid.add(fontSizes, 1, 1);

        Dialog dialog = new Dialog(grid);
        dialog.setTitle("Select a font");
        dialog.setAlwaysOnTop(true);
        dialog.setResizable(false);
        dialog.makeTransparentWhenLoseFocus();

        dialog.show();
        return dialog;
    }

}
