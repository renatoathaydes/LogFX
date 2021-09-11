package com.athaydes.logfx.config;

import com.athaydes.logfx.binding.BindableValue;
import com.athaydes.logfx.data.LogFile;
import com.athaydes.logfx.data.LogLineColors;
import com.athaydes.logfx.ui.FxUtils;
import com.athaydes.logfx.ui.MustCallOnJavaFXThread;
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
    private static final LogLineColors DEFAULT_LOG_LINE_COLORS = new LogLineColors( Color.BLACK, Color.LIGHTGREY );
    private static final Font DEFAULT_FONT = Font.font( FxUtils.isMac() ? "Monaco" : "Courier New" );

    final SimpleObjectProperty<LogLineColors> standardLogColors;
    final ObservableSet<LogFile> observableFiles;
    final SimpleObjectProperty<Orientation> panesOrientation;
    final SimpleObjectProperty<Bounds> windowBounds;
    final ObservableList<Double> paneDividerPositions;
    final BindableValue<Font> font;
    final BooleanProperty enableFilters;
    final HighlightGroups highlightGroups;

    ConfigProperties() {
        standardLogColors = new SimpleObjectProperty<>( DEFAULT_LOG_LINE_COLORS );
        observableFiles = FXCollections.observableSet( new LinkedHashSet<>( 4 ) );
        highlightGroups = new HighlightGroups( observableFiles );
        panesOrientation = new SimpleObjectProperty<>( Orientation.HORIZONTAL );
        windowBounds = new SimpleObjectProperty<>( null );
        paneDividerPositions = FXCollections.observableArrayList();
        font = new BindableValue<>( DEFAULT_FONT );
        enableFilters = new SimpleBooleanProperty( false );
    }

    @MustCallOnJavaFXThread
    void clear() {
        standardLogColors.set( DEFAULT_LOG_LINE_COLORS );
        observableFiles.clear();
        highlightGroups.clear();
        panesOrientation.set( Orientation.HORIZONTAL );
        windowBounds.set( null );
        paneDividerPositions.clear();
        font.setValue( DEFAULT_FONT );
        enableFilters.set( false );
    }
}
