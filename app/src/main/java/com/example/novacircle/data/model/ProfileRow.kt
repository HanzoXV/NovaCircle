package com.example.novacircle.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ProfileRow(
    val id: String,
    val username: String,
    val full_name: String? = null,
    val avatar_url: String? = null,
    val location_sharing_enabled: Boolean = true
)