package com.example.runway.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UserProfileTest {

    @Test
    fun `first name is the first word of the display name`() {
        assertEquals("Divan", firstNameOf("Divan Fourie", "x@example.com"))
        assertEquals("Divan", firstNameOf("  Divan   Fourie ", null))
    }

    @Test
    fun `first name falls back to the email, then to nothing`() {
        assertEquals("divan", firstNameOf("", "divan@example.com"))
        assertEquals("divan", firstNameOf(null, "divan@example.com"))
        assertEquals("", firstNameOf(null, null))
    }

    @Test
    fun `initials use at most two words`() {
        assertEquals("DF", initialsOf("Divan Fourie", null))
        assertEquals("DJ", initialsOf("divan jacobus fourie", null))
        assertEquals("D", initialsOf("Divan", null))
    }

    @Test
    fun `initials fall back to the email`() {
        assertEquals("D", initialsOf("", "divan@example.com"))
        assertEquals("", initialsOf(null, null))
    }
}
