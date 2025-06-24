package com.segerend

import javafx.animation.AnimationTimer
import javafx.geometry.Pos
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane

class ScreenCanvas : Pane() {

    private val skyImage = Image(this::class.java.getResource("/sky.png")!!.toExternalForm())
    private val scrollSpeed = 0.05

    // We will use two ImageViews side by side for smooth scrolling
    private val imageView1 = ImageView(skyImage)
    private val imageView2 = ImageView(skyImage)

    private var offsetX = 0.0

    init {
        // Keep aspect ratio and scale to fit height
        imageView1.isPreserveRatio = true
        imageView2.isPreserveRatio = true
        imageView1.fitHeightProperty().bind(heightProperty())
        imageView2.fitHeightProperty().bind(heightProperty())

        // Put both images in an HBox side by side (no spacing)
        val hbox = HBox(imageView1, imageView2)
        hbox.alignment = Pos.TOP_LEFT
        children.add(hbox)

        // Clip the content to this pane size
        clip = javafx.scene.shape.Rectangle().apply {
            widthProperty().bind(this@ScreenCanvas.widthProperty())
            heightProperty().bind(this@ScreenCanvas.heightProperty())
        }

        // AnimationTimer for scrolling
        val baseScrollFraction = 0.001  // fraction of image width per frame

        object : AnimationTimer() {
            override fun handle(now: Long) {
                val imgWidth = imageView1.boundsInParent.width
                val scrollSpeed = imgWidth * baseScrollFraction

                offsetX -= scrollSpeed
                if (offsetX <= -imgWidth) {
                    offsetX += imgWidth
                }
                hbox.translateX = offsetX
            }
        }.start()

    }
}
