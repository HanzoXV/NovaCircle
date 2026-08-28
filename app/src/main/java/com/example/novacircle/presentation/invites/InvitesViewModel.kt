package com.example.novacircle.presentation.invites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.novacircle.domain.model.CircleInvite
import com.example.novacircle.domain.repository.InviteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InvitesUiState(
    val invites: List<CircleInvite> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class InvitesViewModel @Inject constructor(
    private val inviteRepository: InviteRepository,
    private val client: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvitesUiState())
    val uiState: StateFlow<InvitesUiState> = _uiState.asStateFlow()

    private var realtimeChannel: RealtimeChannel? = null

    init {
        refresh(showLoading = true)
        subscribeToChanges()
    }

    private fun subscribeToChanges() {
        val uid = client.auth.currentUserOrNull()?.id ?: return

        viewModelScope.launch {
            val channel = client.realtime.channel("invites-$uid-${System.currentTimeMillis()}")
            realtimeChannel = channel

            val inviteFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "group_invites"
            }

            launch { inviteFlow.collect { refresh(showLoading = false) } }

            channel.subscribe()
        }
    }

    fun refresh(showLoading: Boolean = false) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }
            inviteRepository.getMyPendingInvites()
                .onSuccess { invites ->
                    _uiState.value = _uiState.value.copy(invites = invites, isLoading = false, error = null)
                }
                .onFailure { e ->
                    android.util.Log.e("NovaCircleInvites", "Failed to load invites", e)
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
        }
    }

    fun accept(invite: CircleInvite) {
        viewModelScope.launch {
            inviteRepository.acceptInvite(invite.id, invite.groupId).onSuccess { refresh(showLoading = false) }
        }
    }

    fun decline(invite: CircleInvite) {
        viewModelScope.launch {
            inviteRepository.declineInvite(invite.id).onSuccess { refresh(showLoading = false) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            realtimeChannel?.unsubscribe()
        }
    }
}