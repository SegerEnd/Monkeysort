package com.segerend.ui

import com.segerend.*
import com.segerend.sorting.SortAlgorithm
import javafx.scene.control.Button
import javafx.scene.layout.FlowPane

object UIFactory {
    lateinit var buyButton: Button
    lateinit var upgradeBubbleButton: Button
    lateinit var upgradeInsertionButton: Button

    fun createButtonPanel(controller: GameController): FlowPane {
        buyButton = Button()
        upgradeBubbleButton = Button()
        upgradeInsertionButton = Button()
        val pauseButton = Button("Pause")

        return FlowPane().apply {
            children.addAll(
                buyButton.apply {
                    id = "buyButton"
                    text = "${GameConfig.DEFAULT_MONKEY} Buy Monkey (${controller.getNewMonkeyPrice()} coins)"
                    setOnAction {
                        controller.buyMonkey()
                    }
                },
                upgradeBubbleButton.apply {
                    id = "upgradeBubbleButton"
                    text = "🫧 Upgrade All BubbleSort"
                    setOnAction {
                        controller.upgradeAllMonkeysToBubbleSort()
                    }
                },
                upgradeInsertionButton.apply {
                    id = "upgradeInsertionButton"
                    text = "Upgrade All InsertionSort"
                    setOnAction {
                        controller.upgradeAllMonkeysToInsertionSort()
                    }
                },
                button("🗺️ Show Sort Chart", id = "chartButton") {
                    SortChartWindow.show(controller)
                },
                button("Speed x5", id = "speedx5Button") {
                    GameStats.timeFactor = if (GameStats.timeFactor == 1.0) 5.0 else 1.0
                },
                button("Speed x100", id = "speedx100Button") {
                    GameStats.timeFactor = if (GameStats.timeFactor == 1.0) 100.0 else 1.0
                },
                pauseButton.apply {
                    id = "pauseButton"
                    setOnAction {
                        if (GameStats.timeFactor == 0.0) {
                            text = "Pause"
                            GameStats.timeFactor = 1.0
                        } else {
                            text = "Resume"
                            GameStats.timeFactor = 0.0
                        }
                    }
                }
            )
        }
    }

    fun updateButtons(controller: GameController) {
        buyButton.text = "${GameConfig.DEFAULT_MONKEY} Buy Monkey (${controller.getNewMonkeyPrice().formatWithDots()} coins)"
        buyButton.isDisable = GameStats.coins < controller.getNewMonkeyPrice() || controller.monkeys.size >= GameConfig.MAX_MONKEYS

        var upgradeAllFee = controller.getUpgradeAllFee(
            GameConfig.BUBBLE_SORT_ALL_START_FEE,
            SortAlgorithm.BUBBLE
        )
        upgradeBubbleButton.text = "🫧 Upgrade All BubbleSort (${upgradeAllFee.formatWithDots()})"
        upgradeBubbleButton.isDisable = GameStats.coins < upgradeAllFee || controller.monkeys.count { it.algorithm != SortAlgorithm.BUBBLE } == 0

        upgradeAllFee = controller.getUpgradeAllFee(
            GameConfig.INSERTION_SORT_ALL_START_FEE,
            SortAlgorithm.INSERTION
        )
        upgradeInsertionButton.text = "Upgrade All InsertionSort (${upgradeAllFee.formatWithDots()})"
        upgradeInsertionButton.isDisable = GameStats.coins < upgradeAllFee || controller.monkeys.count { it.algorithm != SortAlgorithm.INSERTION } == 0
    }

    private fun button(text: String, id: String? = null, onClick: (Button) -> Unit): Button {
        return Button(text).apply {
            if (id != null) this.id = id
            setOnAction { onClick(this) }
        }
    }
}
