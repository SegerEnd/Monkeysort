import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.stage.Stage
import javafx.util.Duration

data class RaceCar(val id: Int, val name: String, var speed: Int)

class MainApp : Application() {

    private val cars = mutableListOf(
        RaceCar(1, "Bliksem", 60),
        RaceCar(2, "Vlam", 50),
        RaceCar(3, "Storm", 70),
        RaceCar(4, "Flits", 55),
        RaceCar(5, "Bliksem 2.0", 65)
    )

    private val carRects = mutableListOf<Rectangle>()

    override fun start(stage: Stage) {
        val root = VBox(20.0)

        val carsBox = HBox(10.0)
        cars.forEach { car ->
            val rect = Rectangle(50.0, 100.0, Color.color(Math.random(), Math.random(), Math.random()))
            carRects.add(rect)
            carsBox.children.add(rect)
        }

        val btnBubbleSort = Button("Sorteren (BubbleSort)")
        btnBubbleSort.setOnAction {
            bubbleSortWithAnimation(cars, carRects)
        }

        val btnQuickSort = Button("Sorteren (QuickSort)")
        btnQuickSort.setOnAction {
            quickSortWithAnimation(cars, carRects)
        }

        root.children.addAll(carsBox, btnBubbleSort, btnQuickSort)

        stage.scene = Scene(root, 400.0, 200.0)
        stage.title = "Racewagen Sortering Visualisatie"
        stage.show()
    }

    private fun bubbleSortWithAnimation(data: MutableList<RaceCar>, rects: MutableList<Rectangle>) {
        val timeline = Timeline()
        val n = data.size
        var i = 0
        var j = 0
        val steps = mutableListOf<() -> Unit>()

        while (i < n - 1) {
            j = 0
            while (j < n - i - 1) {
                val indexJ = j
                val indexJ1 = j + 1
                steps.add {
                    if (data[indexJ].speed > data[indexJ1].speed) {
                        // Swap data
                        val temp = data[indexJ]
                        data[indexJ] = data[indexJ1]
                        data[indexJ1] = temp
                        // Swap visuals
                        val tempRect = rects[indexJ]
                        rects[indexJ] = rects[indexJ1]
                        rects[indexJ1] = tempRect
                    }
                    updateVisualPositions(rects)
                }
                j++
            }
            i++
        }

        playSteps(timeline, steps)
    }

    private fun quickSortWithAnimation(data: MutableList<RaceCar>, rects: MutableList<Rectangle>) {
        val steps = mutableListOf<() -> Unit>()

        fun quickSort(low: Int, high: Int) {
            if (low < high) {
                val pi = partition(low, high)
                quickSort(low, pi - 1)
                quickSort(pi + 1, high)
            }
        }

        fun partition(low: Int, high: Int): Int {
            val pivot = data[high].speed
            var i = low - 1
            for (j in low until high) {
                if (data[j].speed < pivot) {
                    i++
                    if (i != j) {
                        val idxI = i
                        val idxJ = j
                        steps.add {
                            val temp = data[idxI]
                            data[idxI] = data[idxJ]
                            data[idxJ] = temp
                            val tempRect = rects[idxI]
                            rects[idxI] = rects[idxJ]
                            rects[idxJ] = tempRect
                            updateVisualPositions(rects)
                        }
                    }
                }
            }
            steps.add {
                val temp = data[i + 1]
                data[i + 1] = data[high]
                data[high] = temp
                val tempRect = rects[i + 1]
                rects[i + 1] = rects[high]
                rects[high] = tempRect
                updateVisualPositions(rects)
            }
            return i + 1
        }

        quickSort(0, data.size - 1)

        val timeline = Timeline()
        playSteps(timeline, steps)
    }

    private fun playSteps(timeline: Timeline, steps: List<() -> Unit>) {
        timeline.keyFrames.clear()
        steps.forEachIndexed { index, step ->
            val frame = KeyFrame(Duration.seconds(index * 0.5)) {
                step()
            }
            timeline.keyFrames.add(frame)
        }
        timeline.play()
    }

    private fun updateVisualPositions(rects: List<Rectangle>) {
        rects.forEachIndexed { index, rect ->
            rect.translateX = index * 60.0
        }
    }
}

fun main() {
    Application.launch(MainApp::class.java)
}
