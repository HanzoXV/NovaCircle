package com.example.novacircle.domain.model

data class MemberLocationDetail(
    val userId: String,
    val username: String,
    val fullName: String?,
    val latitude: Double?,
    val longitude: Double?,
    val isSharingLocation: Boolean,
    val lastUpdatedAt: String?,
    val batteryPercent: Int?,
    val deviceModel: String?
)