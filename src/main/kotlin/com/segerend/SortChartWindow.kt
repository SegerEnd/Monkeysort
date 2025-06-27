package com.segerend

import javafx.application.Platform
import javafx.geometry.Side
import javafx.scene.Scene
import javafx.scene.chart.*
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import kotlin.concurrent.fixedRateTimer

class SortChartWindow(private val controller: GameController) {

    private val stage = Stage()
    private val xAxis = CategoryAxis()
    private val yAxis = NumberAxis()
    private val series = XYChart.Series<String, Number>()
    private val lineChart = LineChart<String, Number>(xAxis, yAxis)

    init {
        lineChart.title = "Sorting Progress"
        lineChart.legendSide = Side.RIGHT
        xAxis.label = "Index"
        yAxis.label = "Fruit Ordinal"
        lineChart.data.add(series)
        lineChart.animated = true

        val root = BorderPane(lineChart)
        val scene = Scene(root, 600.0, 400.0)
        stage.scene = scene
        stage.title = "Sorting Chart"
        stage.show()

        startUpdating()
    }

    private fun startUpdating() {
        val allFruitsSorted = Fruit.values().sortedBy { it.name }

        // Initialize data once
        Platform.runLater {
            val grid = controller.gridModel.getGridCopy()
            val flatList = grid.flatten()

            series.data.clear()
            flatList.forEachIndexed { index, fruit ->
                val rank = allFruitsSorted.indexOf(fruit)
                val data = XYChart.Data<String, Number>(index.toString(), rank.toDouble() as Number)
                series.data.add(data)
            }
        }

        fixedRateTimer("chart-updater", daemon = true, initialDelay = 500, period = 250) {
            val newGrid = controller.gridModel.getGridCopy().flatten()

            Platform.runLater {
                // Update existing data points, don't clear and recreate
                newGrid.forEachIndexed { index, fruit ->
                    if (index < series.data.size) {
                        val rank = allFruitsSorted.indexOf(fruit)
                        series.data[index].yValue = rank.toDouble() as Number
                    }
                }
            }
        }
    }
}
