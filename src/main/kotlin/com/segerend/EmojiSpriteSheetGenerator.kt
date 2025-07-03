import com.segerend.Fruit
import com.segerend.GameConfig
import javafx.application.Application
import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.stage.Stage
import javafx.embed.swing.SwingFXUtils
import javax.imageio.ImageIO
import java.io.File

class EmojiSpriteSheetGenerator : Application() {
    override fun start(stage: Stage) {
        val allFruits = Fruit.values().filter { it != Fruit.EMPTY }
        val sortedFruits = allFruits.sortedBy { it.name.lowercase() }
        val monkeyEmoji = if (GameConfig.DEFAULT_MONKEY.isNotEmpty()) GameConfig.DEFAULT_MONKEY else "🐒"

        val fruitEmojis = mutableListOf<Pair<String, String>>()

        // Insert monkey emoji at first
        fruitEmojis.add("Monkey" to monkeyEmoji)

        // Add the fruits after, sorted alphabetically
        sortedFruits.forEach { fruit ->
            fruitEmojis.add(fruit.name to fruit.emoji)
        }

        val cols = 4
        val rows = (fruitEmojis.size + cols - 1) / cols
        val emojiSize = 64.0

        val canvasWidth = cols * emojiSize
        val canvasHeight = rows * emojiSize

        val canvas = Canvas(canvasWidth, canvasHeight)
        val gc = canvas.graphicsContext2D

        // Background
        gc.fill = Color.WHITE
        gc.fillRect(0.0, 0.0, canvasWidth, canvasHeight)

        gc.fill = Color.BLACK
        gc.font = Font.font("Segoe UI Emoji", emojiSize * 0.8)

        fruitEmojis.forEachIndexed { index, (_, emoji) ->
            val col = index % cols
            val row = index / cols
            val x = col * emojiSize + emojiSize * 0.15
            val y = row * emojiSize + emojiSize * 0.75

            gc.fillText(emoji, x, y)
        }

        val writableImage = canvas.snapshot(null, null)
        val outputFile = File("src/main/resources/spritesheet.png")
        ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), "png", outputFile)
        println("Saved sprite sheet as: ${outputFile.absolutePath}")

        stage.close()
        System.exit(0)
    }
}

fun main() {
    Application.launch(EmojiSpriteSheetGenerator::class.java)
}