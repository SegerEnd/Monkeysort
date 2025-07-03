package com.segerend

import javafx.scene.paint.Color

enum class Fruit(val emoji: String, val color: Color) {
    APPLE("🍎", Color.RED),
    BANANA("🍌", Color.YELLOW),
    GRAPE("🍇", Color.PURPLE),
    ORANGE("🍊", Color.ORANGE),
    WATERMELON("🍉", Color.GREEN),
    PINEAPPLE("🍍", Color.GOLD),
    STRAWBERRY("🍓", Color.CRIMSON),
    CHERRY("🍒", Color.DARKRED),
    KIWI("🥝", Color.OLIVEDRAB),
    PEACH("🍑", Color.PEACHPUFF),
    MANGO("🥭", Color.DARKORANGE),
    BLUEBERRY("🫐", Color.BLUE),
    LEMON("🍋", Color.LEMONCHIFFON),
    LETTUCE("🥬", Color.FORESTGREEN),
    EMPTY(" ", Color.TRANSPARENT);

    companion object {
        fun random(): Fruit = values().filter { it != EMPTY }.random()
    }
}