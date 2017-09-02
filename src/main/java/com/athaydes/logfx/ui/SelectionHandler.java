package com.athaydes.logfx.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds selection capabilities to a {@link Parent} node.
 */
class SelectionHandler {

    private final Clipboard clipboard = new Clipboard();

    SelectionHandler( Parent root ) {
        EventHandler<MouseEvent> mousePressedEventHandler = event -> {
            onMousePressed( root, event );
            //event.consume();
        };
        root.addEventHandler( MouseEvent.MOUSE_PRESSED, mousePressedEventHandler );
    }

    private void onMousePressed( Parent root, MouseEvent event ) {
        Node target = ( Node ) event.getTarget();
        System.out.println( "Mouse pressed on root: " + root + ", target = " + target );
        SelectableNode singleSelectedNode;

        if ( getSelectedItems().size() == 1 ) {
            singleSelectedNode = getSelectedItems().get( 0 );
        } else {
            singleSelectedNode = null;
        }

        clipboard.unselectAll();

        // try to go up in the hierarchy until we find a selectable node or the root node
        while ( target != root && !( target instanceof SelectableNode ) ) {
            target = target.getParent();
        }

        if ( target instanceof SelectableNode ) {
            SelectableNode selectableTarget = ( SelectableNode ) target;
            if ( selectableTarget != singleSelectedNode ) {
                clipboard.select( selectableTarget, true );
            }
        }
    }

    ObservableList<SelectableNode> getSelectedItems() {
        return clipboard.getSelectedItems();
    }

    /**
     * This class is based on jfxtras-labs
     * <a href="https://github.com/JFXtras/jfxtras-labs/blob/8.0/src/main/java/jfxtras/labs/scene/control/window/Clipboard.java">Clipboard</a>
     * and
     * <a href="https://github.com/JFXtras/jfxtras-labs/blob/8.0/src/main/java/jfxtras/labs/util/WindowUtil.java">WindowUtil</a>
     */
    private class Clipboard {

        private final ObservableList<SelectableNode> selectedItems = FXCollections.observableArrayList();

        ObservableList<SelectableNode> getSelectedItems() {
            return selectedItems;
        }

        void select( SelectableNode node, boolean selected ) {
            if ( selected ) {
                selectedItems.add( node );
            } else {
                selectedItems.remove( node );
            }
            node.setSelect( selected );
        }

        void unselectAll() {
            List<SelectableNode> unselectList = new ArrayList<>( selectedItems );

            for ( SelectableNode node : unselectList ) {
                select( node, false );
            }
        }
    }

    public interface SelectableNode {
        void setSelect( boolean select );
    }
}