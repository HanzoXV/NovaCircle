package com.example.novacircle.domain.model

data class CircleMember(
    val userId: String,
    val username: String,
    val fullName: String?,
    val avatarUrl: String?,
    val role: String,
    val isSharingLocation: Boolean,
    val isOnline: Boolean,
    val lastLocationAt: String?
)