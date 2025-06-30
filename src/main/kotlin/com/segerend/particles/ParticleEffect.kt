package com.segerend.particles

import com.segerend.GameStats
import com.segerend.Pos

import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import kotlin.math.*
import kotlin.random.Random

interface Updatable {
    fun update(deltaMs: Long)
    val isAlive: Boolean
}

interface Renderable {
    fun render(gc: GraphicsContext, cellSize: Double)
}

interface ParticleEffect : Updatable, Renderable
interface Particle : Updatable, Renderable

class ParticleSystem {
    private val _effects = mutableListOf<ParticleEffect>()

    val effects: List<ParticleEffect>
        get() = _effects.toList() // Return an immutable copy

    fun add(effect: ParticleEffect) {
        _effects += effect
    }

    fun update(deltaMs: Long) {
        _effects.removeIf {
            it.update(deltaMs); !it.isAlive
        }
    }

    fun render(gc: GraphicsContext, cellSize: Double) {
        _effects.forEach { it.render(gc, cellSize) }
    }
}

class SimpleParticle(
    private var x: Double,
    private var y: Double,
    private val dx: Double,
    private val dy: Double,
    private val size: Double,
    private val lifetime: Long,
    private val baseColor: Color
) : Particle {

    private var age = 0L
    private var alpha = 1.0

    override fun update(deltaMs: Long) {
        age += deltaMs
        if (age >= lifetime) {
            alpha = 0.0
        } else {
            x += dx
            y += dy
            val progress = age.toDouble() / lifetime
            alpha = 1.0 - progress * progress // ease-out
        }
    }

    override fun render(gc: GraphicsContext, cellSize: Double) {
        if (alpha <= 0.0) return
        gc.fill = baseColor.deriveColor(0.0, 1.0, 1.0, alpha)
        gc.fillOval(x, y, size, size)
    }

    override val isAlive: Boolean get() = alpha > 0.0
}

abstract class BaseParticleEffect(
    val durationMs: Long
) : ParticleEffect {

    private var elapsedMs = 0L
    protected val particles = mutableListOf<Particle>()

    fun particleCount(): Int = particles.size

    override fun update(deltaMs: Long) {
        elapsedMs += deltaMs

        // Only update existing particles
        particles.forEach { it.update(deltaMs) }
        particles.removeIf { !it.isAlive }
    }

    override val isAlive: Boolean
        get() = elapsedMs < durationMs || particles.any { it.isAlive }

    override fun render(gc: GraphicsContext, cellSize: Double) {
        particles.forEach { it.render(gc, cellSize) }
    }
}

class ComboParticleEffect(
    comboCells: List<Pos>,
    cellSize: Double,
    durationMs: Long = 1000L
) : BaseParticleEffect(durationMs) {

     val baseColor: Color = when (comboCells.size) {
        in 0..3 -> Color.ORANGE
        in 4..5 -> Color.DEEPSKYBLUE
        in 6..7 -> Color.LIMEGREEN
        in 8..14 -> Color.HOTPINK
        else -> Color.FUCHSIA
    }

    init {
        comboCells.forEach { cell ->
            val baseX = cell.col * cellSize + cellSize / 2
            val baseY = cell.row * cellSize + cellSize / 2

            // Scale particle count inversely with game speed (higher speed -> fewer particles)
            // and directly with combo size (larger combo -> more particles)
            val speedFactor = (15.0 / GameStats.timeFactor).coerceAtMost(15.0)
            val comboFactor = comboCells.size / 10.0
            val rawCount = speedFactor * comboFactor

            // Clamp the particle count between 2 and 12
            val particleCount = rawCount.toInt().coerceIn(2, 12)

            repeat(particleCount) {
                particles += createParticle(baseX, baseY)
            }
        }
    }

    private fun createParticle(x: Double, y: Double): Particle {
        val angle = Random.nextDouble(0.0, 2 * PI)
        val speed = Random.nextDouble(0.5, 2.0)
        val size = Random.nextDouble(4.0, 10.0)
        val color = baseColor.deriveColor(0.0, 1.0, 1.0, 0.7)

        return SimpleParticle(
            x = x,
            y = y,
            dx = cos(angle) * speed,
            dy = sin(angle) * speed - 0.5,
            size = size,
            lifetime = 1000L,
            baseColor = color
        )
    }
}

class ConfettiEffect(
    private val centerX: Double,
    private val centerY: Double,
    durationMs: Long = 1000L,
    amount: Int = 100
) : BaseParticleEffect(durationMs) {

    init {
        repeat(amount) {
            particles += createParticle()
        }
    }

    private fun createParticle(): Particle {
        val angle = Random.nextDouble(0.0, 2 * PI)
        val speed = Random.nextDouble(1.0, 3.0)
        val size = Random.nextDouble(3.0, 9.0)
        val color = Color.hsb(Random.nextDouble(0.0, 360.0), 1.0, 1.0)

        return SimpleParticle(
            x = centerX,
            y = centerY,
            dx = cos(angle) * speed,
            dy = sin(angle) * speed - 1.0,
            size = size,
            lifetime = durationMs,
            baseColor = color
        )
    }
}