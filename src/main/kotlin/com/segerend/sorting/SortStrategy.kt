package com.segerend.sorting

import com.segerend.GameConfig
import com.segerend.GridModel
import com.segerend.ShuffleTask
import com.segerend.SortAlgorithm

interface SortStrategy {
    fun getNextTask(grid: GridModel): ShuffleTask?
}

fun makeStrategy(algorithm: SortAlgorithm, rows: Int = GameConfig.ROWS, cols: Int = GameConfig.COLS): SortStrategy = when (algorithm) {
    SortAlgorithm.BOGO -> BogoSortStrategy()
    SortAlgorithm.BUBBLE -> BubbleSortStrategy(rows, cols)
    SortAlgorithm.INSERTION -> InsertionSortStrategy(rows, cols)
}