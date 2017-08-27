package com.athaydes.logfx.ui;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method that must be called only from the JavaFX Thread.
 */
@Retention( RetentionPolicy.SOURCE )
@Target( { ElementType.METHOD, ElementType.CONSTRUCTOR } )
public @interface MustCallOnJavaFXThread {
}
