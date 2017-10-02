package com.athaydes.logfx.ui;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
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
     * @param view     to install on
     * @param logsPane view panes
     * @param overlay  screen overlay
     * @param onDrop   action on drop
     */
    public static void install( LogView view,
                                LogViewPane logsPane,
                                Rectangle overlay,
                                BiConsumer<File, DropTarget> onDrop ) {
        AtomicReference<ScrollPane> scrollPaneRef = new AtomicReference<>();

        view.setOnDragEntered( event -> {
            FxUtils.addIfNotPresent( view.getStyleClass(), "dropping-files" );
            FxUtils.addIfNotPresent( overlay.getStyleClass(), "shadow-overlay", "highlight" );
            ScrollPane scrollPane = logsPane.getScrollPaneFor( view );
            if ( scrollPane != null ) {
                scrollPaneRef.set( scrollPane );
                double halfHeight = scrollPane.getHeight() / 2.0;
                Bounds viewBounds = scrollPaneRef.get().localToScene( scrollPaneRef.get().getBoundsInLocal() );
                double halfMark = viewBounds.getMinY() + halfHeight;
                boolean isTopHalf = event.getSceneY() < halfMark;

                overlay.setVisible( true );
                overlay.setMouseTransparent( true );
                overlay.setX( 4 );
                overlay.setY( viewBounds.getMinY() + ( isTopHalf ? 4 : halfHeight - 4 ) );
                overlay.setHeight( halfHeight - 8 );
                overlay.setWidth( scrollPane.getWidth() - 8 );
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

                double halfHeight = scrollPaneRef.get().getHeight() / 2.0;
                Bounds viewBounds = scrollPaneRef.get().localToScene( scrollPaneRef.get().getBoundsInLocal() );
                double halfMark = viewBounds.getMinY() + halfHeight;
                boolean isTopHalf = event.getSceneY() < halfMark;

                overlay.setY( viewBounds.getMinY() + ( isTopHalf ? 4 : halfHeight - 4 ) );
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
                        ScrollPane scrollPane = logsPane.getScrollPaneFor( view );
                        if ( scrollPane != null ) {
                            Bounds viewBounds = view.localToScene( view.getBoundsInLocal() );
                            double halfMark = viewBounds.getMinY() + scrollPane.getHeight() / 2.0;
                            boolean isTopHalf = event.getSceneY() < halfMark;
                            onDrop.accept( draggedFile, isTopHalf ? DropTarget.BEFORE : DropTarget.AFTER );
                            success = true;
                        }
                    }
                }
            }

            event.setDropCompleted( success );
            event.consume();
        } );
    }

    /**
     * Install file drag-and-drop functionality onto the given Node.
     *
     * @param node   node to accept file drop
     * @param onDrop action on drop
     */
    public static void install( Node node,
                                Consumer<File> onDrop ) {
        node.setOnDragEntered( event -> {
            FxUtils.addIfNotPresent( node.getStyleClass(), "dropping-files" );
        } );

        node.setOnDragExited( event -> {
            node.getStyleClass().remove( "dropping-files" );
        } );

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
