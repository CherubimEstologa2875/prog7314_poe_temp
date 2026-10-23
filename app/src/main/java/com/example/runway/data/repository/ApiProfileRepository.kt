package com.example.runway.data.repository

import com.example.runway.data.remote.api.RunwayApi
import com.example.runway.data.remote.api.UserDto
import com.example.runway.domain.model.UserProfile
import com.example.runway.domain.repository.ProfileRepository

// Reads the signed-in user's profile from GET /api/v1/users/me.
class ApiProfileRepository(
    private val api: RunwayApi,
) : ProfileRepository {

    override suspend fun profile(): Result<UserProfile> =
        runCatching { api.getProfile().toDomain() }

    private fun UserDto.toDomain() = UserProfile(
        displayName = displayName,
        email = email,
        photoUrl = photoUrl,
        createdAt = createdAt,
    )
}
