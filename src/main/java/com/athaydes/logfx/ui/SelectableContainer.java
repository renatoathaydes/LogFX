package com.athaydes.logfx.ui;

import com.athaydes.logfx.iterable.ObservableListView;
import javafx.scene.Node;

import java.util.concurrent.CompletionStage;

interface SelectableContainer {

    Node getNode();

    ObservableListView<? extends SelectionHandler.SelectableNode, Node> getSelectables();

    CompletionStage<SelectionHandler.SelectableNode> nextSelectable();

    CompletionStage<SelectionHandler.SelectableNode> previousSelectable();

    void scrollToView( SelectionHandler.SelectableNode node );
}
