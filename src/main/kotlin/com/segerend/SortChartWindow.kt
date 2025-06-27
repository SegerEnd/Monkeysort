package com.segerend

import javafx.application.Platform
import javafx.geometry.Side
import javafx.scene.Scene
import javafx.scene.chart.*
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import kotlin.concurrent.fixedRateTimer
import java.util.logging.Level
import java.util.logging.Logger


class SortChartWindow private constructor(private val controller: GameController) {
    val stage = Stage()

    init {
        // Setup your chart window as before
        val xAxis = CategoryAxis()
        val yAxis = NumberAxis()
        val series = XYChart.Series<String, Number>()
        val barChart = BarChart<String, Number>(xAxis, yAxis)

        barChart.title = "Sorting Chart"
        barChart.legendSide = Side.RIGHT
        xAxis.label = "Index in Grid"
        yAxis.label = "Alphabetical Rank"
        barChart.data.add(series)
        barChart.animated = false

        val root = BorderPane(barChart)
        val scene = Scene(root, 600.0, 400.0)
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

        Platform.runLater {
            val grid = controller.gridModel.getGridCopy()
            val flatList = grid.flatten()

            series.data.clear()

            flatList.forEachIndexed { index, fruit ->
                val rank = if (fruit != Fruit.EMPTY) allFruitsSorted.indexOf(fruit).toDouble() else 0.0
                val data = XYChart.Data<String, Number>(index.toString(), rank)
                series.data.add(data)

                // After the node is created, set the bar color
                data.nodeProperty().addListener { _, _, node ->
                    node?.style = "-fx-bar-fill: ${fruit.color.toRgbString()};"
                }
            }
        }

        fixedRateTimer("chart-updater", daemon = true, initialDelay = 500, period = 100) {
            val newGrid = controller.gridModel.getGridCopy().flatten()

            Platform.runLater {
                newGrid.forEachIndexed { index, fruit ->
                    if (index < series.data.size) {
                        val data = series.data[index]
                        val targetRank = if (fruit != Fruit.EMPTY) allFruitsSorted.indexOf(fruit).toDouble() else 0.0
                        data.yValue = targetRank

                        // Update the bar color on update
                        data.node?.style = "-fx-bar-fill: ${fruit.color.toRgbString()};"
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
    }
}
