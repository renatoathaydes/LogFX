package com.athaydes.logfx.binding;

import javafx.beans.value.ObservableValueBase;

/**
 * A value which can be bound to properties.
 */
public class BindableValue<T> extends ObservableValueBase<T> {

    private T value;

    public BindableValue( T value ) {
        this.value = value;
    }

    public void setValue( T value ) {
        this.value = value;
        fireValueChangedEvent();
    }

    @Override
    public T getValue() {
        return value;
    }

}
