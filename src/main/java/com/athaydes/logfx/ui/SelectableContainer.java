package com.athaydes.logfx.ui;

import com.athaydes.logfx.iterable.ObservableListView;
import javafx.scene.Node;

interface SelectableContainer {

    Node getNode();

    ObservableListView<? extends SelectionHandler.SelectableNode> getSelectables();

}
