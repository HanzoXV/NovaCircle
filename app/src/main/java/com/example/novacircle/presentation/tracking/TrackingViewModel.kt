package com.example.novacircle.presentation.tracking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.novacircle.domain.model.MemberLocationDetail
import com.example.novacircle.domain.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrackingUiState(
    val detail: MemberLocationDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val cooldownSecondsLeft: Int = 0
)

@HiltViewModel
class TrackingViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val userId: String = checkNotNull(savedStateHandle["userId"])

    private val _uiState = MutableStateFlow(TrackingUiState())
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            locationRepository.getMemberLocation(userId)
                .onSuccess { detail ->
                    android.util.Log.d("TrackingVM", "Location Success: Lat=${detail.latitude}, Lng=${detail.longitude}, Sharing=${detail.isSharingLocation}")
                    _uiState.value = _uiState.value.copy(detail = detail, isLoading = false, error = null)
                }
                .onFailure { e ->
                    android.util.Log.e("TrackingVM", "Location Failure", e)
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Unknown error")
                }
        }
    }

    fun pingUpdate() {
        if (_uiState.value.cooldownSecondsLeft > 0) return

        viewModelScope.launch {
            locationRepository.requestFreshPing(userId)
            refresh()
            startCooldown()
        }
    }

    private fun startCooldown() {
        viewModelScope.launch {
            for (remaining in 20 downTo 0) {
                _uiState.value = _uiState.value.copy(cooldownSecondsLeft = remaining)
                kotlinx.coroutines.delay(1000)
            }
        }
    }
}