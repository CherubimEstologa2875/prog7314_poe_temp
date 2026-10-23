package com.example.runway.domain.model

data class UserProfile(
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    // ISO-8601 instant from the API, e.g. 2026-09-01T10:15:30Z.
    val createdAt: String? = null,
)

// "Divan Fourie" -> "Divan". Falls back to the start of the email address.
fun firstNameOf(displayName: String?, email: String?): String {
    val fromName = displayName?.trim()?.split(Regex("\\s+"))?.firstOrNull().orEmpty()
    if (fromName.isNotEmpty()) return fromName
    return email?.substringBefore('@')?.trim().orEmpty()
}

// "Divan Fourie" -> "DF", "divan" -> "D".
fun initialsOf(displayName: String?, email: String?): String {
    val words = displayName?.trim()?.split(Regex("\\s+"))?.filter { it.isNotEmpty() }.orEmpty()
    val letters = if (words.isNotEmpty()) {
        words.take(2).map { it.first() }
    } else {
        listOfNotNull(email?.trim()?.firstOrNull())
    }
    return letters.joinToString("").uppercase()
}
