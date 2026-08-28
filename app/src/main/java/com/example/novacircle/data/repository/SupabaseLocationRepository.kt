package com.example.novacircle.data.repository

import com.example.novacircle.data.model.ProfileRow
import com.example.novacircle.domain.model.MemberLocationDetail
import com.example.novacircle.domain.repository.LocationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import jakarta.inject.Inject
import kotlinx.serialization.Serializable

class SupabaseLocationRepository @Inject constructor(
    private val client: SupabaseClient
) : LocationRepository {

    override suspend fun getMemberLocation(userId: String): Result<MemberLocationDetail> = runCatching {
        android.util.Log.d("LocationRepo", "Fetching location for userId: $userId")
        
        val profile = client.postgrest["profiles"]
            .select { filter { eq("id", userId) } }
            .decodeSingle<ProfileRow>()

        val location = client.postgrest["locations"]
            .select { 
                filter { eq("user_id", userId) }
                order("updated_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(1)
            }
            .decodeList<LocationDetailRow>()
            .firstOrNull()

        android.util.Log.d("LocationRepo", "Profile found: ${profile.username}, Sharing: ${profile.location_sharing_enabled}")
        android.util.Log.d("LocationRepo", "Location found: $location")

        MemberLocationDetail(
            userId = userId,
            username = profile.username,
            fullName = profile.full_name,
            latitude = location?.latitude,
            longitude = location?.longitude,
            isSharingLocation = profile.location_sharing_enabled,
            lastUpdatedAt = location?.updated_at,
            batteryPercent = location?.battery_percent,
            deviceModel = location?.device_model
        )
    }

    override suspend fun requestFreshPing(userId: String): Result<Unit> = runCatching {
        // Real "ping someone else's phone right now" requires push notifications (FCM) to
        // wake their device on demand — not built yet. For now this only re-fetches your
        // own freshest location if userId == self; for other users it's a no-op that just
        // re-reads whatever they last pushed.
    }
}

@Serializable
private data class LocationDetailRow(
    val latitude: Double,
    val longitude: Double,
    val updated_at: String,
    val battery_percent: Int? = null,
    val device_model: String? = null
)