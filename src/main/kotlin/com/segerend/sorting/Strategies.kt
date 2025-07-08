package com.segerend.sorting

import com.segerend.Fruit
import com.segerend.GridModel
import com.segerend.Pos
import com.segerend.monkey.ShuffleTask
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
    private var currentIndex = 1
    private var compareIndex = 1
    private var justSwapped = false

    override fun getNextTask(grid: GridModel): ShuffleTask? {
        val totalCells = rows * cols

        // If we reach end, start over to keep sorting continuously
        if (currentIndex >= totalCells) {
            currentIndex = 1
            compareIndex = currentIndex
        }

        // After returning a swap task, move back one to keep checking previous pairs
        if (justSwapped) {
            compareIndex--
            justSwapped = false
        }

        // If compareIndex at start, move forward to next element to insert
        if (compareIndex <= 0) {
            currentIndex++
            compareIndex = currentIndex
        }

        val indexA = compareIndex - 1
        val indexB = compareIndex

        val posA = Pos(indexA / cols, indexA % cols)
        val posB = Pos(indexB / cols, indexB % cols)

        val fruitA = grid.get(posA)
        val fruitB = grid.get(posB)

        if (fruitA != Fruit.EMPTY && fruitB != Fruit.EMPTY && fruitA.name > fruitB.name) {
            justSwapped = true
            return ShuffleTask(posA, posB, fruitA)
        } else {
            currentIndex++
            compareIndex = currentIndex
            if (currentIndex >= totalCells) {
                currentIndex = 1
                compareIndex = currentIndex
            }
            return null
        }
    }
}
