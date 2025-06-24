import kotlin.system.exitProcess
import kotlin.random.Random

// Interfaces & abstract classes

interface SortAlgorithm {
    fun <T : Comparable<T>> sort(list: MutableList<T>)
}

abstract class Card(val name: String, val emoji: String) : Comparable<Card> {
    abstract fun playEffect(game: Game, owner: Player)

    // Sorteer op name (kan uitgebreid worden met rank, suit etc.)
    override fun compareTo(other: Card): Int = name.compareTo(other.name)

    override fun toString() = "$emoji $name"
}

class NormalCard(
    val suit: String,
    val rank: String,
    emoji: String
) : Card("$rank of $suit", emoji) {
    override fun playEffect(game: Game, owner: Player) {
        // Normale kaarten hebben geen speciaal effect
        println("${owner.name} speelt $this")
    }
}

class JokerCard(
    name: String,
    emoji: String,
    val effectDescription: String,
    val effect: (game: Game, owner: Player) -> Unit
) : Card(name, emoji) {
    override fun playEffect(game: Game, owner: Player) {
        println("${owner.name} speelt Joker $this: $effectDescription")
        effect(game, owner)
    }
}

// Sorteeralgoritmes

class BubbleSort : SortAlgorithm {
    override fun <T : Comparable<T>> sort(list: MutableList<T>) {
        val n = list.size
        for (i in 0 until n - 1) {
            for (j in 0 until n - i - 1) {
                if (list[j] > list[j + 1]) {
                    val tmp = list[j]
                    list[j] = list[j + 1]
                    list[j + 1] = tmp
                    printList(list)
                }
            }
        }
    }
}

class MergeSort : SortAlgorithm {
    override fun <T : Comparable<T>> sort(list: MutableList<T>) {
        mergeSort(list, 0, list.size - 1)
    }

    private fun <T : Comparable<T>> mergeSort(list: MutableList<T>, left: Int, right: Int) {
        if (left < right) {
            val mid = (left + right) / 2
            mergeSort(list, left, mid)
            mergeSort(list, mid + 1, right)
            merge(list, left, mid, right)
            printList(list)
        }
    }

    private fun <T : Comparable<T>> merge(list: MutableList<T>, left: Int, mid: Int, right: Int) {
        val leftList = list.subList(left, mid + 1).toMutableList()
        val rightList = list.subList(mid + 1, right + 1).toMutableList()
        var i = 0
        var j = 0
        var k = left
        while (i < leftList.size && j < rightList.size) {
            if (leftList[i] <= rightList[j]) {
                list[k] = leftList[i]
                i++
            } else {
                list[k] = rightList[j]
                j++
            }
            k++
        }
        while (i < leftList.size) {
            list[k++] = leftList[i++]
        }
        while (j < rightList.size) {
            list[k++] = rightList[j++]
        }
    }
}

// Helper functie om lijst te printen met emoji kaarten
fun <T> printList(list: List<T>) {
    print("Sort stappen: ")
    list.forEach { print("$it | ") }
    println()
}

// Spelers

class Player(val name: String) {
    val hand = mutableListOf<Card>()

    fun draw(deck: Deck) {
        if (deck.cards.isEmpty()) {
            println("Deck is leeg!")
            return
        }
        val drawn = deck.cards.removeAt(0)
        hand.add(drawn)
        println("$name pakt kaart: $drawn")
    }

    fun playCard(index: Int, game: Game) {
        if (index !in hand.indices) {
            println("Ongeldige kaartindex!")
            return
        }
        val card = hand.removeAt(index)
        card.playEffect(game, this)
    }

    fun showHand() {
        println("$name's hand:")
        hand.forEachIndexed { i, card -> println("  [$i] $card") }
    }
}

// Deck

class Deck {
    val cards = mutableListOf<Card>()

    fun createDeck() {
        val suits = mapOf(
            "Hearts" to "♥️",
            "Diamonds" to "♦️",
            "Clubs" to "♣️",
            "Spades" to "♠️"
        )
        val ranks = listOf("2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A")
        val emojis = mapOf(
            "2" to "2️⃣", "3" to "3️⃣", "4" to "4️⃣", "5" to "5️⃣", "6" to "6️⃣",
            "7" to "7️⃣", "8" to "8️⃣", "9" to "9️⃣", "10" to "🔟",
            "J" to "👑", "Q" to "👸", "K" to "🤴", "A" to "🅰️"
        )

        // Normale kaarten toevoegen
        for ((suit, suitEmoji) in suits) {
            for (rank in ranks) {
                cards.add(NormalCard(suit, rank, emojis[rank] + suitEmoji))
            }
        }

        // Voeg 100 Jokers toe (voorbeeld)
        repeat(100) { i ->
            cards.add(
                JokerCard(
                    name = "Joker #$i",
                    emoji = when (i % 3) {
                        0 -> "🃏"
                        1 -> "🤡"
                        else -> "🎭"
                    },
                    effectDescription = "Wis één kaart van tegenstander",
                    effect = { game, owner ->
                        val opponent = game.getOpponent(owner)
                        if (opponent.hand.isNotEmpty()) {
                            val removed = opponent.hand.removeAt(Random.nextInt(opponent.hand.size))
                            println("${opponent.name} verliest kaart $removed door effect van ${owner.name}'s Joker.")
                        } else {
                            println("${opponent.name} heeft geen kaarten om te verliezen.")
                        }
                    }
                )
            )
        }
    }

    fun shuffle() {
        cards.shuffle()
    }

    fun sortDeck() {
        // Gebruik 2 verschillende sorteer algoritmes op de helft van het deck

        val mid = cards.size / 2
        val left = cards.subList(0, mid)
        val right = cards.subList(mid, cards.size)

        println("Sorteren eerste helft met MergeSort:")
        MergeSort().sort(left)

        println("\nSorteren tweede helft met BubbleSort:")
        BubbleSort().sort(right)

        println("\nDeck na sorteren:")
        printList(cards)
    }
}

// Game logic

class Game {
    val deck = Deck()
    val player = Player("Speler")
    val computer = Player("Computer")

    private var currentPlayer = player

    fun start() {
        deck.createDeck()
        deck.shuffle()
        println("Deck geschud!")

        // Uitdelen: 5 kaarten per speler
        repeat(5) {
            player.draw(deck)
            computer.draw(deck)
        }

        deck.sortDeck()

        gameLoop()
    }

    fun gameLoop() {
        while (true) {
            println("\n--- Beurt van ${currentPlayer.name} ---")
            if (currentPlayer == player) {
                player.showHand()
                println("Kies een kaart om te spelen door index, of typ 'draw' om een nieuwe kaart te pakken:")
                val input = readLine()?.trim()
                when {
                    input == null -> {
                        println("Ongeldige invoer!")
                        continue
                    }
                    input.equals("draw", ignoreCase = true) -> {
                        currentPlayer.draw(deck)
                    }
                    input.toIntOrNull() != null -> {
                        val idx = input.toInt()
                        if (idx in currentPlayer.hand.indices) {
                            currentPlayer.playCard(idx, this)
                        } else {
                            println("Ongeldige kaartindex!")
                            continue
                        }
                    }
                    else -> {
                        println("Ongeldige invoer!")
                        continue
                    }
                }
            } else {
                // Simpele AI: speel random kaart of pak kaart als hand < 3
                if (currentPlayer.hand.size < 3 && deck.cards.isNotEmpty()) {
                    println("${currentPlayer.name} pakt een kaart.")
                    currentPlayer.draw(deck)
                } else if (currentPlayer.hand.isNotEmpty()) {
                    val idx = Random.nextInt(currentPlayer.hand.size)
                    println("${currentPlayer.name} speelt kaart index $idx.")
                    currentPlayer.playCard(idx, this)
                } else {
                    println("${currentPlayer.name} heeft geen kaarten om te spelen.")
                }
            }

            // Check of iemand gewonnen is (lege hand)
            if (currentPlayer.hand.isEmpty()) {
                println("\n*** ${currentPlayer.name} heeft gewonnen! ***")
                exitProcess(0)
            }

            switchPlayer()
        }
    }

    fun switchPlayer() {
        currentPlayer = if (currentPlayer == player) computer else player
    }

    fun getOpponent(player: Player): Player = if (player == this.player) computer else this.player
}

fun main() {
    val game = Game()
    game.start()
}
