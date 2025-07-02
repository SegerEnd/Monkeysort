package com.segerend

import javafx.application.Platform
import javafx.geometry.Side
import javafx.scene.Scene
import javafx.scene.chart.*
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import javafx.scene.control.Button
import javafx.geometry.Pos
import javafx.scene.layout.HBox
import javafx.geometry.Insets
import kotlin.concurrent.fixedRateTimer
import java.util.logging.Level
import java.util.logging.Logger

class SortChartWindow internal constructor(private val controller: GameController) {
    val stage = Stage()
    val root = BorderPane()

    init {
        val xAxis = CategoryAxis()
        val yAxis = NumberAxis()
        val series = XYChart.Series<String, Number>()
        val barChart = BarChart<String, Number>(xAxis, yAxis)

        barChart.animated = false

        val toggleAnimButton = Button("Enable Animations").apply {
            id = "toggleAnimButton"
            setOnAction {
                barChart.animated = !barChart.animated
                text = if (barChart.animated) "Disable Animations" else "Enable Animations"
            }
        }

        val buttonBox = HBox(toggleAnimButton)
        buttonBox.alignment = Pos.CENTER
        buttonBox.padding = Insets(10.0)

        root.center = barChart
        root.bottom = buttonBox

        val scene = Scene(root, 600.0, 400.0)

        barChart.title = "Sorting Chart"
        barChart.legendSide = Side.RIGHT
        xAxis.label = "Index in Grid"
        yAxis.label = "Alphabetical Rank"
        barChart.data.add(series)
        barChart.animated = false

        stage.scene = scene
        stage.title = "Sorting Chart"

        stage.setOnCloseRequest {
            instance = null
        }

        startUpdating(series)
    }

    private fun startUpdating(series: XYChart.Series<String, Number>) {
        val allFruitsSorted = Fruit.values()
            .filter { it != Fruit.EMPTY }
            .sortedBy { it.name }

        val grid = controller.gridModel.getGridCopy()
        val flatList = grid.flatten()

        val BATCH_SIZE = 250

        // Load first batch quickly
        Thread {
            val initialData: List<XYChart.Data<String, Number>> =
                flatList.take(BATCH_SIZE).mapIndexed { index, fruit ->
                    val rank = if (fruit != Fruit.EMPTY) allFruitsSorted.indexOf(fruit).toDouble() else 0.0
                    XYChart.Data(index.toString(), rank)
                }

            Platform.runLater {
                series.data.setAll(initialData)
            }

            // Load remaining data in small chunks
            val remainingData = flatList.drop(BATCH_SIZE).mapIndexed { i, fruit ->
                val index = i + BATCH_SIZE
                val rank = if (fruit != Fruit.EMPTY) allFruitsSorted.indexOf(fruit).toDouble() else 0.0
                XYChart.Data(index.toString(), rank)
            }

            remainingData.chunked(10).forEach { chunk ->
                Thread.sleep(50)
                Platform.runLater {
                    series.data.addAll(chunk as Collection<XYChart.Data<String, Number>>)
                }
            }
        }.start()

        // Periodic chart updates (for sorting changes)
        fixedRateTimer("chart-updater", daemon = true, initialDelay = 500, period = 100) {
            val newGrid = controller.gridModel.getGridCopy().flatten()

            Platform.runLater {
                newGrid.forEachIndexed { index, fruit ->
                    if (index < series.data.size) {
                        val data = series.data[index]
                        val targetRank = if (fruit != Fruit.EMPTY) allFruitsSorted.indexOf(fruit).toDouble() else 0.0
                        data.yValue = targetRank

                        // Optional: apply color styling lazily
                        data.node?.let { node ->
                            node.style = "-fx-bar-fill: ${fruit.color.toRgbString()};"
                        }
                    }
                }
            }
        }
    }

    companion object {
        private var instance: SortChartWindow? = null

        fun show(controller: GameController) {
            try {
                if (instance == null) {
                    instance = SortChartWindow(controller)
                    instance!!.stage.show()
                } else {
                    if (!instance!!.stage.isShowing) {
                        instance!!.stage.show()
                    }
                    instance!!.stage.toFront()
                    instance!!.stage.requestFocus()
                }
            } catch (e: Exception) {
                Logger.getLogger(SortChartWindow::class.java.name)
                    .log(Level.SEVERE, "Failed to show SortChartWindow", e)
            }
        }

        fun getInstance(): SortChartWindow? {
            return instance
        }
    }
}
