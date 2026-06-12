package com.kmhmubin.kothagolp.ui.screens.about

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kmhmubin.kothagolp.data.update.StartupUpdateChecker
import com.kmhmubin.kothagolp.data.update.UpdateChecker
import com.kmhmubin.kothagolp.data.update.UpdatePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AboutUiState(
    val currentVersion: String = "",
    val isCheckingUpdate: Boolean = false,
    val updateResult: UpdateChecker.UpdateResult? = null,
    val updateError: String? = null,
    val hasChecked: Boolean = false,
    val checkUpdatesOnStartup: Boolean = true
)

class AboutViewModel(application: Application) : AndroidViewModel(application) {

    private val updateChecker = UpdateChecker(application)
    private val updatePreferences = UpdatePreferences(application)

    private val _uiState = MutableStateFlow(AboutUiState())
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(currentVersion = updateChecker.getCurrentVersion())
        }

        // Observe startup checker results
        viewModelScope.launch {
            combine(
                StartupUpdateChecker.updateResult,
                StartupUpdateChecker.isChecking,
                StartupUpdateChecker.hasChecked,
                StartupUpdateChecker.error,
                updatePreferences.checkUpdatesOnStartup
            ) { result, isChecking, hasChecked, error, checkOnStartup ->
                _uiState.value.copy(
                    updateResult = result,
                    isCheckingUpdate = isChecking,
                    hasChecked = hasChecked,
                    updateError = error,
                    checkUpdatesOnStartup = checkOnStartup
                )
            }.collect { newState ->
                _uiState.value = newState.copy(
                    currentVersion = updateChecker.getCurrentVersion()
                )
            }
        }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            StartupUpdateChecker.checkNow(getApplication())
        }
    }

    fun setCheckUpdatesOnStartup(enabled: Boolean) {
        viewModelScope.launch {
            updatePreferences.setCheckUpdatesOnStartup(enabled)
        }
    }
}