package com.athaydes.logfx

import com.athaydes.logfx.ui.FxUtils
import groovy.transform.CompileStatic
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.stage.Stage

@CompileStatic
class LogFXStyler extends Application {

    @Override
    void start( Stage primaryStage ) throws Exception {
        def root = new HBox( 10.0 )

        def button = new Button( 'Close' )
        button.background = FxUtils.simpleBackground( Color.DARKGRAY )

        button.setOnMouseEntered( { event ->
            button.background = FxUtils.simpleBackground( Color.LIGHTGRAY )
        } )
        button.setOnMouseExited( { event ->
            button.background = FxUtils.simpleBackground( Color.DARKGRAY )
        } )

        root.children.addAll( new Label( 'Hello' ), button )

        primaryStage.scene = new Scene( root, 300, 300 )
        primaryStage.show()
    }
}
