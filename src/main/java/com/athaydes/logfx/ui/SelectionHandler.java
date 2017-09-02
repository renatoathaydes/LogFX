package com.athaydes.logfx.ui;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableSet;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adds selection capabilities to a {@link Parent} node.
 */
class SelectionHandler {

    private final SelectionManager selectionManager = new SelectionManager();
    private final Parent root;

    private SelectableNode dragEventStartedOnNode;

    SelectionHandler( Parent root ) {
        this.root = root;
        root.setOnDragDetected( this::onDragDetected );
        root.setOnMouseReleased( this::onMouseReleased );
        root.setOnMouseExited( this::onMouseReleased );

        root.getChildrenUnmodifiable().addListener( ( ListChangeListener<Node> ) change -> {
            if ( change.next() ) {
                for ( Node newNode : change.getAddedSubList() ) {
                    enableDragEventsOn( newNode );
                }
                for ( Node removedNode : change.getRemoved() ) {
                    disableDragEventsOn( removedNode );
                }
            }
        } );
    }

    private void onDragDetected( MouseEvent event ) {
        Node target = getTargetNode( event.getTarget() );

        if ( target instanceof SelectableNode ) {
            dragEventStartedOnNode = ( SelectableNode ) target;
            root.startFullDrag();

            // when we start dragging, we want to make sure only one node is selected
            selectionManager.unselectAll();

            // TODO allow dragging the selected content out of the log view
//        Dragboard db = root.startDragAndDrop( TransferMode.COPY );
//        db.setContent( selectionManager.getContent() );
        }
    }

    private void onMouseReleased( MouseEvent event ) {
        // only handle event if the node was not being dragged
        if ( event.getButton() == MouseButton.PRIMARY &&
                dragEventStartedOnNode == null ) {
            Node target = getTargetNode( event.getTarget() );
            if ( target instanceof SelectableNode ) {
                SelectableNode selectableTarget = ( SelectableNode ) target;

                // select only this node, unless it was selected before
                boolean wasSelected = getSelectedItems().contains( selectableTarget );
                selectionManager.unselectAll();
                if ( !wasSelected ) {
                    selectionManager.select( selectableTarget, true );
                }
            }
        }

        dragEventStartedOnNode = null;
    }

    private Node getTargetNode( Object objectTarget ) {
        if ( objectTarget instanceof Node ) {

            Node target = ( Node ) objectTarget;

            // try to go up in the hierarchy until we find a selectable node or the root node
            while ( target != root && !( target instanceof SelectableNode ) ) {
                target = target.getParent();
            }

            return target;
        } else {
            return root;
        }
    }

    Optional<ClipboardContent> getSelection() {
        return selectionManager.getContent();
    }

    ObservableSet<SelectableNode> getSelectedItems() {
        return selectionManager.getSelectedItems();
    }

    private void enableDragEventsOn( Node node ) {
        if ( node instanceof SelectableNode ) {
            SelectableNode selectableNode = ( SelectableNode ) node;
            node.setOnMouseDragEntered( event -> {
                if ( dragEventStartedOnNode != null ) {
                    selectAllBetween( selectableNode, dragEventStartedOnNode );
                    event.consume();
                }
            } );
        } else {
            throw new IllegalArgumentException( "node must be a SelectableNode" );
        }
    }

    private void disableDragEventsOn( Node node ) {
        if ( node instanceof SelectableNode ) {
            node.setOnMouseDragEntered( null );
            selectionManager.select( ( SelectableNode ) node, false );
        }
    }

    void selectAllBetween( SelectableNode start, SelectableNode end ) {
        boolean selecting = false;
        for ( Node node : root.getChildrenUnmodifiable() ) {
            if ( node instanceof SelectableNode ) {
                SelectableNode selectableNode = ( SelectableNode ) node;
                if ( selecting ) {
                    if ( node == start || node == end ) {
                        selecting = false;
                    }
                    selectionManager.select( selectableNode, true );
                } else {
                    if ( node == start || node == end ) {
                        selecting = ( start != end );
                        selectionManager.select( selectableNode, true );
                    } else {
                        selectionManager.select( selectableNode, false );
                    }
                }
            }
        }
    }

    private static class SelectionManager {

        private final ObservableSet<SelectableNode> selectedItems =
                FXCollections.observableSet( new LinkedHashSet<>() );

        ObservableSet<SelectableNode> getSelectedItems() {
            return selectedItems;
        }

        void select( SelectableNode node, boolean selected ) {
            boolean nodeAffected;
            if ( selected ) {
                nodeAffected = selectedItems.add( node );
            } else {
                nodeAffected = selectedItems.remove( node );
            }
            if ( nodeAffected ) {
                node.setSelect( selected );
            }
        }

        void unselectAll() {
            List<SelectableNode> unselectList = new ArrayList<>( selectedItems );

            for ( SelectableNode node : unselectList ) {
                select( node, false );
            }
        }

        Optional<ClipboardContent> getContent() {
            if ( selectedItems.isEmpty() ) {
                return Optional.empty();
            }

            ClipboardContent content = new ClipboardContent();
            content.putString( getSelectedItems().stream()
                    .map( SelectableNode::getText )
                    .collect( Collectors.joining( System.lineSeparator() ) ) );
            return Optional.of( content );
        }

    }

    public interface SelectableNode {
        void setSelect( boolean select );

        String getText();
    }
}