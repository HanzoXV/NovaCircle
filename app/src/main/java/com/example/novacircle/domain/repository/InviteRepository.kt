package com.example.novacircle.domain.repository

import com.example.novacircle.domain.model.CircleInvite

interface InviteRepository {
    suspend fun inviteUsername(circleId: String, username: String): Result<Unit>
    suspend fun getMyPendingInvites(): Result<List<CircleInvite>>
    suspend fun acceptInvite(inviteId: String, groupId: String): Result<Unit>
    suspend fun declineInvite(inviteId: String): Result<Unit>
}