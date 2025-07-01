package com.segerend.sorting

import com.segerend.Fruit
import com.segerend.GameConfig
import com.segerend.GameStats
import com.segerend.GridModel
import com.segerend.LockManager
import com.segerend.Monkey
import com.segerend.Pos
import com.segerend.ShuffleTask
import com.segerend.SortAlgorithm
import com.segerend.particles.ParticleSystem
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InsertionSortStrategyTest {
    private lateinit var strategy: InsertionSortStrategy
    private lateinit var grid: GridModel

    val testRows = 5
    val testCols = 5

    @BeforeEach
    fun setUp() {
        grid = GridModel(testRows, testCols)
        strategy = InsertionSortStrategy(testRows, testCols)
    }

    @Test
    fun `test getNextTask moves out-of-order element`() {
        val strategy = InsertionSortStrategy(1, 3)
        val grid = GridModel(1, 3)
        grid.set(Pos(0, 0), Fruit.CHERRY)
        grid.set(Pos(0, 1), Fruit.BANANA)
        grid.set(Pos(0, 2), Fruit.APPLE)

        val task = strategy.getNextTask(grid)
        assertEquals(
            ShuffleTask(Pos(0, 0), Pos(0, 1), Fruit.CHERRY),
            task,
            "Should move CHERRY right to insert BANANA"
        )
    }

    @Test
    fun `test getNextTask returns null when sorted`() {
        val strategy = InsertionSortStrategy(1, 3)
        val grid = GridModel(1, 3)
        grid.set(Pos(0, 0), Fruit.APPLE)
        grid.set(Pos(0, 1), Fruit.BANANA)
        grid.set(Pos(0, 2), Fruit.CHERRY)

        assertNull(strategy.getNextTask(grid), "Should return null when already sorted")
    }

    fun printGrid(grid: GridModel) {
        for (row in 0 until grid.rows) {
            for (col in 0 until grid.cols) {
                val fruit = grid.get(Pos(row, col))
                print("${fruit.emoji} ")
            }
            println()
        }
    }

    @Test
    fun sortingTest() {
        GameConfig.ROWS = 4
        GameConfig.COLS = 4

        LockManager.clear()

        val monkey = Monkey(SortAlgorithm.INSERTION)
        val grid = GridModel(GameConfig.ROWS, GameConfig.COLS)
        val particleSystem = ParticleSystem()
        GameStats.timeFactor = 1.0

        // Random test grid
        grid.set(Pos(0, 0), Fruit.GRAPE)
        grid.set(Pos(0, 1), Fruit.CHERRY)
        grid.set(Pos(0, 2), Fruit.BANANA)
        grid.set(Pos(0, 3), Fruit.APPLE)
        grid.set(Pos(1, 0), Fruit.ORANGE)

        println("Initial grid state:")
        printGrid(grid)

        var iterations = 0
        val maxIterations = 250000

        while (!grid.isSorted() && iterations < maxIterations) {
            if (monkey.isIdle()) {
                val task = monkey.strategy.getNextTask(grid)
                if (task != null) {
                    monkey.assignTask(task, GameConfig.CELL_SIZE)
                }
            }
            monkey.update(0.01.toDouble(), grid, GameConfig.CELL_SIZE, particleSystem)
            iterations++
        }

        println("Sorting completed in $iterations iterations")
        println("Final grid state:")
        printGrid(grid)
        assertTrue(grid.isSorted(), "Grid should be sorted after insertion sort")
        LockManager.clear()
    }

    @Test
    fun getRows() {
        assertEquals(testRows, strategy.rows, "getRows should return the number of rows set in the constructor")
    }

    @Test
    fun getCols() {
        assertEquals(testCols, strategy.cols, "getCols should return the number of columns set in the constructor")
    }
}
