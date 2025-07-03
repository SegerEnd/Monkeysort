package com.segerend

data class Pos(val row: Int, val col: Int)

class GridModel(val rows: Int = GameConfig.ROWS, val cols: Int = GameConfig.COLS) {
    private val grid = Array(rows) { Array(cols) { Fruit.random() } }

    fun getGridCopy(): Array<Array<Fruit>> = Array(rows) { r -> grid[r].clone() }
    fun isSorted(): Boolean = grid.flatten().zipWithNext().all { it.first.name <= it.second.name }
    fun get(pos: Pos): Fruit = grid[pos.row][pos.col]
    fun set(pos: Pos, fruit: Fruit) { grid[pos.row][pos.col] = fruit }

    fun fill(fruit: Fruit) {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                grid[r][c] = fruit
            }
        }
    }

    fun getComboCellsAt(pos: Pos): List<Pos> {
        val fruit = get(pos)
        if (fruit == Fruit.EMPTY) return emptyList() // Empty cell may not have combos

        fun collectMatches(direction: (Int) -> Pos): List<Pos> {
            val matches = mutableListOf(pos)
            var offset = 1
            while (true) {
                val nextPos = direction(offset++)
                if (nextPos.row !in 0 until rows || nextPos.col !in 0 until cols || get(nextPos) != fruit) break
                matches.add(nextPos)
            }
            return matches
        }

        val horizontal = collectMatches { Pos(pos.row, pos.col + it) } + collectMatches { Pos(pos.row, pos.col - it) }.drop(1)
        val vertical = collectMatches { Pos(pos.row + it, pos.col) } + collectMatches { Pos(pos.row - it, pos.col) }.drop(1)
        val result = mutableSetOf<Pos>()
        if (horizontal.size >= 3) result.addAll(horizontal)
        if (vertical.size >= 3) result.addAll(vertical)
        return result.toList()
    }

    fun getSameFruitCount(fruit: Fruit): Int {
        return grid.sumOf { row -> row.count { it == fruit } }
    }

    fun getSameFruitNeighborCount(fruit: Fruit): Int {
        val flatGrid = grid.flatten()
        var maxCount = 0
        var count = 0

        for (cell in flatGrid) {
            if (cell == fruit) {
                count++
            } else {
                maxCount = maxOf(maxCount, count)
                count = 0
            }
        }
        return maxOf(maxCount, count) // Handle streak at the end
    }
}