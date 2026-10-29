package com.example.runway.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// SCRUM-163: which pushes the user sees, and where.
class NotificationRulesTest {

    private val allOn = RunwaySettings(notifyWash = true, notifySwap = true, notifyWeather = true)
    private val allOff = RunwaySettings(notifyWash = false, notifySwap = false, notifyWeather = false)

    @Test fun everyTypeIsShownWhenEverythingIsOn() {
        NotificationType.entries.forEach { type ->
            assertEquals(type.name, DeliveryDecision.SHOW, NotificationRules.decide(type, allOn, true))
        }
    }

    @Test fun washRemindersCanBeTurnedOffOnTheirOwn() {
        val settings = allOn.copy(notifyWash = false)
        assertEquals(DeliveryDecision.TURNED_OFF, NotificationRules.decide(NotificationType.WASH, settings, true))
        assertEquals(DeliveryDecision.SHOW, NotificationRules.decide(NotificationType.SWAP, settings, true))
        assertEquals(DeliveryDecision.SHOW, NotificationRules.decide(NotificationType.WEATHER, settings, true))
    }

    @Test fun swapRequestsFollowTheirSwitch() {
        assertFalse(NotificationRules.isEnabled(NotificationType.SWAP, allOn.copy(notifySwap = false)))
        assertTrue(NotificationRules.isEnabled(NotificationType.SWAP, allOn))
    }

    @Test fun weatherSuggestionsFollowTheirSwitch() {
        assertFalse(NotificationRules.isEnabled(NotificationType.WEATHER, allOn.copy(notifyWeather = false)))
    }

    @Test fun generalMessagesCannotBeTurnedOff() {
        assertEquals(DeliveryDecision.SHOW, NotificationRules.decide(NotificationType.GENERAL, allOff, true))
    }

    @Test fun blockedNotificationsStillReachTheInAppList() {
        assertEquals(DeliveryDecision.IN_APP_ONLY, NotificationRules.decide(NotificationType.WASH, allOn, false))
    }

    @Test fun anAlertTheUserTurnedOffIsDroppedEvenWhenBlocked() {
        // TURNED_OFF wins over IN_APP_ONLY: the user said they do not want these at all.
        assertEquals(DeliveryDecision.TURNED_OFF, NotificationRules.decide(NotificationType.SWAP, allOff, false))
    }

    @Test fun wireNamesMatchTheApi() {
        assertEquals(NotificationType.WASH, NotificationType.fromWire("wash"))
        assertEquals(NotificationType.SWAP, NotificationType.fromWire("swap"))
        assertEquals(NotificationType.WEATHER, NotificationType.fromWire("weather"))
        assertEquals(NotificationType.GENERAL, NotificationType.fromWire("general"))
    }

    @Test fun anUnknownOrMissingTypeArrivesAsGeneral() {
        assertEquals(NotificationType.GENERAL, NotificationType.fromWire("laundry-v2"))
        assertEquals(NotificationType.GENERAL, NotificationType.fromWire(null))
    }

    @Test fun eachTypeHasItsOwnChannel() {
        val channels = NotificationType.entries.map { it.channelId }
        assertEquals(channels.size, channels.toSet().size)
        // The manifest names "general" as FCM's default channel, so it must exist.
        assertTrue("general" in channels)
    }
}
