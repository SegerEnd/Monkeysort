package com.segerend

import javafx.beans.value.ChangeListener
import javafx.scene.canvas.Canvas
import javafx.scene.control.Label
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color

class ScreenCanvas(
    private val canvasFillColor: Color = Color.LIGHTBLUE
) : StackPane() {

    private val canvas = Canvas()
    private val label = Label("Hello World!")

    init {
        children.addAll(canvas, label)
        label.textFill = Color.BLACK

        // Make canvas always match the size of this StackPane
        canvas.widthProperty().bind(widthProperty())
        canvas.heightProperty().bind(heightProperty())

        // Redraw whenever size changes
        val redrawListener = ChangeListener<Number> { _, _, _ -> draw() }
        widthProperty().addListener(redrawListener)
        heightProperty().addListener(redrawListener)
    }

    private fun draw() {
        val gc = canvas.graphicsContext2D
        val w = canvas.width
        val h = canvas.height

        gc.clearRect(0.0, 0.0, w, h)

        gc.fill = canvasFillColor
        gc.fillRect(0.0, 0.0, w, h)
    }
}