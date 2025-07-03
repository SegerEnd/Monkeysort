package com.segerend

object LockManager {
    private val lockedPositions = mutableSetOf<Pos>()

    @Synchronized
    fun tryLock(vararg positions: Pos): Boolean {
        if (positions.any { it in lockedPositions }) return false
        lockedPositions.addAll(positions)
        return true
    }

    @Synchronized
    fun unlock(vararg positions: Pos) { lockedPositions.removeAll(positions.toSet()) }

    fun clear() {
        lockedPositions.clear()
    }
}