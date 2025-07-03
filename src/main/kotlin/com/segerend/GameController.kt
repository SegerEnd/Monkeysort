package com.segerend

import com.segerend.monkey.Monkey
import com.segerend.monkey.ShuffleTask
import com.segerend.particles.ParticleSystem
import com.segerend.sorting.SortAlgorithm

class GameController(rows: Int = GameConfig.ROWS, cols: Int = GameConfig.COLS) {
    var gridModel = GridModel(rows, cols)
    val monkeys = mutableListOf(Monkey(SortAlgorithm.BOGO)) // Start with one monkey
    val particleSystem = ParticleSystem()

    fun tick(frameTime: FrameTime) {
        if (GameStats.timeFactor == 0.0) return

        val delta = frameTime.deltaMs / 1000.0 * GameStats.timeFactor
        val deltaMs = frameTime.deltaMs * GameStats.timeFactor

        for (monkey in monkeys) {
            if (monkey.isIdle()) {
                val task = monkey.strategy.getNextTask(gridModel)
                if (task != null) monkey.assignTask(task as ShuffleTask, GameConfig.CELL_SIZE)
            }
            monkey.update(delta, gridModel, GameConfig.CELL_SIZE, particleSystem)
        }
        particleSystem.update(deltaMs.toLong())
    }

    fun getNewMonkeyPrice(): Int {
        return (GameConfig.MONKEY_BASE_COST * monkeys.size * GameConfig.MONKEY_COST_INCREASE_FACTOR).toInt()
    }

    fun buyMonkey(): Boolean {
        val cost = getNewMonkeyPrice()
        if (GameStats.coins >= cost) {
            GameStats.coins -= cost
            monkeys.add(Monkey(SortAlgorithm.BOGO))
            return true
        }
        return false
    }

    fun upgradeMonkey(): Boolean {
        if (GameStats.coins >= GameConfig.MONKEY_UPGRADE_COST) {
            monkeys.firstOrNull { it.algorithm == SortAlgorithm.BOGO }?.let {
                it.algorithm = SortAlgorithm.BUBBLE
                GameStats.coins -= GameConfig.MONKEY_UPGRADE_COST
                return true
            }
        }
        return false
    }
}