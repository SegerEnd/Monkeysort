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

    fun getUpgradeAllFee(startFee: Int, algorithm: SortAlgorithm) : Int {
        var algorithmMonkeys = monkeys.count { it.algorithm == algorithm }
        var otherMonkeys = monkeys.size - algorithmMonkeys
        return (startFee * (1 + 0.25 * otherMonkeys + 0.1 * algorithmMonkeys)).toInt()
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

    fun upgradeAllMonkeysToBubbleSort() {
        val upgradeFee = getUpgradeAllFee(
            GameConfig.BUBBLE_SORT_ALL_START_FEE,
            SortAlgorithm.BUBBLE
        )
        if (GameStats.coins < upgradeFee) return
        GameStats.coins -= upgradeFee
        monkeys.forEach { it.algorithm = SortAlgorithm.BUBBLE }
    }
}