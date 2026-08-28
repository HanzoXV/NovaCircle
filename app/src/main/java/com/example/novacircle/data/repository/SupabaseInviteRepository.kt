package com.example.novacircle.data.repository

import com.example.novacircle.domain.model.CircleInvite
import com.example.novacircle.domain.repository.InviteRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class InviteRow(
    val id: String,
    val group_id: String,
    val status: String,
    val created_at: String,
    val groups: GroupEmbed? = null,
    val inviter: InviterEmbed
)

@Serializable
private data class GroupEmbed(val name: String)

@Serializable
private data class InviterEmbed(val username: String)

class SupabaseInviteRepository @Inject constructor(
    private val client: SupabaseClient
) : InviteRepository {

    override suspend fun inviteUsername(circleId: String, username: String): Result<Unit> = runCatching {
        val inviterUid = client.auth.currentUserOrNull()?.id ?: error("Not authenticated")

        val targetUserId = client.postgrest.rpc(
            "find_user_id_by_username",
            mapOf("check_username" to username.trim().lowercase())
        ).decodeAsOrNull<String>() ?: error("No user found with that username")

        if (targetUserId == inviterUid) {
            error("You can't invite yourself")
        }

        client.postgrest["group_invites"].insert(
            mapOf(
                "group_id" to circleId,
                "invited_by" to inviterUid,
                "invited_user_id" to targetUserId
            )
        )

        Unit
    }.recoverCatching { e ->
        val msg = e.message.orEmpty()
        if (msg.contains("duplicate key", ignoreCase = true) ||
            msg.contains("group_invites_group_id_invited_user_id_status_key", ignoreCase = true)
        ) {
            throw Exception("That user already has a pending invite to this circle")
        }
        throw e
    }

    override suspend fun getMyPendingInvites(): Result<List<CircleInvite>> = runCatching {
        val uid = client.auth.currentUserOrNull()?.id ?: error("Not authenticated")

        client.postgrest["group_invites"]
            .select(
                columns = Columns.raw(
                    "id, group_id, status, created_at, groups(name), inviter:profiles!invited_by(username)"
                )
            ) {
                filter {
                    eq("invited_user_id", uid)
                    eq("status", "pending")
                }
            }
            .decodeList<InviteRow>()
            .mapNotNull {
                val groupName = it.groups?.name ?: return@mapNotNull null
                CircleInvite(
                    id = it.id,
                    groupId = it.group_id,
                    groupName = groupName,
                    invitedByUsername = it.inviter.username,
                    status = it.status,
                    createdAt = it.created_at
                )
            }

    }.onFailure { e ->
        android.util.Log.e("NovaCircleInvites", "Failed to load invites", e)
    }

    override suspend fun acceptInvite(inviteId: String, groupId: String): Result<Unit> = runCatching {
        val uid = client.auth.currentUserOrNull()?.id ?: error("Not authenticated")

        client.postgrest["group_members"].insert(
            mapOf("group_id" to groupId, "user_id" to uid, "role" to "member")
        )

        client.postgrest["group_invites"]
            .update({ set("status", "accepted") }) { filter { eq("id", inviteId) } }
    }

    override suspend fun declineInvite(inviteId: String): Result<Unit> = runCatching {
        client.postgrest["group_invites"]
            .update({ set("status", "declined") }) { filter { eq("id", inviteId) } }
    }

}