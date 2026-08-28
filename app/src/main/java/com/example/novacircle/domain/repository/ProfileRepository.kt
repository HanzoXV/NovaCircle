package com.example.novacircle.domain.repository

import com.example.novacircle.domain.model.NovaProfile

interface ProfileRepository {
    suspend fun getMyProfile(): Result<NovaProfile>
    suspend fun updateFullName(fullName: String): Result<Unit>
    suspend fun updateAvatarUrl(avatarUrl: String): Result<Unit>
    suspend fun setGlobalLocationSharing(enabled: Boolean): Result<Unit>
}