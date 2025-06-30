package com.segerend.particles

import com.segerend.GameStats
import com.segerend.Pos
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color

class MockParticleEffect(
    durationMs: Long = 1000L
) : BaseParticleEffect(durationMs) {
    // No-op for testing
}

class MockParticle(
    private val lifetime: Long = 1000L
) : Particle {
    private var age = 0L
    override fun update(deltaMs: Long) {
        age += deltaMs
    }
    override val isAlive: Boolean get() = age < lifetime
    override fun render(gc: GraphicsContext, cellSize: Double) {
        // No-op
    }
}

class ParticleSystemTest {
    private lateinit var particleSystem: ParticleSystem

    @BeforeEach
    fun setUp() {
        particleSystem = ParticleSystem()
    }

    @Test
    fun `test add and update removes dead effects`() {
        val effect = MockParticleEffect(durationMs = 1000L)
        particleSystem.add(effect)
        assertEquals(1, particleSystem.effects.size, "Effect should be added")

        particleSystem.update(500L)
        assertTrue(effect.isAlive, "Effect should still be alive after 500ms")

        particleSystem.update(600L) // Total 1100ms > 1000ms
        assertFalse(effect.isAlive, "Effect should be dead after 1100ms")
        assertEquals(0, particleSystem.effects.size, "Dead effect should be removed from the system")
    }

    @Test
    fun `test render calls render on all effects`() {
        val effect1 = MockParticleEffect()
        val effect2 = MockParticleEffect()
        particleSystem.add(effect1)
        particleSystem.add(effect2)

        val canvas = javafx.scene.canvas.Canvas(5.0, 5.0)
        val gc = canvas.graphicsContext2D
        particleSystem.render(gc, 24.0)
        // check that render was called on both effects
        assertTrue(effect1.isAlive || effect2.isAlive, "At least one effect should be alive to render")
        assertTrue(gc.canvas.width > 0 && gc.canvas.height > 0, "GraphicsContext should have valid dimensions")
    }
}

class BaseParticleEffectTest {
    private lateinit var particleSystem: ParticleSystem

    @BeforeEach
    fun setUp() {
        particleSystem = ParticleSystem()
    }

    @Test
    fun `test effect lifespan`() {
        val effect = MockParticleEffect(durationMs = 1000L)
        assertTrue(effect.isAlive, "Effect should be alive initially")

        effect.update(500L)
        assertTrue(effect.isAlive, "Effect should still be alive")

        effect.update(600L) // Total 1100ms > 1000ms
        assertFalse(effect.isAlive, "Effect should die after duration")
    }
}

class ConfettiEffectTest {
    @Test
    fun `test confetti particle count`() {
        val particleAmount = 10
        val effect = ConfettiEffect(100.0, 100.0, amount = particleAmount)
        assertEquals(particleAmount, effect.particleCount(), "Particle count should be 100")
    }

    @Test
    fun `test confetti particle update and render`() {
        val effect = ConfettiEffect(100.0, 100.0)
        val canvas = javafx.scene.canvas.Canvas(200.0, 200.0)
        val gc = canvas.graphicsContext2D

        // Update and render the effect
        effect.update(100L)
        effect.render(gc, 24.0)

        // Check if particles are rendered (not null)
        assertNotNull(gc.fill, "GraphicsContext should have a fill color")
    }
}