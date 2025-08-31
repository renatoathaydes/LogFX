package com.athaydes.logfx.ui;

import javafx.beans.property.LongProperty;
import javafx.scene.control.TextField;

import java.util.OptionalLong;

final class LongField extends TextField {

    LongField( LongProperty value ) {
        getStyleClass().add( "long-field" );
        setText( Long.toString( value.get() ) );
        textProperty().addListener( ( observable, oldValue, newValue ) -> {
            var maybeValue = getPositiveLong( newValue );
            if ( maybeValue.isPresent() ) {
                value.set( maybeValue.getAsLong() );
                getStyleClass().remove( "error" );
            } else {
                FxUtils.addIfNotPresent( getStyleClass(), "error" );
            }
        } );
    }

    private OptionalLong getPositiveLong( String newValue ) {
        try {
            var i = Long.parseLong( newValue );
            return i >= 0 ? OptionalLong.of( i ) : OptionalLong.empty();
        } catch ( NumberFormatException ignore ) {
            return OptionalLong.empty();
        }
    }

}
