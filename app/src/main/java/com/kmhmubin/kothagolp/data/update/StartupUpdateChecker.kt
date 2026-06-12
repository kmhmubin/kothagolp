package com.kmhmubin.kothagolp.data.update

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton to manage startup update checking and share results across the app
 */
object StartupUpdateChecker {

    private val _updateResult = MutableStateFlow<UpdateChecker.UpdateResult?>(null)
    val updateResult: StateFlow<UpdateChecker.UpdateResult?> = _updateResult.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _hasChecked = MutableStateFlow(false)
    val hasChecked: StateFlow<Boolean> = _hasChecked.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var hasRunStartupCheck = false

    /**
     * Check for updates on app startup (only runs once per app session)
     */
    suspend fun checkOnStartup(context: Context) {
        if (hasRunStartupCheck) return

        val preferences = UpdatePreferences(context)
        if (!preferences.getCheckUpdatesOnStartup()) {
            hasRunStartupCheck = true
            return
        }

        hasRunStartupCheck = true
        performCheck(context)
    }

    /**
     * Force a manual update check
     */
    suspend fun checkNow(context: Context) {
        performCheck(context)
    }

    private suspend fun performCheck(context: Context) {
        _isChecking.value = true
        _error.value = null

        val checker = UpdateChecker(context)
        val result = checker.checkForUpdate()

        result.onSuccess { updateResult ->
            _updateResult.value = updateResult
            _hasChecked.value = true
        }.onFailure { e ->
            _error.value = e.message ?: "Failed to check for updates"
            _hasChecked.value = true
        }

        _isChecking.value = false
    }

    /**
     * Reset the startup check flag (useful for testing)
     */
    fun resetStartupCheck() {
        hasRunStartupCheck = false
    }

    /**
     * Clear results
     */
    fun clearResults() {
        _updateResult.value = null
        _hasChecked.value = false
        _error.value = null
    }
}