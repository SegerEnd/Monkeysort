package com.segerend.sorting

import com.segerend.GridModel
import com.segerend.Pos
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.RepeatedTest

class BogoSortStrategyTest {
    private lateinit var strategy: BogoSortStrategy
    private lateinit var grid: GridModel

    @BeforeEach
    fun setUp() {
        grid = GridModel(5, 5) // Initialize a 5x5 grid for testing
        strategy = BogoSortStrategy() // Initialize the BogoSortStrategy
    }

    @RepeatedTest(25)
    fun getNextTask() {
        val grid = GridModel(5, 5)
        val task = strategy.getNextTask(grid)
        assertNotNull(task, "ShuffleTask should not be null")

        assertTrue(task?.from?.row in 0 until grid.rows, "from.row out of bounds")
        assertTrue(task?.from?.col in 0 until grid.cols, "from.col out of bounds")

        assertTrue(task?.to?.row in 0 until grid.rows, "to.row out of bounds")
        assertTrue(task?.to?.col in 0 until grid.cols, "to.col out of bounds")

        assertNotEquals(task?.from, task?.to, "'from' and 'to' positions must be different")

        val expectedFruit = grid.get(task?.from ?: Pos(0, 0))
        assertEquals(expectedFruit, task?.fruit, "ShuffleTask fruit must match grid.get(from)")
    }
}