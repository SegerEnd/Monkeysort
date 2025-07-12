package com.segerend

import com.segerend.ui.SortChartWindow
import javafx.application.Platform
import javafx.scene.control.Button
import javafx.stage.Stage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.testfx.framework.junit5.ApplicationTest
import org.testfx.util.WaitForAsyncUtils

class SortChartWindowTest : ApplicationTest() {
    private lateinit var monkeySortSimulatorApp: MonkeySortSimulatorApp
    private lateinit var stage: Stage

    override fun start(stage: Stage) {
        monkeySortSimulatorApp = MonkeySortSimulatorApp()
        monkeySortSimulatorApp.start(stage)
        stage.toFront()
        this.stage = stage
    }

    // test without needing accessibility permissions for clickon or fire
    @Test
    fun openSortChartWindowWithoutAccessibility() {
        lateinit var newSortChartWindow : SortChartWindow
        LockManager.clear()

        Platform.runLater {
            newSortChartWindow = SortChartWindow(monkeySortSimulatorApp.controller)
            assertNotNull(newSortChartWindow, "SortChartWindow should not be null")
            assertNotNull(newSortChartWindow.root, "SortChartWindow should not be null")
        }
        WaitForAsyncUtils.waitForFxEvents()
        assertDoesNotThrow({
            Platform.runLater {
                newSortChartWindow.stage.show()
                newSortChartWindow.stage.toFront()
                newSortChartWindow.stage.requestFocus()
            }
            WaitForAsyncUtils.waitForFxEvents()
        }, "SortChartWindow should be able to show without accessibility permissions")
        // clean up
        Platform.runLater {
            newSortChartWindow.stage.close()
        }
        WaitForAsyncUtils.waitForFxEvents()
        LockManager.clear()
    }

    @Test
    fun openSortChartWindow() {
        assumeTrue(AccessibilityUtil.isAccessibilityEnabled, "Accessibility permissions not enabled");

        Thread.sleep(1000)

        Platform.runLater {
            stage.toFront()
            stage.requestFocus()
        }
        WaitForAsyncUtils.waitForFxEvents()
        Thread.sleep(100)

        val chartButton = lookup("#chartButton").queryButton()
        assertNotNull(chartButton, "Chart button should not be null")
        assertTrue(chartButton.isVisible, "Chart button should be visible")
        assertFalse(chartButton.isDisabled, "Chart button should not be disabled")

        WaitForAsyncUtils.waitForFxEvents()

        clickOn(chartButton) // clickOn is needed otherwise the chart window does not open with .fire()

        WaitForAsyncUtils.waitForFxEvents()

        Thread.sleep(100)

        WaitForAsyncUtils.waitForFxEvents()

        // check if the SortChartWindow is open
        val sortChartWindow = SortChartWindow.getInstance()
        assertTrue(sortChartWindow!!.stage.isShowing, "SortChartWindow should be showing")
        assertTrue(sortChartWindow.stage.isFocused, "SortChartWindow should be focused")
        // cget barchart from the SortChartWindow root center
        val barChart = sortChartWindow.root.center as? javafx.scene.chart.BarChart<*, *>
        assertNotNull(barChart, "BarChart should not be null")
        assertTrue(barChart!!.data.isNotEmpty(), "BarChart should have data")
        assertTrue(barChart.data[0].data.isNotEmpty(), "BarChart should have data in the first series")
        WaitForAsyncUtils.waitForFxEvents()

        // click the #toggleAnimButton
        Thread.sleep(300)
        WaitForAsyncUtils.waitForFxEvents()
        val toggleAnimButton = sortChartWindow.root.lookup("#toggleAnimButton") as? Button

        assertNotNull(toggleAnimButton, "Toggle Animations button should not be null")
        assertTrue(toggleAnimButton!!.isVisible, "Toggle Animations button should be visible")
        assertFalse(toggleAnimButton.isDisabled, "Toggle Animations button should not be disabled")

        assertFalse(barChart.animated, "BarChart should not be animated initially")
        clickOn(toggleAnimButton)
        Thread.sleep(50)
        WaitForAsyncUtils.waitForFxEvents()
        assertTrue(barChart.animated, "BarChart should be animated after clicking the toggle button")

        // check if it toggles back to false
        clickOn(toggleAnimButton)
        Thread.sleep(50)
        WaitForAsyncUtils.waitForFxEvents()
        assertFalse(barChart.animated, "BarChart should not be animated after toggling back")

        Thread.sleep(250)

        Platform.runLater {
            stage.toFront()
            stage.requestFocus()
        }
        WaitForAsyncUtils.waitForFxEvents()

        // close the SortChartWindow
        // press the open chart button again, it must not open a new window or close the existing one
        clickOn(chartButton)
        WaitForAsyncUtils.waitForFxEvents()
        assertTrue(sortChartWindow.stage.isShowing, "SortChartWindow should still be showing after clicking the button again")
        // check if the SortChartWindow is still focused
        assertTrue(sortChartWindow.stage.isFocused, "SortChartWindow should still be focused after clicking the button again")
    }
}