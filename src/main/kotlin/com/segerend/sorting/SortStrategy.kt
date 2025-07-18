package com.segerend.sorting

import com.segerend.GridModel
import com.segerend.monkey.ShuffleTask

enum class SortAlgorithm { BOGO, BUBBLE, INSERTION }

interface SortStrategy {
    fun getNextTask(grid: GridModel): ShuffleTask?
}

fun makeStrategy(algorithm: SortAlgorithm, rows: Int, cols: Int): SortStrategy = when (algorithm) {
    SortAlgorithm.BOGO -> BogoSortStrategy()
    SortAlgorithm.BUBBLE -> BubbleSortStrategy(rows, cols)
    SortAlgorithm.INSERTION -> InsertionSortStrategy(rows, cols)
}