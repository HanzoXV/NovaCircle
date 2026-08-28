package com.example.novacircle.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.novacircle.domain.model.Circle
import com.example.novacircle.domain.model.CircleMember
import com.example.novacircle.domain.repository.CircleRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.result.PostgrestResult
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.format.DateTimeParseException
import javax.inject.Inject

private const val ONLINE_THRESHOLD_SECONDS = 120L
@Serializable
private data class MyMembershipRow(val group_id: String)

@Serializable
private data class CircleRow(val id: String, val name: String, val owner_id: String)

@Serializable
private data class MemberRow(
    val user_id: String,
    val role: String,
    val location_sharing_enabled: Boolean,
    val profiles: ProfileEmbed
)

@Serializable
private data class ProfileEmbed(
    val username: String,
    val full_name: String? = null,
    val avatar_url: String? = null
)

@Serializable
private data class LocationRow(val user_id: String, val updated_at: String)

@Serializable
private data class MemberCountRow(val user_id: String)

class SupabaseCircleRepository @Inject constructor(
    private val client: SupabaseClient
) : CircleRepository {

    override suspend fun getMyCircles(): Result<List<Circle>> = runCatching {
        val uid = client.auth.currentUserOrNull()?.id ?: error("Not authenticated")

        val myGroupIds = client.postgrest["group_members"]
            .select(columns = Columns.raw("group_id")) { filter { eq("user_id", uid) } }
            .decodeList<MyMembershipRow>()
            .map { it.group_id }
            .toSet()

        if (myGroupIds.isEmpty()) return@runCatching emptyList()

        val circles = client.postgrest["groups"]
            .select { filter { isIn("id", myGroupIds.toList()) } }
            .decodeList<CircleRow>()

        circles.map { c ->
            val count = client.postgrest["group_members"]
                .select { filter { eq("group_id", c.id) } }
                .decodeList<MemberCountRow>()
                .size
            Circle(id = c.id, name = c.name, ownerId = c.owner_id, memberCount = count)
        }
    }
    private fun mapCircleError(e: Throwable): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("groups_owner_name_unique", ignoreCase = true) ||
                    (msg.contains("duplicate key", ignoreCase = true) && msg.contains("groups", ignoreCase = true)) ->
                "You already have a circle with that name"
            msg.contains("network", ignoreCase = true) || msg.contains("timeout", ignoreCase = true) ->
                "Network error — check your connection and try again"
            else -> "Couldn't create circle. Please try again"
        }
    }

    override suspend fun createCircle(name: String): Result<Circle> = runCatching {
        val uid = client.auth.currentUserOrNull()?.id ?: error("Not authenticated")

        val created = client.postgrest["groups"]
            .insert(mapOf("name" to name, "owner_id" to uid)) { select() }
            .decodeSingle<CircleRow>()

        client.postgrest["group_members"].insert(
            mapOf("group_id" to created.id, "user_id" to uid, "role" to "owner")
        )

        Circle(id = created.id, name = created.name, ownerId = uid, memberCount = 1)
    }.recoverCatching { e ->
        android.util.Log.e("NovaCircleDashboard", "Create circle failed", e)
        throw Exception(mapCircleError(e))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun getCircleMembers(circleId: String): Result<List<CircleMember>> = runCatching {
        val members = client.postgrest["group_members"]
            .select(columns = Columns.raw("user_id, role, location_sharing_enabled, profiles(username, full_name, avatar_url)")) {
                filter { eq("group_id", circleId) }
            }
            .decodeList<MemberRow>()

        val locations = client.postgrest["locations"]
            .select { filter { isIn("user_id", members.map { it.user_id }) } }
            .decodeList<LocationRow>()
            .associateBy { it.user_id }

        val now = Instant.now()

        members.map { m ->
            val loc = locations[m.user_id]
            val isOnline = loc?.updated_at?.let {
                try {
                    Instant.parse(it).let { ts -> now.epochSecond - ts.epochSecond <= ONLINE_THRESHOLD_SECONDS }
                } catch (e: DateTimeParseException) {
                    false
                }
            } ?: false

            CircleMember(
                userId = m.user_id,
                username = m.profiles.username,
                fullName = m.profiles.full_name,
                avatarUrl = m.profiles.avatar_url,
                role = m.role,
                isSharingLocation = m.location_sharing_enabled,
                isOnline = isOnline,
                lastLocationAt = loc?.updated_at
            )
        }
    }
    override suspend fun deleteCircle(circleId: String): Result<Unit> = runCatching {
        client.postgrest["groups"].delete { filter { eq("id", circleId) } }
    }

    override suspend fun setCircleLocationSharing(circleId: String, enabled: Boolean): Result<Unit> =
        runCatching {
            val uid = client.auth.currentUserOrNull()?.id ?: error("Not authenticated")
            client.postgrest["group_members"]
                .update({ set("location_sharing_enabled", enabled) }) {
                    filter {
                        eq("group_id", circleId)
                        eq("user_id", uid)
                    }
                }
        }

    override suspend fun kickMember(circleId: String, userId: String): Result<Unit> = runCatching {
        client.postgrest["group_members"].delete {
            filter {
                eq("group_id", circleId)
                eq("user_id", userId)
            }
        }
    }

    override suspend fun leaveCircle(circleId: String): Result<PostgrestResult> = runCatching {
        val uid = client.auth.currentUserOrNull()?.id ?: error("Not authenticated")
        client.postgrest["group_members"].delete {
            filter {
                eq("group_id", circleId)
                eq("user_id", uid)
            }
        }
    }.onFailure { e ->
        android.util.Log.e("NovaCircleCircle", "Leave circle failed", e)
    }
    override suspend fun getCircleName(circleId: String): Result<String> = runCatching {
        client.postgrest["groups"]
            .select { filter { eq("id", circleId) } }
            .decodeSingle<CircleRow>()
            .name
    }
}