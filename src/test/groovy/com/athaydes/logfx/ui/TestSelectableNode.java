package com.athaydes.logfx.ui;

import javafx.scene.shape.Box;

/**
 * Class written in Java because currently Groovy compiler cannot generate proper bytecode
 * for a child of a sealed class, as it seems.
 */
public abstract class TestSelectableNode extends Box implements SelectionHandler.SelectableNode {
}
