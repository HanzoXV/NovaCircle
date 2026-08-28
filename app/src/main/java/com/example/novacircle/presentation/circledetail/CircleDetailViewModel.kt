package com.example.novacircle.presentation.circledetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.novacircle.domain.model.CircleMember
import com.example.novacircle.domain.repository.CircleRepository
import com.example.novacircle.domain.repository.InviteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
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

data class CircleDetailUiState(
    val circleName: String = "",
    val members: List<CircleMember> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentUserId: String? = null,
    val isInviting: Boolean = false,
    val inviteError: String? = null,
    val inviteSuccess: Boolean = false
)

private var realtimeChannel: RealtimeChannel? = null

@HiltViewModel
class CircleDetailViewModel @Inject constructor(
    private val circleRepository: CircleRepository,
    private val inviteRepository: InviteRepository,
    private val client: SupabaseClient,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val circleId: String = checkNotNull(savedStateHandle["circleId"])

    private val _uiState = MutableStateFlow(CircleDetailUiState())
    val uiState: StateFlow<CircleDetailUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(
            currentUserId = client.auth.currentUserOrNull()?.id
        )
        refresh(showLoading = true)
        subscribeToChanges()
    }


    private fun subscribeToChanges() {
        viewModelScope.launch {
            val channel = client.realtime.channel("circle-$circleId-${System.currentTimeMillis()}")
            realtimeChannel = channel

            val memberFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "group_members"
                filter("group_id", FilterOperator.EQ, circleId)
            }
            val locationFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "locations"
            }

            launch { memberFlow.collect { refresh(showLoading = false) } }
            launch { locationFlow.collect { refresh(showLoading = false) } }

            channel.subscribe()
        }
    }

    fun refresh(showLoading: Boolean = false) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }
            circleRepository.getCircleMembers(circleId)
                .onSuccess { members ->
                    _uiState.value = _uiState.value.copy(members = members, isLoading = false, error = null)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
        }
    }
    fun inviteUsername(username: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isInviting = true, inviteError = null, inviteSuccess = false)
            inviteRepository.inviteUsername(circleId, username)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isInviting = false, inviteSuccess = true)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isInviting = false, inviteError = e.message)
                }
        }
    }
    fun resetInviteState() {
        _uiState.value = _uiState.value.copy(inviteError = null, inviteSuccess = false)
    }
    fun toggleMyLocationSharing(enabled: Boolean) {
        val uid = _uiState.value.currentUserId ?: return
        val previous = _uiState.value.members

        _uiState.value = _uiState.value.copy(
            members = _uiState.value.members.map {
                if (it.userId == uid) it.copy(isSharingLocation = enabled) else it
            }
        )

        viewModelScope.launch {
            circleRepository.setCircleLocationSharing(circleId, enabled)
                .onFailure { e ->
                    android.util.Log.e("NovaCircleCircle", "Toggle sharing failed", e)
                    _uiState.value = _uiState.value.copy(members = previous)
                }
        }
    }

    fun kickMember(userId: String) {
        viewModelScope.launch {
            circleRepository.kickMember(circleId, userId).onSuccess { refresh() }
        }
    }

    fun leaveCircle(onLeft: () -> Unit) {
        viewModelScope.launch {
            circleRepository.leaveCircle(circleId).onSuccess { onLeft() }
        }
    }

    fun deleteCircle(onDeleted: () -> Unit) {
        viewModelScope.launch {
            circleRepository.deleteCircle(circleId).onSuccess { onDeleted() }
        }
    }
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            realtimeChannel?.unsubscribe()
        }
    }
}