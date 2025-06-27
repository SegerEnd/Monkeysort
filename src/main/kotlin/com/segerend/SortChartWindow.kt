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
    private val barChart = BarChart<String, Number>(xAxis, yAxis)

    init {
        barChart.title = "Sorting Progress"
        barChart.legendSide = Side.RIGHT
        xAxis.label = "Index"
        yAxis.label = "Fruit Ordinal"
        barChart.data.add(series)
        barChart.animated = false

        val root = BorderPane(barChart)
        val scene = Scene(root, 600.0, 400.0)
        stage.scene = scene
        stage.title = "Sorting Chart"
        stage.show()

        startUpdating()
    }

    private fun startUpdating() {
        // Filter out Fruit.EMPTY when generating sorted list
        val allFruitsSorted = Fruit.values()
            .filter { it != Fruit.EMPTY }
            .sortedBy { it.name }

        Platform.runLater {
            val grid = controller.gridModel.getGridCopy()
            val flatList = grid.flatten()

            series.data.clear()

            flatList.forEachIndexed { index, fruit ->
                if (fruit != Fruit.EMPTY) {
                    val rank = allFruitsSorted.indexOf(fruit).toDouble()
                    val data = XYChart.Data<String, Number>(index.toString(), rank)
                    series.data.add(data)
                } else {
                    // Add a zero-height bar or skip entirely.
                    // If skipping, bar positions may shift. If you want consistency, add a "null" or 0 bar.
                    val data = XYChart.Data<String, Number>(index.toString(), 0)
                    series.data.add(data)
                }
            }
        }

        fixedRateTimer("chart-updater", daemon = true, initialDelay = 500, period = 100) {
            val newGrid = controller.gridModel.getGridCopy().flatten()

            Platform.runLater {
                newGrid.forEachIndexed { index, fruit ->
                    if (index < series.data.size) {
                        if (fruit != Fruit.EMPTY) {
                            val targetRank = allFruitsSorted.indexOf(fruit).toDouble()
                            series.data[index].yValue = targetRank
                        } else {
                            series.data[index].yValue = 0 // Optional: or leave unchanged
                        }
                    }
                }
            }
        }
    }
}
