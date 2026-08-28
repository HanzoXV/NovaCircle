package com.example.novacircle.data.repository

import com.example.novacircle.data.model.ProfileRow
import com.example.novacircle.domain.model.NovaUser
import com.example.novacircle.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.Serializable
import javax.inject.Inject

private const val EMAIL_SUFFIX = "@novacircle.app"
private val KNOWN_CLEAN_MESSAGES = setOf(
    "Username is already taken",
    "You already have a circle named",
)
@Serializable
private data class ProfileInsert(
    val id: String,
    val username: String
)

class SupabaseAuthRepository @Inject constructor(
    private val client: SupabaseClient
) : AuthRepository {

    override suspend fun signUp(username: String, password: String): Result<NovaUser> {
        return runCatching {
            val normalized = username.trim().lowercase()

            if (isUsernameTaken(normalized)) {
                error("Username is already taken")
            }

            client.auth.signUpWith(Email) {
                email = "$normalized$EMAIL_SUFFIX"
                this.password = password
            }

            val userId = client.auth.currentUserOrNull()?.id
                ?: error("Signup succeeded but no session was returned")

            client.postgrest["profiles"].insert(
                ProfileInsert(id = userId, username = normalized)
            )

            NovaUser(id = userId, username = normalized)
        }.recoverCatching { e ->
            android.util.Log.e("NovaCircleAuth", "SignUp failed", e)
            throw Exception(mapAuthError(e))
        }
    }

    override suspend fun login(username: String, password: String): Result<NovaUser> {
        return runCatching {
            val normalized = username.trim().lowercase()

            client.auth.signInWith(Email) {
                email = "$normalized$EMAIL_SUFFIX"
                this.password = password
            }

            val userId = client.auth.currentUserOrNull()?.id
                ?: error("Login succeeded but no session was returned")

            NovaUser(id = userId, username = normalized)
        }.recoverCatching { e ->
            android.util.Log.e("NovaCircleAuth", "Login failed", e)
            throw Exception(mapAuthError(e))
        }
    }

    override suspend fun logout() {
        client.auth.signOut()
    }

    override suspend fun currentUser(): NovaUser? {
        val session = client.auth.currentUserOrNull() ?: return null
        val profile = client.postgrest["profiles"]
            .select { filter { eq("id", session.id) } }
            .decodeSingleOrNull<ProfileRow>()
            ?: return null

        return NovaUser(id = profile.id, username = profile.username)
    }

    override suspend fun isUsernameTaken(username: String): Boolean {
        return client.postgrest.rpc(
            "is_username_taken",
            mapOf("check_username" to username.trim().lowercase())
        ).decodeAs<Boolean>()
    }

    private fun mapAuthError(e: Throwable): String {
        val msg = e.message.orEmpty()

        if (msg == "Username is already taken") return msg

        return when {
            msg.contains("Invalid login credentials", ignoreCase = true) ->
                "Incorrect username or password"
            msg.contains("Email not confirmed", ignoreCase = true) ->
                "Account setup incomplete — please contact support"
            msg.contains("User already registered", ignoreCase = true) ->
                "That username is already taken"
            msg.contains("Password should be at least", ignoreCase = true) ->
                "Password must be at least 6 characters"
            msg.contains("network", ignoreCase = true) || msg.contains("timeout", ignoreCase = true) ->
                "Network error — check your connection and try again"
            else -> "Something went wrong. Please try again"
        }
    }
}