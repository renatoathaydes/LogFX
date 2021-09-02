package com.athaydes.logfx.config;

import com.athaydes.logfx.binding.BindableValue;
import com.athaydes.logfx.data.LogFile;
import com.athaydes.logfx.data.LogLineColors;
import com.athaydes.logfx.ui.FxUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.LinkedHashSet;

final class ConfigProperties {
    final SimpleObjectProperty<LogLineColors> standardLogColors;
    final ObservableSet<LogFile> observableFiles;
    final SimpleObjectProperty<Orientation> panesOrientation;
    final SimpleObjectProperty<Bounds> windowBounds;
    final ObservableList<Double> paneDividerPositions;
    final BindableValue<Font> font;
    final BooleanProperty enableFilters;
    final HighlightGroups highlightGroups;

    ConfigProperties() {
        standardLogColors = new SimpleObjectProperty<>( new LogLineColors( Color.BLACK, Color.LIGHTGREY ) );
        observableFiles = FXCollections.observableSet( new LinkedHashSet<>( 4 ) );
        highlightGroups = new HighlightGroups( observableFiles );
        panesOrientation = new SimpleObjectProperty<>( Orientation.HORIZONTAL );
        windowBounds = new SimpleObjectProperty<>( null );
        paneDividerPositions = FXCollections.observableArrayList();
        font = new BindableValue<>( Font.font( FxUtils.isMac() ? "Monaco" : "Courier New" ) );
        enableFilters = new SimpleBooleanProperty( false );
    }
}
