package com.example.novacircle.presentation.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.novacircle.domain.model.Circle
import com.example.novacircle.domain.model.NovaProfile
import com.example.novacircle.domain.repository.CircleRepository
import com.example.novacircle.domain.repository.InviteRepository
import com.example.novacircle.domain.repository.ProfileRepository
import com.example.novacircle.service.LocationTrackingService
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject


data class DashboardUiState(
    val circles: List<Circle> = emptyList(),
    val profile: NovaProfile? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isCreatingCircle: Boolean = false,
    val createCircleError: String? = null,
    val pendingInviteCount: Int = 0
)



@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val circleRepository: CircleRepository,
    private val profileRepository: ProfileRepository,
    private val inviteRepository: InviteRepository,
    private val client: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _circleCreated = MutableSharedFlow<Unit>()
    val circleCreated: SharedFlow<Unit> = _circleCreated

    private var realtimeChannel: RealtimeChannel? = null

    init {
        refresh(showLoading = true)
        subscribeToChanges()
    }

    private fun subscribeToChanges() {
        val uid = client.auth.currentUserOrNull()?.id ?: return

        viewModelScope.launch {
            val channel = client.realtime.channel("dashboard-$uid-${System.currentTimeMillis()}")
            realtimeChannel = channel

            val memberFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "group_members"
            }
            val groupFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "groups"
            }
            val inviteFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "group_invites"
            }

            launch { memberFlow.collect { refresh(showLoading = false) } }
            launch { groupFlow.collect { refresh(showLoading = false) } }
            launch { inviteFlow.collect { refresh(showLoading = false) } }

            channel.subscribe()
        }
    }

    fun refresh(showLoading: Boolean = false) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }

            val circlesResult = circleRepository.getMyCircles()
            val profileResult = profileRepository.getMyProfile()
            val invitesResult = inviteRepository.getMyPendingInvites()

            _uiState.value = _uiState.value.copy(
                circles = circlesResult.getOrDefault(emptyList()),
                profile = profileResult.getOrNull() ?: _uiState.value.profile,
                pendingInviteCount = invitesResult.getOrDefault(emptyList()).size,
                isLoading = false,
                error = circlesResult.exceptionOrNull()?.message
                    ?: profileResult.exceptionOrNull()?.message
            )
        }
    }

    fun createCircle(name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingCircle = true, createCircleError = null)
            circleRepository.createCircle(name)
                .onSuccess {
                    android.util.Log.d("NovaCircleDashboard", "Circle created: ${it.id} / ${it.name}")
                    _uiState.value = _uiState.value.copy(isCreatingCircle = false)
                    refreshCirclesOnly()
                    _circleCreated.emit(Unit)
                }
                .onFailure { e ->
                    android.util.Log.e("NovaCircleDashboard", "Create circle failed", e)
                    _uiState.value = _uiState.value.copy(
                        isCreatingCircle = false,
                        createCircleError = e.message ?: "Couldn't create circle"
                    )
                }
        }
    }
    fun setGlobalLocationSharing(enabled: Boolean, context: Context) {
        val previous = _uiState.value.profile
        _uiState.value = _uiState.value.copy(
            profile = _uiState.value.profile?.copy(locationSharingEnabled = enabled)
        )
        viewModelScope.launch {
            profileRepository.setGlobalLocationSharing(enabled)
                .onSuccess {
                    if (enabled) LocationTrackingService.start(context) else LocationTrackingService.stop(context)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(profile = previous)
                }
        }
    }
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            realtimeChannel?.unsubscribe()
        }
    }

    fun updateFullName(fullName: String) {
        val previous = _uiState.value.profile
        _uiState.value = _uiState.value.copy(
            profile = _uiState.value.profile?.copy(fullName = fullName)
        )
        viewModelScope.launch {
            profileRepository.updateFullName(fullName)
                .onFailure { e ->
                    android.util.Log.e("NovaCircleDashboard", "Update full name failed", e)
                    _uiState.value = _uiState.value.copy(profile = previous)
                }
        }
    }

    private fun refreshCirclesOnly() {
        viewModelScope.launch {
            circleRepository.getMyCircles()
                .onSuccess { circles ->
                    android.util.Log.d("NovaCircleDashboard", "Circles refreshed: ${circles.size}")
                    _uiState.value = _uiState.value.copy(circles = circles)
                }
                .onFailure { e ->
                    android.util.Log.e("NovaCircleDashboard", "Refresh circles failed", e)
                }
        }
    }
}