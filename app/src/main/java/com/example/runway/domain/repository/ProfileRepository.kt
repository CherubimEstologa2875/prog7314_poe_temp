package com.example.runway.domain.repository

import com.example.runway.domain.model.UserProfile

interface ProfileRepository {
    suspend fun profile(): Result<UserProfile>
}
