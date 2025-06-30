package com.segerend

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GridModelTest {
    private lateinit var grid: GridModel

    @BeforeEach
    fun setUp() {
        grid = GridModel(5, 5) // 5x5 grid for testing
    }

    @Test
    fun `test initialization has no empty cells`() {
        val copy = grid.getGridCopy()
        copy.forEach { row ->
            row.forEach { fruit ->
                assertTrue(fruit != Fruit.EMPTY, "Grid should not contain any EMPTY cells")
            }
        }
    }

    @Test
    fun getGridCopy() {
        // Check if function returns a new instance that is a copy of the grid and not the same instance
        val copy = grid.getGridCopy()
        assertNotSame(grid, copy, "getGridCopy should return a new instance")
        val copy2 = grid.getGridCopy()
        assertNotSame(copy, copy2, "getGridCopy should return a new instance each time")
        // Check if copy and copy2 has the same fruit distribution as the copy grid
        assertEquals(grid.getGridCopy().contentDeepToString(), copy.contentDeepToString(), "getGridCopy should return a grid with the same content")
    }

    @Test
    fun isSorted() {
        // fill the grid with Fruit.EMPTY
        var sortedGrid = GridModel(5, 5)
        sortedGrid.fill(Fruit.EMPTY)
        // Test with an empty grid with no fruits
        assertTrue(sortedGrid.isSorted(), "An empty grid should be considered sorted")

        // Fill the grid with a sorted distribution of fruits
        // create a new grid it should fill the grid automatically with a random distribution of fruits
        sortedGrid = GridModel(5, 5)
        assertFalse(sortedGrid.isSorted(), "A newly created grid with random fruits should not be sorted")
    }

    @Test
    fun get() {
        // Test getting a cell that does not exist
        assertThrows(IndexOutOfBoundsException::class.java) {
            grid.get(Pos(-1, -1)) // Invalid position
        }
        // check getting a cell that exists and that it returns the correct fruit
        assertDoesNotThrow { grid.set(Pos(2, 2), Fruit.APPLE) }

        assertEquals(Fruit.APPLE, grid.get(Pos(2, 2)), "get should return the correct fruit at the specified position")
    }

    @Test
    fun set() {
        // Test setting a cell that does not exist
        assertThrows(IndexOutOfBoundsException::class.java) {
            grid.set(Pos(-1, -1), Fruit.BANANA) // Invalid position
        }
        // check setting a cell that exists and that it sets the correct fruit
        assertDoesNotThrow { grid.set(Pos(3, 1), Fruit.BANANA) }

        assertEquals(Fruit.BANANA, grid.get(Pos(3, 1)), "set should correctly set the fruit at the specified position")
    }

    @Test
    fun getComboCellsAt() {
        // clear the grid before testing
        var comboGrid = GridModel(5, 5)
        comboGrid.fill(Fruit.EMPTY)

        // Set up a grid with a 3-cell horizontal combo
        comboGrid.set(Pos(0, 0), Fruit.CHERRY)
        comboGrid.set(Pos(0, 1), Fruit.CHERRY)
        comboGrid.set(Pos(0, 2), Fruit.CHERRY)
        comboGrid.set(Pos(0, 3), Fruit.BANANA)

        val comboCells = comboGrid.getComboCellsAt(Pos(0, 1))
        assertEquals(3, comboCells.size, "getComboCellsAt should return 3 cells for a 3-cell combo")
        assertTrue(comboCells.contains(Pos(0, 1)), "Combo should include the center cell")

        comboGrid = GridModel(5, 5)
        comboGrid.fill(Fruit.EMPTY)
        // Set up a grid with a 4-cell vertical combo
        comboGrid.set(Pos(1, 0), Fruit.APPLE)
        comboGrid.set(Pos(2, 0), Fruit.APPLE)
        comboGrid.set(Pos(3, 0), Fruit.APPLE)
        comboGrid.set(Pos(4, 0), Fruit.APPLE)
        val verticalComboCells = comboGrid.getComboCellsAt(Pos(2, 0))
        assertEquals(4, verticalComboCells.size, "getComboCellsAt should return 4 cells for a 4-cell vertical combo")
        assertTrue(verticalComboCells.contains(Pos(2, 0)), "Combo should include the center cell")
        // Test with a cell that has no combo
        comboGrid = GridModel(5, 5)
        comboGrid.fill(Fruit.EMPTY)
        comboGrid.set(Pos(2, 2), Fruit.GRAPE)
        val noComboCells = comboGrid.getComboCellsAt(Pos(2, 2))
        assertEquals(0, noComboCells.size, "getComboCellsAt should return 0 for a cell with no combo but has a fruit")
    }

    @Test
    fun getSameFruitNeighborCount() {
        // clear the grid before testing
        val neighborGrid = GridModel(5, 5)
        neighborGrid.fill(Fruit.EMPTY)

        // Set up a grid with some fruits
        neighborGrid.set(Pos(0, 0), Fruit.APPLE)
        neighborGrid.set(Pos(0, 1), Fruit.APPLE)
        neighborGrid.set(Pos(1, 0), Fruit.BANANA)
        neighborGrid.set(Pos(1, 1), Fruit.BANANA)
        neighborGrid.set(Pos(2, 2), Fruit.CHERRY)

        assertEquals(2, neighborGrid.getSameFruitNeighborCount(Fruit.APPLE), "getSameFruitNeighborCount should return the correct count for APPLE")
        assertEquals(2, neighborGrid.getSameFruitNeighborCount(Fruit.BANANA), "getSameFruitNeighborCount should return the correct count for BANANA")
        assertEquals(1, neighborGrid.getSameFruitNeighborCount(Fruit.CHERRY), "getSameFruitNeighborCount should return 1 for CHERRY since it has no neighbors, only itself")
    }

    @Test
    fun getSameFruitCount() {
        // clear the grid before testing
        val countGrid = GridModel(5, 5)
        countGrid.fill(Fruit.EMPTY)

        // Set up a grid with some fruits
        countGrid.set(Pos(0, 0), Fruit.APPLE)
        countGrid.set(Pos(0, 1), Fruit.APPLE)
        countGrid.set(Pos(1, 0), Fruit.BANANA)
        countGrid.set(Pos(1, 1), Fruit.BANANA)
        countGrid.set(Pos(2, 2), Fruit.CHERRY)

        assertEquals(2, countGrid.getSameFruitCount(Fruit.APPLE), "getSameFruitCount should return the correct count for APPLE")
        assertEquals(2, countGrid.getSameFruitCount(Fruit.BANANA), "getSameFruitCount should return the correct count for BANANA")
        assertEquals(1, countGrid.getSameFruitCount(Fruit.CHERRY), "getSameFruitCount should return the correct count for CHERRY")
    }

    @Test
    fun testGetRowsAndGetCols() {
        val gridModel = GridModel(5, 7)
        assertEquals(5, gridModel.rows)
        assertEquals(7, gridModel.cols)
    }
}