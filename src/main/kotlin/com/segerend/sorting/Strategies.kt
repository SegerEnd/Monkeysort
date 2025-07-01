package com.segerend.sorting

import com.segerend.Fruit
import com.segerend.GridModel
import com.segerend.Pos
import com.segerend.ShuffleTask
import kotlin.random.Random

// --- Sorting Strategies ---

class BogoSortStrategy : SortStrategy {
    override fun getNextTask(grid: GridModel): ShuffleTask? {
        val from = Pos(Random.nextInt(grid.rows), Random.nextInt(grid.cols))
        if (grid.get(from) == Fruit.EMPTY) return null
        var to: Pos
        do { to = Pos(Random.nextInt(grid.rows), Random.nextInt(grid.cols)) } while (to == from)
        return ShuffleTask(from, to, grid.get(from))
    }
}

class BubbleSortStrategy(val rows: Int, val cols: Int) : SortStrategy {
    private var bubbleSortIndex = 0
    private var bubbleSortPass = 0

    override fun getNextTask(grid: GridModel): ShuffleTask? {
        val totalCells = rows * cols
        while (bubbleSortIndex < totalCells - 1 - bubbleSortPass) {
            val indexA = bubbleSortIndex
            val indexB = indexA + 1
            val from = Pos(indexA / cols, indexA % cols)
            val to = Pos(indexB / cols, indexB % cols)
            bubbleSortIndex++
            if (grid.get(from) != Fruit.EMPTY && grid.get(to) != Fruit.EMPTY && grid.get(from).name > grid.get(to).name) {
                return ShuffleTask(from, to, grid.get(from))
            }
        }
        bubbleSortIndex = 0
        bubbleSortPass++
        if (bubbleSortPass >= totalCells - 1) bubbleSortPass = 0
        return null
    }
}

class InsertionSortStrategy(val rows: Int, val cols: Int) : SortStrategy {
    private var sortedIndex = 1
    private var compareIndex = 1
    private var didSwap = false

    override fun getNextTask(grid: GridModel): ShuffleTask? {
        val totalCells = rows * cols

        if (sortedIndex >= totalCells) {
            return null
        }

        if (didSwap) {
            // After swap, move compareIndex backward
            compareIndex--
            didSwap = false
        }

        if (compareIndex > 0) {
            val indexA = compareIndex - 1
            val indexB = compareIndex
            val posA = Pos(indexA / cols, indexA % cols)
            val posB = Pos(indexB / cols, indexB % cols)

            if (grid.get(posA) != Fruit.EMPTY && grid.get(posB) != Fruit.EMPTY && grid.get(posA).name > grid.get(posB).name) {
                didSwap = true
                return ShuffleTask(posA, posB, grid.get(posA))
            } else {
                sortedIndex++
                compareIndex = sortedIndex
                return null
            }
        } else {
            sortedIndex++
            compareIndex = sortedIndex
            return null
        }
    }
}