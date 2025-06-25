package com.segerend

import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.random.Random

// Base particle effect
abstract class ParticleEffect(private val durationMs: Long) {
    private var elapsedMs = 0L
    var isAlive = true
        private set

    fun update(deltaMs: Long) {
        if (!isAlive) return
        elapsedMs += deltaMs
        isAlive = elapsedMs < durationMs
        if (isAlive) onUpdate(deltaMs)
    }

    protected abstract fun onUpdate(deltaMs: Long)
    abstract fun render(gc: GraphicsContext, cellSize: Double)
}

// Particle manager
class ParticleSystem {
    private val effects = mutableListOf<ParticleEffect>()

    fun add(effect: ParticleEffect) {
        effects += effect
    }

    fun update(deltaMs: Long) {
        effects.removeAll {
            it.update(deltaMs)
            !it.isAlive
        }
    }

    fun render(gc: GraphicsContext, cellSize: Double) {
        effects.forEach { it.render(gc, cellSize) }
    }
}

// Encapsulated particle behavior
class Particle(
    private var x: Double,
    private var y: Double,
    private val dx: Double,
    private val dy: Double,
    private val size: Double = 6.0,
    private val lifetime: Long = 1000L,
    private val baseColor: Color = Color.rgb(255, 200, 50)
) {
    private var age = 0L
    private var alpha = 1.0

    fun update(deltaMs: Long) {
        age += deltaMs
        if (age >= lifetime) {
            alpha = 0.0
        } else {
            x += dx
            y += dy
            alpha = 1.0 - age.toDouble() / lifetime
        }
    }

    fun render(gc: GraphicsContext) {
        if (alpha <= 0.0) return
        gc.fill = baseColor.deriveColor(0.0, 1.0, 1.0, alpha)
        gc.fillOval(x, y, size, size)
    }

    val isAlive: Boolean
        get() = alpha > 0.0
}

class ComboParticleEffect(
    comboCells: List<Pos>,
    cellSize: Double,
    durationMs: Long = 1000L
) : ParticleEffect(durationMs) {

    private val particles = mutableListOf<Particle>()

    // Determine color based on combo size
    private val baseColor: Color = when {
        comboCells.size <= 3 -> Color.ORANGE
        comboCells.size <= 5 -> Color.DEEPSKYBLUE
        else -> Color.HOTPINK
    }

    init {
        comboCells.forEach { cell ->
            val baseX = cell.col * cellSize + cellSize / 2
            val baseY = cell.row * cellSize + cellSize / 2
            repeat(10) {
                particles += createRandomParticle(baseX, baseY, durationMs, baseColor)
            }
        }
    }

    private fun createRandomParticle(x: Double, y: Double, lifetime: Long, color: Color): Particle {
        val angle = Random.nextDouble(0.0, 2 * PI)
        val speed = Random.nextDouble(0.5, 2.0)
        return Particle(
            x = x,
            y = y,
            dx = cos(angle) * speed,
            dy = sin(angle) * speed - 0.5,
            size = Random.nextDouble(5.0, 12.0),
            lifetime = lifetime,
            baseColor = color
        )
    }

    override fun onUpdate(deltaMs: Long) {
        particles.forEach { it.update(deltaMs) }
        particles.removeAll { !it.isAlive }
    }

    override fun render(gc: GraphicsContext, cellSize: Double) {
        particles.forEach { it.render(gc) }
    }
}

class CompletionParticleEffect(
    private val centerX: Double,
    private val centerY: Double,
    durationMs: Long = 2000L
) : ParticleEffect(durationMs) {

    private val particles = mutableListOf<Particle>()

    init {
        repeat(100) {
            particles += createRandomParticle(centerX, centerY, durationMs)
        }
    }

    private fun createRandomParticle(x: Double, y: Double, lifetime: Long): Particle {
        val angle = Random.nextDouble(0.0, 2 * PI)
        val speed = Random.nextDouble(1.0, 3.0)
        return Particle(
            x = x,
            y = y,
            dx = cos(angle) * speed,
            dy = sin(angle) * speed - 1.0,
            size = Random.nextDouble(5.0, 15.0),
            lifetime = lifetime,
            baseColor = Color.rgb(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
        )
    }

    override fun onUpdate(deltaMs: Long) {
        particles.forEach { it.update(deltaMs) }
        particles.removeAll { !it.isAlive }
    }

    override fun render(gc: GraphicsContext, cellSize: Double) {
        particles.forEach { it.render(gc) }
    }
}
