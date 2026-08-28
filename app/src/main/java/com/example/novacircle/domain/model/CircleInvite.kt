package com.example.novacircle.domain.model

data class CircleInvite(
    val id: String,
    val groupId: String,
    val groupName: String,
    val invitedByUsername: String,
    val status: String,
    val createdAt: String
)