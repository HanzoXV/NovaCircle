package com.example.novacircle.domain.repository

import com.example.novacircle.domain.model.Circle
import com.example.novacircle.domain.model.CircleMember
import io.github.jan.supabase.postgrest.result.PostgrestResult

interface CircleRepository {
    suspend fun getMyCircles(): Result<List<Circle>>
    suspend fun createCircle(name: String): Result<Circle>
    suspend fun getCircleMembers(circleId: String): Result<List<CircleMember>>
    suspend fun setCircleLocationSharing(circleId: String, enabled: Boolean): Result<Unit>
    suspend fun kickMember(circleId: String, userId: String): Result<Unit>
    suspend fun leaveCircle(circleId: String): Result<PostgrestResult>

    suspend fun getCircleName(circleId: String): Result<String>
    suspend fun deleteCircle(circleId: String): Result<Unit>
}