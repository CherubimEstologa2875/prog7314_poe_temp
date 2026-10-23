package com.example.runway.data.mapper

import com.example.runway.data.remote.api.SettingsDto
import com.example.runway.domain.model.RunwaySettings
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsMappersTest {
    @Test
    fun `server wear limit replaces the cached value`() {
        val updated = SettingsDto(defaultWearLimit = 7)
            .applyTo(RunwaySettings(defaultWearLimit = 3))
        assertEquals(7, updated.defaultWearLimit)
    }

    @Test
    fun `edited wear limit is sent back to the API`() {
        val request = RunwaySettings(defaultWearLimit = 8).toDto("METRIC")
        assertEquals(8, request.defaultWearLimit)
    }
}
