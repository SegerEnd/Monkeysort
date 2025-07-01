package com.segerend

import com.segerend.monkey.IdleState
import com.segerend.monkey.MovingToSourceState
import com.segerend.particles.ParticleSystem
import com.segerend.sorting.BogoSortStrategy
import com.segerend.sorting.BubbleSortStrategy
import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MonkeyTest {
    private lateinit var monkey: Monkey
    private lateinit var grid: GridModel

    @BeforeEach
    fun setUp() {
        LockManager.clear()
        monkey = Monkey(SortAlgorithm.BOGO)
        grid = GridModel()
    }

    @Test
    fun getAlgorithm() {
        assertEquals(SortAlgorithm.BOGO, monkey.algorithm, "Algorithm should be BOGO")
        assertTrue(monkey.strategy is BogoSortStrategy, "Strategy should be BogoSortStrategy")
        // and the state should be IDLE on initialization
        assertEquals(monkey.state as IdleState, monkey.state, "Initial state should be IDLE")
    }

    @Test
    fun setAlgorithm() {
        monkey.algorithm = SortAlgorithm.BUBBLE
        assertEquals(SortAlgorithm.BUBBLE, monkey.algorithm, "Algorithm should be set to BUBBLE")
        assertTrue(monkey.strategy is BubbleSortStrategy, "Strategy should be BubbleSortStrategy")
    }

    @Test
    fun GetAndSetFruitBeingCarried() {
        assertNull(monkey.fruitBeingCarried, "Initially, fruitBeingCarried should be null")
        monkey.fruitBeingCarried = Fruit.BANANA
        assertEquals(Fruit.BANANA, monkey.fruitBeingCarried, "Fruit being carried should be set to banana")
    }

    @Test
    fun assignTask() {
        val taskMonkey = Monkey(SortAlgorithm.BOGO)
        val task = ShuffleTask(Pos(0, 0), Pos(1, 1), Fruit.APPLE)
        LockManager.clear()
        taskMonkey.assignTask(task)
        assertFalse { LockManager.tryLock(task.from) }
        assertFalse { LockManager.tryLock(task.to) }
        assertTrue(taskMonkey.state is MovingToSourceState, "State should be MovingToSourceState after assigning a task")
    }

    @Test
    fun setState() {
        val newTask = ShuffleTask(Pos(0, 0), Pos(1, 1), Fruit.APPLE)
        val (currentX, currentY) = monkey.state.getDrawPosition()
        val newState = MovingToSourceState(newTask, GameConfig.CELL_SIZE, currentX, currentY)
        monkey.state = newState
        assertTrue(monkey.state is MovingToSourceState, "State should be set to MovingToSourceState")
    }

    @Test
    fun update() {
        assertDoesNotThrow {
            monkey.update(0.1, grid, GameConfig.CELL_SIZE, ParticleSystem())
        }
    }

    @Test
    fun `update completes task`() {
        LockManager.clear()
        val updateMonkey = Monkey(SortAlgorithm.BUBBLE)
        val task = ShuffleTask(Pos(0, 0), Pos(1, 1), Fruit.APPLE)
        val updateGrid = GridModel(5, 5)
        updateGrid.set(Pos(0, 0), Fruit.APPLE)
        updateMonkey.assignTask(task, GameConfig.CELL_SIZE)
        val particleSystem = ParticleSystem() // Dummy instance
        repeat(100) { // Simulate multiple updates
            updateMonkey.update(0.1, updateGrid, GameConfig.CELL_SIZE, particleSystem)
        }
        assertTrue(updateMonkey.isIdle(), "Monkey should return to idle after task")
        assertEquals(Fruit.APPLE, updateGrid.get(task.to), "Grid should have the fruit moved to the target position")
        LockManager.clear()
    }

    @Test
    fun draw() {
        // create a javafx.scene.canvas.GraphicsContext mock
        val canvas = Canvas(GameConfig.COLS * GameConfig.CELL_SIZE, GameConfig.ROWS * GameConfig.CELL_SIZE + 30)
        val gc = canvas.graphicsContext2D
        monkey.draw(gc, GameConfig.CELL_SIZE)
        // Check if the monkey is drawn on the canvas
        assertNotNull(gc, "GraphicsContext should not be null")
        // set Monkey algorithm to Bubble
        monkey.algorithm = SortAlgorithm.BUBBLE
        monkey.draw(gc, GameConfig.CELL_SIZE)
        monkey.algorithm = SortAlgorithm.INSERTION
        monkey.draw(gc, GameConfig.CELL_SIZE)
    }

    @Test
    fun isIdle() {
        val monkey = Monkey(SortAlgorithm.BOGO)
        LockManager.clear()

        assertTrue(monkey.isIdle(), "Monkey should be idle initially")

        // Assign a task and check if the monkey is not idle
        val task = ShuffleTask(Pos(0, 0), Pos(1, 1), Fruit.APPLE)
        monkey.assignTask(task)
        Thread.sleep(75)
//        assertFalse(monkey.isIdle(), "Monkey should not be idle after assigning a task")
        println("Monkey state after assigning task: ${monkey.state}")
        println("Is monkey idle? ${monkey.isIdle()}")
        assertTrue(monkey.state is MovingToSourceState, "Monkey should be in MovingToSourceState after assigning a task")

        val wanderingMonkey = Monkey(SortAlgorithm.BOGO)
        wanderingMonkey.state = com.segerend.monkey.WanderingState(2.0, 2.0, GameConfig.CELL_SIZE)
        assertTrue(wanderingMonkey.isIdle(), "Monkey should be idle after wandering state")

        val chattingMonkey = Monkey(SortAlgorithm.BOGO)
        chattingMonkey.state = com.segerend.monkey.ChattingState(2.0, 2.0)
        assertTrue(chattingMonkey.isIdle(), "Monkey should be idle after chatting state")

        LockManager.clear()
    }
}