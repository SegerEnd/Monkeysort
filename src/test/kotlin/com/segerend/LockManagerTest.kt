package com.segerend

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LockManagerTest {
    @Test
    fun `tryLock and unlock`() {
        LockManager.clear()
        val pos = Pos(0, 0)
        assertTrue(LockManager.tryLock(pos), "First lock should succeed")
        assertFalse(LockManager.tryLock(pos), "Second lock should reject")
        LockManager.unlock(pos)
        assertTrue(LockManager.tryLock(pos), "Lock should succeed after unlock")
        LockManager.clear()
    }
}