package com.athaydes.logfx.ui;

import javafx.beans.property.ObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.shape.Rectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Utility class to install file drag-and-drop functionality onto Nodes.
 */
public class FileDragAndDrop {

    private static final Logger log = LoggerFactory.getLogger( FileDragAndDrop.class );

    public enum DropTarget {
        BEFORE, AFTER
    }

    /**
     * Install file drag-and-drop functionality onto the given view.
     *
     * @param view                to install on
     * @param logsPane            view panes
     * @param overlay             screen overlay
     * @param orientationProperty orientation of the panes
     * @param onDrop              action on drop
     */
    public static void install( LogView view,
                                LogViewPane logsPane,
                                Rectangle overlay,
                                ObjectProperty<Orientation> orientationProperty,
                                BiConsumer<File, DropTarget> onDrop ) {
        AtomicReference<ScrollPane> scrollPaneRef = new AtomicReference<>();

        view.setOnDragEntered( event -> {
            FxUtils.addIfNotPresent( view.getStyleClass(), "dropping-files" );
            FxUtils.addIfNotPresent( overlay.getStyleClass(), "shadow-overlay", "highlight" );
            ScrollPane scrollPane = logsPane.getScrollPaneFor( view );
            if ( scrollPane != null ) {
                scrollPaneRef.set( scrollPane );
                positionOverlay( overlay, event, scrollPane, orientationProperty.get() );
                overlay.setVisible( true );
                overlay.setMouseTransparent( true );
            } else {
                log.warn( "Cannot find ScrollPane for LogView" );
            }
        } );

        view.setOnDragExited( event -> {
            view.getStyleClass().remove( "dropping-files" );
            overlay.getStyleClass().remove( "shadow-overlay" );
            overlay.getStyleClass().remove( "highlight" );
            overlay.setVisible( false );
            overlay.setMouseTransparent( false );
        } );

        view.setOnDragOver( event -> {
            if ( event.getDragboard().hasFiles() ) {
                event.acceptTransferModes( TransferMode.ANY );
                positionOverlay( overlay, event, scrollPaneRef.get(), orientationProperty.get() );
            }
            event.consume();
        } );

        view.setOnDragDropped( event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;

            if ( db.hasFiles() ) {
                log.debug( "Dropping files: {}", db.getFiles() );
                for ( File draggedFile : db.getFiles() ) {
                    if ( draggedFile.isFile() ) {
                        ScrollPane scrollPane = scrollPaneRef.get();
                        if ( scrollPane != null ) {
                            var dropTarget =
                                    getDropTarget( view, event, scrollPane, orientationProperty.get() );
                            onDrop.accept( draggedFile, dropTarget );
                            success = true;
                        }
                    }
                }
            }

            event.setDropCompleted( success );
            event.consume();
        } );
    }

    private static void positionOverlay( Rectangle overlay,
                                         DragEvent event,
                                         ScrollPane scrollPane,
                                         Orientation orientation ) {
        var viewBounds = scrollPane.localToScene( scrollPane.getBoundsInLocal() );

        switch ( orientation ) {
            case VERTICAL -> {
                double halfHeight = scrollPane.getHeight() / 2.0;
                double halfMark = viewBounds.getMinY() + halfHeight;
                var isTopHalf = event.getSceneY() < halfMark;
                overlay.setX( 4 );
                overlay.setY( viewBounds.getMinY() + ( isTopHalf ? 4 : halfHeight - 4 ) );
                overlay.setHeight( halfHeight - 8 );
                overlay.setWidth( scrollPane.getWidth() - 8 );
            }
            case HORIZONTAL -> {
                double halfWidth = scrollPane.getWidth() / 2.0;
                double halfMark = viewBounds.getMinX() + halfWidth;
                var isLeftHalf = event.getSceneX() < halfMark;
                overlay.setX( viewBounds.getMinX() + ( isLeftHalf ? 4 : halfWidth - 4 ) );
                overlay.setY( 4 );
                overlay.setHeight( scrollPane.getHeight() - 8 );
                overlay.setWidth( halfWidth - 8 );
            }
        }
    }

    private static DropTarget getDropTarget( LogView view,
                                             DragEvent event,
                                             ScrollPane scrollPane,
                                             Orientation orientation ) {
        Bounds viewBounds = view.localToScene( view.getBoundsInLocal() );

        return switch ( orientation ) {
            case VERTICAL -> {
                double halfHeight = scrollPane.getHeight() / 2.0;
                double halfMark = viewBounds.getMinY() + halfHeight;
                var isTopHalf = event.getSceneY() < halfMark;
                yield isTopHalf ? DropTarget.BEFORE : DropTarget.AFTER;
            }
            case HORIZONTAL -> {
                double halfWidth = scrollPane.getWidth() / 2.0;
                double halfMark = viewBounds.getMinX() + halfWidth;
                var isLeftHalf = event.getSceneX() < halfMark;
                yield isLeftHalf ? DropTarget.BEFORE : DropTarget.AFTER;
            }
        };
    }

    /**
     * Install file drag-and-drop functionality onto the given Node.
     *
     * @param node   node to accept file drop
     * @param onDrop action on drop
     */
    public static void install( Node node,
                                Consumer<File> onDrop ) {
        node.setOnDragEntered( event -> FxUtils.addIfNotPresent( node.getStyleClass(), "dropping-files" ) );
        node.setOnDragExited( event -> node.getStyleClass().remove( "dropping-files" ) );

        node.setOnDragOver( event -> {
            if ( event.getDragboard().hasFiles() ) {
                event.acceptTransferModes( TransferMode.ANY );
            }
            event.consume();
        } );

        node.setOnDragDropped( event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;

            if ( db.hasFiles() ) {
                log.debug( "Dropping files: {}", db.getFiles() );
                for ( File draggedFile : db.getFiles() ) {
                    if ( draggedFile.isFile() ) {
                        onDrop.accept( draggedFile );
                        success = true;
                    }
                }
            }

            event.setDropCompleted( success );
            event.consume();
        } );
    }

}
