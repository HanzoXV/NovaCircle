package com.example.novacircle.domain.model

data class NovaProfile(
    val id: String,
    val username: String,
    val fullName: String?,
    val avatarUrl: String?,
    val locationSharingEnabled: Boolean
)
