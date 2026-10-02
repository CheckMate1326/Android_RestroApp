package com.restaurant.mobile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import java.util.UUID
import org.junit.Rule
import org.junit.Test

/** Run against scripts/verify_delivery.py --keep-running (temporary catalog/database). */
class DeliveryFlowTest {
    @get:Rule val app = createAndroidComposeRule<MainActivity>()

    private fun awaitText(text: String) {
        app.waitUntil(30000) { app.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun click(text: String) = app.onNodeWithText(text).performScrollTo().performClick()

    @Test
    fun registerCustomizeCheckoutAndRestoreSession() {
        awaitText("New to Restro? Create an account")
        click("New to Restro? Create an account")
        app.onNodeWithText("Your name").performTextInput("Device verification")
        app.onNodeWithText("Email").performTextInput("device-${UUID.randomUUID()}@example.com")
        app.onNodeWithText("Password · at least 10 characters")
            .performTextInput(UUID.randomUUID().toString())
        click("Create account")
        awaitText("What sounds\ngood today?")
        app.onNodeWithText("Profile").performClick()
        click("Addresses")
        click("Add address")
        app.onNodeWithText("Flat, building and street").performTextInput("12 Verification Street")
        app.onNodeWithText("City").performTextInput("Bengaluru")
        app.onNodeWithText("Six-digit PIN code").performTextInput("560001")
        app.onNodeWithText("Contact phone").performTextInput("9000000000")
        click("Save address")
        awaitText("Your addresses")
        app.onAllNodesWithText("Home").onLast().performClick()
        awaitText("What sounds\ngood today?")
        app.onNodeWithTag("discovery-list").performScrollToNode(hasText("Paneer tikka"))
        app.onNodeWithText("Paneer tikka").performClick()
        awaitText("Make it yours")
        app.onNodeWithContentDescription("Increase quantity").performClick()
        app.onNode(hasText("Add to cart", substring = true)).performScrollTo().performClick()
        awaitText("Your bag")
        click("Review checkout")
        awaitText("The last little details")
        app.onAllNodesWithText("Home").onFirst().performClick()
        click("Continue to payment")
        awaitText("Payment")
        app.onNode(hasText("Place order", substring = true)).performScrollTo().performClick()
        awaitText("Order confirmed.")
        click("Track order")
        awaitText("From kitchen\nto your door.")
        app.activityRule.scenario.recreate()
        awaitText("From kitchen\nto your door.")
        app.onNodeWithText("Orders").performClick()
        awaitText("Your orders")
        app.onNodeWithText("Bistro Lumière").assertExists()
    }
}
