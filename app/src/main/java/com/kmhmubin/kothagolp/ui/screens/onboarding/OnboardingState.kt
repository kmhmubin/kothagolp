package com.kmhmubin.kothagolp.ui.screens.onboarding

import com.kmhmubin.kothagolp.recommendation.TagNormalizer.TagCategory
import com.kmhmubin.kothagolp.recommendation.model.OnboardingPreferences

data class OnboardingState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val preferences: OnboardingPreferences = OnboardingPreferences.EMPTY,

    // Provider selection
    val availableProviders: List<ProviderInfo> = emptyList(),
    val selectedProviders: Set<String> = emptySet(),
    val isLoadingProviders: Boolean = false,

    // Genre selection
    val likedGenres: Set<TagCategory> = emptySet(),
    val dislikedGenres: Set<TagCategory> = emptySet(),

    // Content settings
    val includeMatureContent: Boolean = true,
    val includeBLContent: Boolean = false,
    val includeGLContent: Boolean = false,

    // Seeding errors (populated after seeding completes)
    val seedingErrors: List<String> = emptyList(),

    // Storage folder (SAF URI string — persisted to SharedPreferences)
    val storageFolderUri: String? = null,

    // Seeding state
    val isSeeding: Boolean = false,
    val seedingProgress: SeedingProgressInfo? = null,

    // Error handling
    val error: String? = null
) {
    val canProceed: Boolean
        get() = when (currentStep) {
            OnboardingStep.WELCOME -> true
            OnboardingStep.PERMISSIONS -> true  // all permissions optional
            OnboardingStep.PROVIDERS -> selectedProviders.isNotEmpty()
            OnboardingStep.GENRES -> true
            OnboardingStep.CONTENT -> true
            OnboardingStep.READY -> true
            OnboardingStep.SEEDING -> false
            OnboardingStep.COMPLETE -> true
        }

    val progress: Float
        get() = (currentStep.ordinal.toFloat() / (OnboardingStep.entries.size - 1))

    fun toOnboardingPreferences(): OnboardingPreferences {
        return OnboardingPreferences(
            selectedProviders = selectedProviders,
            likedGenres = likedGenres,
            dislikedGenres = dislikedGenres,
            includeMatureContent = includeMatureContent,
            includeBLContent = includeBLContent,
            includeGLContent = includeGLContent,
            completed = true,
            completedAt = System.currentTimeMillis()
        )
    }
}

enum class OnboardingStep {
    WELCOME,
    PERMISSIONS,
    PROVIDERS,
    GENRES,
    CONTENT,
    READY,
    SEEDING,
    COMPLETE
}

data class ProviderInfo(
    val name: String,
    val description: String,
    val novelCount: String,
    val genres: List<String>,
    val isEnabled: Boolean = true
)

data class SeedingProgressInfo(
    val currentProvider: String,
    val currentIndex: Int,
    val totalProviders: Int,
    val novelsDiscovered: Int
)
