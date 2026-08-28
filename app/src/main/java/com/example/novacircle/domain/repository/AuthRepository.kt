package com.example.novacircle.domain.repository

import com.example.novacircle.domain.model.NovaUser

interface AuthRepository {
    suspend fun signUp(username: String, password: String): Result<NovaUser>
    suspend fun login(username: String, password: String): Result<NovaUser>
    suspend fun logout()
    suspend fun currentUser(): NovaUser?
    suspend fun isUsernameTaken(username: String): Boolean
}