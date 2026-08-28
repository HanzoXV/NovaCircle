package com.example.novacircle.data.repository

import com.example.novacircle.data.model.ProfileRow
import com.example.novacircle.domain.model.NovaProfile
import com.example.novacircle.domain.repository.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject

class SupabaseProfileRepository @Inject constructor(
    private val client: SupabaseClient
) : ProfileRepository {

    override suspend fun getMyProfile(): Result<NovaProfile> = runCatching {
        val uid = client.auth.currentUserOrNull()?.id ?: error("Not authenticated")
        val row = client.postgrest["profiles"]
            .select { filter { eq("id", uid) } }
            .decodeSingle<ProfileRow>()

        NovaProfile(
            id = row.id,
            username = row.username,
            fullName = row.full_name,
            avatarUrl = row.avatar_url,
            locationSharingEnabled = row.location_sharing_enabled
        )
    }

    override suspend fun updateFullName(fullName: String): Result<Unit> = runCatching {
        val uid = client.auth.currentUserOrNull()?.id ?: error("Not authenticated")
        client.postgrest["profiles"]
            .update({ set("full_name", fullName) }) { filter { eq("id", uid) } }
    }

    override suspend fun updateAvatarUrl(avatarUrl: String): Result<Unit> = runCatching {
        val uid = client.auth.currentUserOrNull()?.id ?: error("Not authenticated")
        client.postgrest["profiles"]
            .update({ set("avatar_url", avatarUrl) }) { filter { eq("id", uid) } }
    }

    override suspend fun setGlobalLocationSharing(enabled: Boolean): Result<Unit> = runCatching {
        val uid = client.auth.currentUserOrNull()?.id ?: error("Not authenticated")
        client.postgrest["profiles"]
            .update({ set("location_sharing_enabled", enabled) }) { filter { eq("id", uid) } }
    }
}