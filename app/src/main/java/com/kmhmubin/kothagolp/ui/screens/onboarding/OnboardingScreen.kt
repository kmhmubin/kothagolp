package com.kmhmubin.kothagolp.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Battery5Bar
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Source
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kmhmubin.kothagolp.recommendation.TagNormalizer.TagCategory
import com.kmhmubin.kothagolp.recommendation.model.GenreOption
import com.kmhmubin.kothagolp.recommendation.model.OnboardingGenres
import com.kmhmubin.kothagolp.ui.theme.AppShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Navigate away when complete
    LaunchedEffect(state.currentStep) {
        if (state.currentStep == OnboardingStep.COMPLETE) {
            onComplete()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Progress indicator
            if (state.currentStep != OnboardingStep.WELCOME &&
                state.currentStep != OnboardingStep.SEEDING &&
                state.currentStep != OnboardingStep.COMPLETE) {
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Content
            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "step_content"
            ) { step ->
                when (step) {
                    OnboardingStep.WELCOME -> WelcomeStep(
                        onNext = { viewModel.nextStep() },
                        onSkip = { viewModel.skipOnboarding() }
                    )
                    OnboardingStep.PERMISSIONS -> PermissionsStep(
                        storageFolderUri = state.storageFolderUri,
                        onFolderSelected = { viewModel.setStorageFolder(it) },
                        onNext = { viewModel.nextStep() },
                        onBack = { viewModel.previousStep() }
                    )
                    OnboardingStep.PROVIDERS -> ProvidersStep(
                        providers = state.availableProviders,
                        selectedProviders = state.selectedProviders,
                        isLoadingProviders = state.isLoadingProviders,
                        isDownloadingSource = state.isDownloadingSource,
                        sourceDownloadProgress = state.sourceDownloadProgress,
                        sourceDownloadStatus = state.sourceDownloadStatus,
                        sourceDownloadError = state.sourceDownloadError,
                        onToggleProvider = { viewModel.toggleProvider(it) },
                        onSelectAll = { viewModel.selectAllProviders() },
                        onDeselectAll = { viewModel.deselectAllProviders() },
                        onRetryDownload = { viewModel.retrySourceDownload() },
                        onNext = { viewModel.nextStep() },
                        onBack = { viewModel.previousStep() }
                    )
                    OnboardingStep.GENRES -> GenresStep(
                        likedGenres = state.likedGenres,
                        dislikedGenres = state.dislikedGenres,
                        onSetPreference = { genre, pref -> viewModel.setGenrePreference(genre, pref) },
                        onNext = { viewModel.nextStep() },
                        onBack = { viewModel.previousStep() }
                    )
                    OnboardingStep.CONTENT -> ContentStep(
                        includeMature = state.includeMatureContent,
                        includeBL = state.includeBLContent,
                        includeGL = state.includeGLContent,
                        onMatureChange = { viewModel.setMatureContent(it) },
                        onBLChange = { viewModel.setBLContent(it) },
                        onGLChange = { viewModel.setGLContent(it) },
                        onNext = { viewModel.nextStep() },
                        onBack = { viewModel.previousStep() }
                    )
                    OnboardingStep.READY -> ReadyStep(
                        selectedProviders = state.selectedProviders.size,
                        likedGenres = state.likedGenres.size,
                        onStart = { viewModel.nextStep() },
                        onBack = { viewModel.previousStep() }
                    )
                    OnboardingStep.SEEDING -> SeedingStep(
                        progress = state.seedingProgress
                    )
                    OnboardingStep.COMPLETE -> {
                        // Will trigger navigation
                    }
                }
            }
        }
    }
}

// ================================================================
// STEP 1: WELCOME
// ================================================================

@Composable
private fun WelcomeStep(
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(0.2f))

        // Icon
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Welcome to Kothagolp",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Pick your sources and genres — we'll find novels you'll love.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(0.3f))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = AppShape.large
        ) {
            Text("Get Started", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onSkip) {
            Text("Skip setup", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.weight(0.1f))
    }
}

// ================================================================
// STEP 2: PERMISSIONS
// ================================================================

@Composable
private fun PermissionsStep(
    storageFolderUri: String?,
    onFolderSelected: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var installAppsGranted by remember { mutableStateOf(false) }
    var notificationsGranted by remember { mutableStateOf(false) }
    var batteryGranted by remember { mutableStateOf(false) }

    // Re-check permission states each time user returns from Settings
    DisposableEffect(lifecycleOwner.lifecycle) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                installAppsGranted = context.packageManager.canRequestPackageInstalls()
                notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else true
                batteryGranted = context.getSystemService<PowerManager>()
                    ?.isIgnoringBatteryOptimizations(context.packageName) ?: false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* state re-checked on resume */ }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            onFolderSelected(uri.toString())
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        StepHeader(
            title = "App Permissions",
            subtitle = "Grant access for the best experience",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // ── Install unknown apps ──────────────────────────────────
            item {
                PermissionItem(
                    icon = Icons.Rounded.InstallMobile,
                    title = "Install Source Updates",
                    subtitle = "Required to download and load novel source plugins. Without this, sources cannot be installed.",
                    granted = installAppsGranted,
                    required = true,
                    onGrant = {
                        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            // ── Notifications ─────────────────────────────────────────
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    PermissionItem(
                        icon = Icons.Rounded.NotificationsActive,
                        title = "Notifications",
                        subtitle = "Get alerts for new chapters, download progress, and library update results.",
                        granted = notificationsGranted,
                        required = false,
                        onGrant = {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            // ── Battery optimisation ──────────────────────────────────
            item {
                @Suppress("BatteryLife")
                PermissionItem(
                    icon = Icons.Rounded.Battery5Bar,
                    title = "Unrestricted Battery Usage",
                    subtitle = "Prevents the OS from killing background library updates, downloads, and backup restores mid-run.",
                    granted = batteryGranted,
                    required = false,
                    onGrant = {
                        val intent = Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        ).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            // ── Storage folder ────────────────────────────────────────
            item {
                val folderName = storageFolderUri?.let {
                    Uri.parse(it).lastPathSegment?.substringAfterLast(':') ?: "Selected"
                }
                PermissionItem(
                    icon = Icons.Rounded.Folder,
                    title = "Download Folder",
                    subtitle = if (folderName != null)
                        "Folder: $folderName"
                    else
                        "Choose where to save EPUB exports and backup files. Recommended but optional.",
                    granted = storageFolderUri != null,
                    required = false,
                    grantLabel = if (storageFolderUri != null) "Change" else "Choose",
                    onGrant = { folderLauncher.launch(null) }
                )
            }
        }

        // Hint: all optional
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = AppShape.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "You can change these later in Settings. Install Source Updates is required to load any novel sources.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        StepNavigation(
            canProceed = true,
            onNext = onNext,
            nextLabel = "Continue"
        )
    }
}

@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    required: Boolean,
    modifier: Modifier = Modifier,
    grantLabel: String = "Grant",
    onGrant: () -> Unit
) {
    ListItem(
        modifier = modifier,
        leadingContent = {
            Surface(
                shape = AppShape.medium,
                color = if (granted)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (granted)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        },
        headlineContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (required) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "Required",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            if (granted && grantLabel == "Grant") {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "Granted",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onGrant,
                    shape = AppShape.small
                ) {
                    Text(grantLabel, style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

// ================================================================
// STEP 3: PROVIDERS
// ================================================================

@Composable
private fun ProvidersStep(
    providers: List<ProviderInfo>,
    selectedProviders: Set<String>,
    isLoadingProviders: Boolean,
    isDownloadingSource: Boolean,
    sourceDownloadProgress: Float,
    sourceDownloadStatus: String,
    sourceDownloadError: String?,
    onToggleProvider: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onRetryDownload: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        StepHeader(
            title = "Choose Your Sources",
            subtitle = "Pick where to find your novels",
            onBack = onBack
        )

        when {
            // ── Error state ──────────────────────────────────────────
            sourceDownloadError != null -> {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Text(
                            text = "Download Failed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = sourceDownloadError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = onRetryDownload) {
                            Text("Retry Download")
                        }
                    }
                }
            }

            // ── Downloading ──────────────────────────────────────────
            isDownloadingSource || (isLoadingProviders && providers.isEmpty()) -> {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudDownload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Text(
                            text = "Setting Up Sources",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Surface(
                            shape = AppShape.large,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = { sourceDownloadProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(AppShape.extraSmall),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = sourceDownloadStatus.ifBlank { "Downloading…" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${(sourceDownloadProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Text(
                            text = "This only happens once on first install.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ── Loaded — show provider list ──────────────────────────
            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onSelectAll) { Text("Select All") }
                    TextButton(onClick = onDeselectAll) { Text("Clear") }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(providers, key = { it.name }) { provider ->
                        ProviderCard(
                            provider = provider,
                            isSelected = provider.name in selectedProviders,
                            onToggle = { onToggleProvider(provider.name) }
                        )
                    }
                }
            }
        }

        StepNavigation(
            canProceed = !isLoadingProviders && selectedProviders.isNotEmpty() && sourceDownloadError == null,
            onNext = onNext,
            nextLabel = "Continue"
        )
    }
}

@Composable
private fun ProviderCard(
    provider: ProviderInfo,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        shape = AppShape.large,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = provider.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Novel count chip
                    Surface(
                        shape = AppShape.small,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = provider.novelCount,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Genre chips
                    provider.genres.take(2).forEach { genre ->
                        Surface(
                            shape = AppShape.small,
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ================================================================
// STEP 3: GENRES
// ================================================================

@Composable
private fun GenresStep(
    likedGenres: Set<TagCategory>,
    dislikedGenres: Set<TagCategory>,
    onSetPreference: (TagCategory, GenrePreference) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        StepHeader(
            title = "What do you like?",
            subtitle = "Tap to like a genre. Tap again to dislike. We'll use this to personalise your feed.",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Main genres
            item {
                GenreSection(
                    title = "Main Genres",
                    genres = OnboardingGenres.mainGenres,
                    likedGenres = likedGenres,
                    dislikedGenres = dislikedGenres,
                    onSetPreference = onSetPreference
                )
            }

            // Sub-genres
            item {
                GenreSection(
                    title = "Popular Sub-Genres",
                    genres = OnboardingGenres.subGenres,
                    likedGenres = likedGenres,
                    dislikedGenres = dislikedGenres,
                    onSetPreference = onSetPreference
                )
            }

            // Tones
            item {
                GenreSection(
                    title = "Tone & Mood",
                    genres = OnboardingGenres.tones,
                    likedGenres = likedGenres,
                    dislikedGenres = dislikedGenres,
                    onSetPreference = onSetPreference
                )
            }

            // Protagonist types
            item {
                GenreSection(
                    title = "Protagonist Types",
                    genres = OnboardingGenres.protagonistTypes,
                    likedGenres = likedGenres,
                    dislikedGenres = dislikedGenres,
                    onSetPreference = onSetPreference
                )
            }
        }

        StepNavigation(
            canProceed = true,
            onNext = onNext,
            nextLabel = "Continue"
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenreSection(
    title: String,
    genres: List<GenreOption>,
    likedGenres: Set<TagCategory>,
    dislikedGenres: Set<TagCategory>,
    onSetPreference: (TagCategory, GenrePreference) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            genres.forEach { genre ->
                val preference = when {
                    genre.category in likedGenres -> GenrePreference.LIKED
                    genre.category in dislikedGenres -> GenrePreference.DISLIKED
                    else -> GenrePreference.NEUTRAL
                }

                GenreChip(
                    genre = genre,
                    preference = preference,
                    onClick = {
                        val newPref = when (preference) {
                            GenrePreference.NEUTRAL -> GenrePreference.LIKED
                            GenrePreference.LIKED -> GenrePreference.DISLIKED
                            GenrePreference.DISLIKED -> GenrePreference.NEUTRAL
                        }
                        onSetPreference(genre.category, newPref)
                    }
                )
            }
        }
    }
}

@Composable
private fun GenreChip(
    genre: GenreOption,
    preference: GenrePreference,
    onClick: () -> Unit
) {
    val containerColor = when (preference) {
        GenrePreference.LIKED -> MaterialTheme.colorScheme.primaryContainer
        GenrePreference.DISLIKED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        GenrePreference.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val contentColor = when (preference) {
        GenrePreference.LIKED -> MaterialTheme.colorScheme.primary
        GenrePreference.DISLIKED -> MaterialTheme.colorScheme.error
        GenrePreference.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val borderColor = when (preference) {
        GenrePreference.LIKED -> MaterialTheme.colorScheme.primary
        GenrePreference.DISLIKED -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
        GenrePreference.NEUTRAL -> Color.Transparent
    }

    Surface(
        onClick = onClick,
        shape = AppShape.medium,
        color = containerColor,
        border = if (preference != GenrePreference.NEUTRAL) {
            BorderStroke(1.dp, borderColor)
        } else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            genre.icon?.let { emoji ->
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = genre.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )

            if (preference != GenrePreference.NEUTRAL) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = when (preference) {
                        GenrePreference.LIKED -> Icons.Rounded.ThumbUp
                        GenrePreference.DISLIKED -> Icons.Rounded.ThumbDown
                        else -> Icons.Rounded.Remove
                    },
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// ================================================================
// STEP 4: CONTENT SETTINGS
// ================================================================

@Composable
private fun ContentStep(
    includeMature: Boolean,
    includeBL: Boolean,
    includeGL: Boolean,
    onMatureChange: (Boolean) -> Unit,
    onBLChange: (Boolean) -> Unit,
    onGLChange: (Boolean) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        StepHeader(
            title = "Content Preferences",
            subtitle = "Control what shows up in your feed",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            ContentToggle(
                title = "Mature Content",
                description = "Show novels with adult themes, violence, or explicit content",
                icon = "🔞",
                isEnabled = includeMature,
                onToggle = onMatureChange
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ContentToggle(
                title = "Boys Love (BL)",
                description = "Include male/male romance stories (Yaoi, Danmei)",
                icon = "👨‍❤️‍👨",
                isEnabled = includeBL,
                onToggle = onBLChange
            )

            ContentToggle(
                title = "Girls Love (GL)",
                description = "Include female/female romance stories (Yuri, Baihe)",
                icon = "👩‍❤️‍👩",
                isEnabled = includeGL,
                onToggle = onGLChange
            )

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = AppShape.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "You can change these anytime in Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        StepNavigation(
            canProceed = true,
            onNext = onNext,
            nextLabel = "Continue"
        )
    }
}

@Composable
private fun ContentToggle(
    title: String,
    description: String,
    icon: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle
        )
    }
}

// ================================================================
// STEP 5: READY
// ================================================================

@Composable
private fun ReadyStep(
    selectedProviders: Int,
    likedGenres: Int,
    onStart: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.15f))

        // Checkmark animation
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(100.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Rounded.RocketLaunch,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "You're All Set!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "We'll scan your chosen sources and build your recommendations.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Summary
        Surface(
            shape = AppShape.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryRow(
                    icon = Icons.Rounded.Source,
                    label = "Sources",
                    value = "$selectedProviders sources selected"
                )

                if (likedGenres > 0) {
                    SummaryRow(
                        icon = Icons.Rounded.Favorite,
                        label = "Preferences",
                        value = "$likedGenres genres liked"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.25f))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = AppShape.large
        ) {
            Icon(Icons.Rounded.RocketLaunch, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start Discovery", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Go Back")
        }

        Spacer(modifier = Modifier.weight(0.1f))
    }
}

@Composable
private fun SummaryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

// ================================================================
// STEP 6: SEEDING
// ================================================================

@Composable
private fun SeedingStep(
    progress: SeedingProgressInfo?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated loading
        Surface(
            shape = AppShape.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 4.dp,
            modifier = Modifier.size(110.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(
                    modifier = Modifier.size(52.dp),
                    strokeWidth = 5.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Discovering Novels",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        progress?.let { p ->
            Text(
                text = "Searching ${p.currentProvider}...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = AppShape.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { p.currentIndex.toFloat() / p.totalProviders.coerceAtLeast(1) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(AppShape.extraSmall),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Text(
                        text = "${p.currentIndex} of ${p.totalProviders} sources",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "This only happens once. We're scanning your sources based on your preferences.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ================================================================
// SHARED COMPONENTS
// ================================================================

@Composable
private fun StepHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.offset(x = (-12).dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StepNavigation(
    canProceed: Boolean,
    onNext: () -> Unit,
    nextLabel: String
) {
    Surface(
        tonalElevation = 3.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Button(
                onClick = onNext,
                enabled = canProceed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = AppShape.medium
            ) {
                Text(nextLabel, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
            }
        }
    }
}