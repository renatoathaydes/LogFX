package com.athaydes.logfx.config;

import com.athaydes.logfx.binding.BindableValue;
import com.athaydes.logfx.data.LogLineColors;
import com.athaydes.logfx.text.HighlightExpression;
import com.athaydes.logfx.ui.FxUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.geometry.Orientation;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.io.File;
import java.util.LinkedHashSet;

final class ConfigProperties {
    final SimpleObjectProperty<LogLineColors> standardLogColors;
    final ObservableList<HighlightExpression> observableExpressions;
    final ObservableSet<File> observableFiles;
    final SimpleObjectProperty<Orientation> panesOrientation;
    final ObservableList<Double> paneDividerPositions;
    final BindableValue<Font> font;
    final BooleanProperty enableFilters;

    ConfigProperties() {
        standardLogColors = new SimpleObjectProperty<>( new LogLineColors( Color.BLACK, Color.LIGHTGREY ) );
        observableExpressions = FXCollections.observableArrayList();
        observableFiles = FXCollections.observableSet( new LinkedHashSet<>( 4 ) );
        panesOrientation = new SimpleObjectProperty<>( Orientation.HORIZONTAL );
        paneDividerPositions = FXCollections.observableArrayList();
        font = new BindableValue<>( Font.font( FxUtils.isMac() ? "Monaco" : "Courier New" ) );
        enableFilters = new SimpleBooleanProperty( false );
    }
}
