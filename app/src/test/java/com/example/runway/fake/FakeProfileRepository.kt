package com.example.runway.fake

import com.example.runway.domain.model.UserProfile
import com.example.runway.domain.repository.ProfileRepository

class FakeProfileRepository(
    var profile: UserProfile = UserProfile(),
) : ProfileRepository {

    var error: Throwable? = null

    override suspend fun profile(): Result<UserProfile> =
        error?.let { Result.failure(it) } ?: Result.success(profile)
}
