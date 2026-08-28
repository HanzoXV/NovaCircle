package com.example.novacircle.domain.repository

import com.example.novacircle.domain.model.MemberLocationDetail

interface LocationRepository {
    suspend fun getMemberLocation(userId: String): Result<MemberLocationDetail>
    suspend fun requestFreshPing(userId: String): Result<Unit>
}