package com.athaydes.logfx.ui;

import com.athaydes.logfx.iterable.IterableUtils;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Adds selection capabilities to a {@link Parent} node.
 */
final class SelectionHandler {

    private final SelectionManager selectionManager = new SelectionManager();
    private final SelectableContainer root;

    private SelectableNode dragEventStartedOnNode;

    SelectionHandler( SelectableContainer root ) {
        this.root = root;
        var node = root.getNode();
        node.setOnDragDetected( this::onDragDetected );
        node.setOnMouseReleased( this::onMouseReleased );
        node.setOnMouseExited( this::onMouseReleased );

        root.getSelectables().getList().addListener( ( ListChangeListener<Node> ) change -> {
            if ( change.next() ) {
                for ( Node newNode : change.getAddedSubList() ) {
                    if ( newNode instanceof SelectableNode selectableNode ) {
                        enableDragEventsOn( selectableNode );
                    }
                }
                for ( Node removedNode : change.getRemoved() ) {
                    if ( removedNode instanceof SelectableNode selectableNode ) {
                        disableDragEventsOn( selectableNode );
                    }
                }
            }
        } );
    }

    private void onDragDetected( MouseEvent event ) {
        Node target = getTargetNode( event.getTarget() );

        if ( target instanceof SelectableNode selectableNode ) {
            dragEventStartedOnNode = selectableNode;
            root.getNode().startFullDrag();

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
            if ( target instanceof SelectableNode selectableTarget ) {
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
        if ( objectTarget instanceof Node target ) {
            // try to go up in the hierarchy until we find a selectable node or the root node
            while ( target != root.getNode() && !( target instanceof SelectableNode ) ) {
                target = target.getParent();
            }

            return target;
        } else {
            return root.getNode();
        }
    }

    Optional<ClipboardContent> getSelection() {
        return selectionManager.getContent();
    }

    ObservableSet<SelectableNode> getSelectedItems() {
        return selectionManager.getSelectedItems();
    }

    void select( SelectableNode node ) {
        selectionManager.unselectAll();

        selectionManager.select( node, true );
        // only scroll to view every 10th line to avoid jumping around too much
//        var scrollToView = node.getLineIndex() % 10 == 0;
//        if (scrollToView) {
        root.scrollToView( node );
//        }
    }

    CompletionStage<? extends SelectableNode> getNextItem() {
        return IterableUtils.getLast( getSelectedItems() ).map( last -> {
            var returnNext = false;
            for ( var node : root.getSelectables().getIterable() ) {
                if ( returnNext ) {
                    return CompletableFuture.completedFuture( node );
                }
                if ( node == last ) {
                    returnNext = true;
                }
            }
            return root.nextSelectable();
        } ).orElseGet( CompletableFuture::new );
    }

    CompletionStage<? extends SelectableNode> getPreviousItem() {
        return IterableUtils.getFirst( getSelectedItems() ).map( first -> {
            SelectableNode previous = null;
            for ( var node : root.getSelectables().getIterable() ) {
                if ( node == first ) {
                    if ( previous != null ) {
                        return CompletableFuture.completedFuture( previous );
                    }
                    break;
                }
                previous = node;
            }
            return root.previousSelectable();
        } ).orElseGet( CompletableFuture::new );
    }

    private void enableDragEventsOn( SelectableNode node ) {
        node.getNode().setOnMouseDragEntered( event -> {
            if ( dragEventStartedOnNode != null ) {
                selectAllBetween( node, dragEventStartedOnNode );
                event.consume();
            }
        } );
    }

    private void disableDragEventsOn( SelectableNode node ) {
        node.getNode().setOnMouseDragEntered( null );
        selectionManager.select( node, false );
    }

    void selectAllBetween( SelectableNode start, SelectableNode end ) {
        boolean selecting = false;
        for ( SelectableNode selectableNode : root.getSelectables().getIterable() ) {
            if ( selecting ) {
                if ( selectableNode == start || selectableNode == end ) {
                    selecting = false;
                }
                selectionManager.select( selectableNode, true );
            } else {
                if ( selectableNode == start || selectableNode == end ) {
                    selecting = ( start != end );
                    selectionManager.select( selectableNode, true );
                } else {
                    selectionManager.select( selectableNode, false );
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

        Node getNode();

        int getLineIndex();
    }
}