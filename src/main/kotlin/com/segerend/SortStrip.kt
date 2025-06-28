package com.segerend

import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.paint.CycleMethod
import javafx.scene.paint.RadialGradient
import javafx.scene.text.Font

class SortStrip {
    fun draw(gc: GraphicsContext, grid: GridModel) {
        val stripWidth = gc.canvas.width
        val stripHeight = GameConfig.STRIP_HEIGHT

        val fruits = Fruit.values()
            .filter { it != Fruit.EMPTY }
            .sortedBy { it.name }

        val count = fruits.size

        val spacing = 2.0
        val totalSpacing = spacing * (count - 1)
        val squareWidth = (stripWidth - totalSpacing) / count
        val squareHeight = stripHeight

        val fontSize = 18.0
        gc.font = Font.font(fontSize)

        // loop through each fruit in alphabetical order
        for ((index, fruit) in fruits.withIndex()) {
            val maxCount = grid.getSameFruitCount(fruit)
            val neighborCount = grid.getSameFruitNeighborCount(fruit)
            val grayScaleFactor = if (maxCount > 0) neighborCount.toDouble() / maxCount else 0.0

            val x = index * (squareWidth + spacing)
            val y = gc.canvas.height - stripHeight

            fun blendColors(c1: Color, c2: Color, t: Double): Color {
                val r = c1.red * (1 - t) + c2.red * t
                val g = c1.green * (1 - t) + c2.green * t
                val b = c1.blue * (1 - t) + c2.blue * t
                return Color.color(r, g, b)
            }

            val centerColor = blendColors(Color.WHITE, fruit.color, grayScaleFactor)
            // edge color is a little bit more saturated than the center color
            val edgeColor = blendColors(Color.gray(0.8), fruit.color, grayScaleFactor * 0.8)

            val gradient = RadialGradient(
                0.0, 0.0,
                x + squareWidth / 2, y + squareHeight / 2,
                squareWidth / 1.5,
                false, CycleMethod.NO_CYCLE,
                javafx.scene.paint.Stop(0.0, centerColor),
                javafx.scene.paint.Stop(1.0, edgeColor)
            )

            gc.fill = gradient
            gc.fillRect(x, y, squareWidth, squareHeight)

            // Use Text node to measure emoji width
            val emojiText = javafx.scene.text.Text(fruit.emoji)
            emojiText.font = gc.font
            val textWidth = emojiText.layoutBounds.width
            val textHeight = emojiText.layoutBounds.height

            val textX = x + (squareWidth - textWidth) / 2
            val textY = y + (squareHeight + textHeight) / 2 - 4 // fine-tuned vertical centering

            gc.fill = centerColor
            gc.fillText(fruit.emoji, textX, textY)

            if (maxCount == neighborCount) {
                gc.stroke = Color.BLACK
                gc.lineWidth = 2.0
                gc.strokeRect(x, y, squareWidth, squareHeight)
                gc.fill = Color.LIMEGREEN
                gc.fillText("✅", x + squareWidth - 20, y + squareHeight - 5)
            }
        }
    }
}