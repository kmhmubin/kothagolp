package com.kmhmubin.kothagolp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmhmubin.kothagolp.domain.model.FontCategory
import com.kmhmubin.kothagolp.domain.model.FontFamily
import com.kmhmubin.kothagolp.domain.model.MaxWidth
import com.kmhmubin.kothagolp.domain.model.ReaderSettings
import com.kmhmubin.kothagolp.domain.model.ReaderTheme
import com.kmhmubin.kothagolp.domain.model.VolumeKeyDirection
import com.kmhmubin.kothagolp.ui.screens.reader.theme.ReaderColors
import com.kmhmubin.kothagolp.ui.theme.AppElevation
import com.kmhmubin.kothagolp.ui.theme.AppShape
import com.kmhmubin.kothagolp.ui.theme.Success
import com.kmhmubin.kothagolp.ui.theme.Warning
import com.kmhmubin.kothagolp.domain.model.FontWeight as ReaderFontWeight
import com.kmhmubin.kothagolp.domain.model.TextAlign as ReaderTextAlign

// =============================================================================
// DESIGN TOKENS
// =============================================================================

private object BarTheme {
    val success = Success
    val successMuted = Success.copy(alpha = 0.15f)
    val warning = Warning
}

// Settings tabs for inline settings
private enum class SettingsTab(val title: String, val icon: ImageVector) {
    PRESETS("Presets", Icons.Default.AutoAwesome),
    TYPOGRAPHY("Text", Icons.Default.TextFields),
    LAYOUT("Layout", Icons.Default.SpaceBar),
    APPEARANCE("Theme", Icons.Default.Palette),
    BEHAVIOR("Options", Icons.Outlined.Settings)
}

// =============================================================================
// MAIN BOTTOM BAR
// =============================================================================

@Composable
fun ReaderBottomBar(
    settings: ReaderSettings,
    onSettingsChange: (ReaderSettings) -> Unit,
    onOpenChapterList: () -> Unit,
    onStartTTS: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSettingsExpandedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()
    val haptic = LocalHapticFeedback.current
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(showSettings) { onSettingsExpandedChange(showSettings) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = navBarPadding.calculateBottomPadding() + 8.dp)
    ) {
        // Inline Settings Panel
        AnimatedVisibility(
            visible = showSettings,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f)
            ) + fadeIn(tween(200)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.9f, stiffness = 400f)
            ) + fadeOut(tween(150))
        ) {
            Column {
                InlineSettingsPanel(
                    settings = settings,
                    onSettingsChange = onSettingsChange,
                    onNavigateToSettings = {
                        showSettings = false
                        onNavigateToSettings()
                    },
                    onDismiss = { showSettings = false }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Main Bottom Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = AppShape.extraLarge,
            color = Color.Transparent,
            tonalElevation = AppElevation.xl,
            shadowElevation = 16.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceContainer,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)
                            )
                        ),
                        shape = AppShape.extraLarge
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomBarButton(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        label = "Chapters",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenChapterList()
                        }
                    )

                    ListenButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStartTTS()
                        }
                    )

                    BottomBarButton(
                        icon = if (showSettings) Icons.Rounded.Close else Icons.Rounded.Tune,
                        label = "Settings",
                        isActive = showSettings,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showSettings = !showSettings
                        }
                    )
                }
            }
        }
    }
}

// =============================================================================
// BOTTOM BAR BUTTONS
// =============================================================================

@Composable
private fun BottomBarButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isActive: Boolean = false
) {
    val iconColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "iconColor"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
        label = "bgColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "buttonScale"
    )

    Surface(
        onClick = onClick,
        shape = AppShape.large,
        color = bgColor,
        modifier = Modifier.scale(scale)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                    fontSize = 11.sp
                ),
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun ListenButton(onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "buttonScale"
    )

    Surface(
        onClick = onClick,
        shape = AppShape.extraLarge,
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = AppElevation.lg,
        modifier = Modifier
            .height(54.dp)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Headphones,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = "Listen",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = Color.White
            )
        }
    }
}

// =============================================================================
// INLINE SETTINGS PANEL
// =============================================================================

@Composable
private fun InlineSettingsPanel(
    settings: ReaderSettings,
    onSettingsChange: (ReaderSettings) -> Unit,
    onNavigateToSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var selectedTab by remember { mutableStateOf(SettingsTab.PRESETS) }
    val scrollState = rememberScrollState()

    // Count modified settings from default
    val modifiedCount = remember(settings) {
        countModifiedSettings(settings, ReaderSettings.DEFAULT)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 450.dp),
        shape = AppShape.extraLarge,
        color = Color.Transparent,
        tonalElevation = AppElevation.lg,
        shadowElevation = 14.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    shape = AppShape.extraLarge
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with Reset
                SettingsHeader(
                    modifiedCount = modifiedCount,
                    onReset = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSettingsChange(ReaderSettings.DEFAULT)
                    },
                    onDismiss = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDismiss()
                    }
                )

                // Tab Bar
                SettingsTabBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedTab = tab
                    }
                )

                // Tab Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    AnimatedContent(
                        targetState = selectedTab,
                        label = "tabContent"
                    ) { tab ->
                        when (tab) {
                            SettingsTab.PRESETS -> PresetsSettingsContent(
                                settings = settings,
                                onSettingsChange = onSettingsChange
                            )

                            SettingsTab.TYPOGRAPHY -> TypographySettingsContent(
                                settings = settings,
                                onSettingsChange = onSettingsChange
                            )

                            SettingsTab.LAYOUT -> LayoutSettingsContent(
                                settings = settings,
                                onSettingsChange = onSettingsChange
                            )

                            SettingsTab.APPEARANCE -> AppearanceSettingsContent(
                                settings = settings,
                                onSettingsChange = onSettingsChange
                            )

                            SettingsTab.BEHAVIOR -> BehaviorSettingsContent(
                                settings = settings,
                                onSettingsChange = onSettingsChange
                            )
                        }
                    }
                }

                // More Settings Button at the bottom
                MoreSettingsButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToSettings()
                    }
                )
            }
        }
    }
}

@Composable
private fun MoreSettingsButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        shape = AppShape.large,
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "All Reader Settings",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SettingsHeader(
    modifiedCount: Int,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column {
                Text(
                    text = "Reader Settings",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (modifiedCount > 0) {
                    Text(
                        text = "$modifiedCount changes from default",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Reset button
            if (modifiedCount > 0) {
                Surface(
                    onClick = onReset,
                    shape = AppShape.medium,
                    color = BarTheme.warning.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, BarTheme.warning.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            tint = BarTheme.warning,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Reset",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = BarTheme.warning
                        )
                    }
                }
            }

            // Close button
            Surface(
                onClick = onDismiss,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsTabBar(
    selectedTab: SettingsTab,
    onTabSelected: (SettingsTab) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(SettingsTab.entries) { tab ->
            val isSelected = tab == selectedTab
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                label = "tabBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "tabText"
            )

            Surface(
                onClick = { onTabSelected(tab) },
                shape = AppShape.medium,
                color = bgColor
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = textColor
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
}

// =============================================================================
// HELPER FUNCTION
// =============================================================================

private fun countModifiedSettings(current: ReaderSettings, default: ReaderSettings): Int {
    var count = 0
    if (current.fontSize != default.fontSize) count++
    if (current.fontFamily != default.fontFamily) count++
    if (current.fontWeight != default.fontWeight) count++
    if (current.lineHeight != default.lineHeight) count++
    if (current.letterSpacing != default.letterSpacing) count++
    if (current.wordSpacing != default.wordSpacing) count++
    if (current.textAlign != default.textAlign) count++
    if (current.hyphenation != default.hyphenation) count++
    if (current.maxWidth != default.maxWidth) count++
    if (current.marginHorizontal != default.marginHorizontal) count++
    if (current.marginVertical != default.marginVertical) count++
    if (current.paragraphSpacing != default.paragraphSpacing) count++
    if (current.paragraphIndent != default.paragraphIndent) count++
    if (current.theme != default.theme) count++
    if (current.brightness != default.brightness) count++
    if (current.warmthFilter != default.warmthFilter) count++
    if (current.scrollMode != default.scrollMode) count++
    if (current.pageAnimation != default.pageAnimation) count++
    if (current.readingDirection != default.readingDirection) count++
    if (current.scrollSensitivity != default.scrollSensitivity) count++
    if (current.volumeKeyNavigation != default.volumeKeyNavigation) count++
    if (current.volumeKeyDirection != default.volumeKeyDirection) count++
    if (current.autoHideControlsDelay != default.autoHideControlsDelay) count++
    if (current.keepScreenOn != default.keepScreenOn) count++
    if (current.showProgress != default.showProgress) count++
    if (current.showReadingTime != default.showReadingTime) count++
    if (current.showChapterTitle != default.showChapterTitle) count++
    if (current.immersiveMode != default.immersiveMode) count++
    if (current.autoScrollEnabled != default.autoScrollEnabled) count++
    if (current.autoScrollSpeed != default.autoScrollSpeed) count++
    return count
}

// =============================================================================
// TAB CONTENT - PRESETS
// =============================================================================

@Composable
private fun PresetsSettingsContent(
    settings: ReaderSettings,
    onSettingsChange: (ReaderSettings) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val presets = remember { ReaderSettings.getPresets() }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Info text
        Surface(
            shape = AppShape.medium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Choose a preset for quick setup, then customize individual settings in other tabs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Preset cards
        presets.forEach { (name, preset) ->
            val isSelected = isPresetMatch(settings, preset)

            PresetCard(
                name = name,
                preset = preset,
                isSelected = isSelected,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSettingsChange(preset)
                }
            )
        }
    }
}

@Composable
private fun PresetCard(
    name: String,
    preset: ReaderSettings,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        label = "presetBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
        label = "presetBg"
    )

    Surface(
        onClick = onClick,
        shape = AppShape.large,
        color = bgColor,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    if (isSelected) {
                        Surface(
                            shape = AppShape.extraSmall,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildPresetDescription(preset),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun buildPresetDescription(preset: ReaderSettings): String {
    return buildString {
        append("${preset.fontSize}sp")
        append(" · ${preset.fontFamily.displayName}")
        append(" · ${preset.theme.displayName}")
        if (preset.lineHeight != ReaderSettings.DEFAULT_LINE_HEIGHT) {
            append(" · ${String.format("%.1f", preset.lineHeight)}x line")
        }
    }
}

private fun isPresetMatch(current: ReaderSettings, preset: ReaderSettings): Boolean {
    return current.fontSize == preset.fontSize &&
            current.fontFamily == preset.fontFamily &&
            current.theme == preset.theme &&
            current.lineHeight == preset.lineHeight &&
            current.maxWidth == preset.maxWidth
}

// =============================================================================
// TAB CONTENT - TYPOGRAPHY
// =============================================================================

@Composable
private fun TypographySettingsContent(
    settings: ReaderSettings,
    onSettingsChange: (ReaderSettings) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SettingSectionCard(title = "Font Size", trailing = "${settings.fontSize}sp") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Aa", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "Aa", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Slider(
                    value = settings.fontSize.toFloat(),
                    onValueChange = { onSettingsChange(settings.withFontSize(it.toInt())) },
                    valueRange = ReaderSettings.MIN_FONT_SIZE.toFloat()..ReaderSettings.MAX_FONT_SIZE.toFloat(),
                    steps = ReaderSettings.MAX_FONT_SIZE - ReaderSettings.MIN_FONT_SIZE - 1,
                    colors = sliderColors()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(14, 16, 18, 20, 24).forEach { size ->
                        QuickOptionChip(
                            text = "$size",
                            isSelected = settings.fontSize == size,
                            onClick = { onSettingsChange(settings.withFontSize(size)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        SettingSectionCard(title = "Font Family") {
            FontFamilySelector(
                currentFont = settings.fontFamily,
                onFontChange = { font ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSettingsChange(settings.withFontFamily(font))
                }
            )
        }

        SettingSectionCard(title = "Font Weight") {
            FontWeightSelector(
                currentWeight = settings.fontWeight,
                onWeightChange = { weight ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSettingsChange(settings.copy(fontWeight = weight))
                }
            )
        }

        SettingSectionCard(title = "Line Height", trailing = String.format("%.1fx", settings.lineHeight)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Tight" to 1.2f, "Normal" to 1.6f, "Relaxed" to 2.0f, "Loose" to 2.4f).forEach { (label, value) ->
                        QuickOptionChip(
                            text = label,
                            isSelected = kotlin.math.abs(settings.lineHeight - value) < 0.15f,
                            onClick = { onSettingsChange(settings.withLineHeight(value)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Slider(
                    value = settings.lineHeight,
                    onValueChange = { onSettingsChange(settings.withLineHeight(it)) },
                    valueRange = ReaderSettings.MIN_LINE_HEIGHT..ReaderSettings.MAX_LINE_HEIGHT,
                    colors = sliderColors()
                )
            }
        }

        SettingSectionCard(title = "Letter Spacing", trailing = String.format("%.2fem", settings.letterSpacing)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Tight" to -0.03f, "Normal" to 0f, "Wide" to 0.05f, "Extra" to 0.1f).forEach { (label, value) ->
                    QuickOptionChip(
                        text = label,
                        isSelected = kotlin.math.abs(settings.letterSpacing - value) < 0.01f,
                        onClick = { onSettingsChange(settings.copy(letterSpacing = value)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        SettingSectionCard(title = "Text Alignment") {
            TextAlignmentSelector(
                currentAlign = settings.textAlign,
                onAlignChange = { align ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSettingsChange(settings.copy(textAlign = align))
                }
            )
        }

        ToggleSetting(
            title = "Hyphenation",
            subtitle = "Break long words at line ends",
            checked = settings.hyphenation,
            onCheckedChange = { onSettingsChange(settings.copy(hyphenation = it)) }
        )
    }
}

// =============================================================================
// TAB CONTENT - LAYOUT
// =============================================================================

@Composable
private fun LayoutSettingsContent(
    settings: ReaderSettings,
    onSettingsChange: (ReaderSettings) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SettingSectionCard(title = "Content Width") {
            MaxWidthSelector(
                currentMaxWidth = settings.maxWidth,
                onMaxWidthChange = { maxWidth ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSettingsChange(settings.copy(maxWidth = maxWidth))
                }
            )
        }

        SettingSectionCard(title = "Horizontal Margin", trailing = "${settings.marginHorizontal}dp") {
            SliderWithLabels(
                value = settings.marginHorizontal.toFloat(),
                onValueChange = { onSettingsChange(settings.withMargins(horizontal = it.toInt())) },
                valueRange = ReaderSettings.MIN_MARGIN.toFloat()..ReaderSettings.MAX_MARGIN.toFloat(),
                startLabel = "0",
                endLabel = "48"
            )
        }

        SettingSectionCard(title = "Vertical Margin", trailing = "${settings.marginVertical}dp") {
            SliderWithLabels(
                value = settings.marginVertical.toFloat(),
                onValueChange = { onSettingsChange(settings.withMargins(vertical = it.toInt())) },
                valueRange = ReaderSettings.MIN_MARGIN.toFloat()..ReaderSettings.MAX_MARGIN.toFloat(),
                startLabel = "0",
                endLabel = "48"
            )
        }

        SettingSectionCard(title = "Paragraph Spacing", trailing = String.format("%.1fx", settings.paragraphSpacing)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Compact" to 0.5f, "Normal" to 1.2f, "Relaxed" to 1.8f, "Loose" to 2.5f).forEach { (label, value) ->
                        QuickOptionChip(
                            text = label,
                            isSelected = kotlin.math.abs(settings.paragraphSpacing - value) < 0.2f,
                            onClick = { onSettingsChange(settings.copy(paragraphSpacing = value)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Slider(
                    value = settings.paragraphSpacing,
                    onValueChange = { onSettingsChange(settings.copy(paragraphSpacing = it)) },
                    valueRange = ReaderSettings.MIN_PARAGRAPH_SPACING..ReaderSettings.MAX_PARAGRAPH_SPACING,
                    colors = sliderColors()
                )
            }
        }

        SettingSectionCard(title = "First Line Indent", trailing = String.format("%.1fem", settings.paragraphIndent)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("None" to 0f, "Small" to 1f, "Medium" to 2f, "Large" to 3f).forEach { (label, value) ->
                    QuickOptionChip(
                        text = label,
                        isSelected = kotlin.math.abs(settings.paragraphIndent - value) < 0.1f,
                        onClick = { onSettingsChange(settings.copy(paragraphIndent = value)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// =============================================================================
// TAB CONTENT - APPEARANCE
// =============================================================================

@Composable
private fun AppearanceSettingsContent(
    settings: ReaderSettings,
    onSettingsChange: (ReaderSettings) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SettingSectionCard(title = "Color Theme") {
            ThemeGrid(
                currentTheme = settings.theme,
                onThemeChange = { theme ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSettingsChange(settings.withTheme(theme))
                }
            )
        }

        SettingSectionCard(title = "Brightness") {
            BrightnessControl(
                brightness = settings.brightness,
                onBrightnessChange = { onSettingsChange(settings.withBrightness(it)) },
                onResetToSystem = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSettingsChange(settings.resetBrightness())
                }
            )
        }

        SettingSectionCard(
            title = "Night Warmth",
            trailing = if (settings.warmthFilter == 0f) "Off" else "${(settings.warmthFilter * 100).toInt()}%"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SliderWithLabels(
                    value = settings.warmthFilter,
                    onValueChange = { onSettingsChange(settings.copy(warmthFilter = it)) },
                    valueRange = 0f..1f,
                    startLabel = "Off",
                    endLabel = "Warm"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(AppShape.extraSmall)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.White, Color(0xFFFFF4E0), Color(0xFFFFE4B5), Color(0xFFFFD49A))
                            )
                        )
                )
            }
        }

        ToggleSetting(
            title = "Show Reading Progress",
            subtitle = "Display progress bar at the top",
            checked = settings.showProgress,
            onCheckedChange = { onSettingsChange(settings.copy(showProgress = it)) }
        )

        ToggleSetting(
            title = "Show Reading Time",
            subtitle = "Estimated time remaining",
            checked = settings.showReadingTime,
            onCheckedChange = { onSettingsChange(settings.copy(showReadingTime = it)) }
        )

        ToggleSetting(
            title = "Show Chapter Title",
            subtitle = "Display chapter name in header",
            checked = settings.showChapterTitle,
            onCheckedChange = { onSettingsChange(settings.copy(showChapterTitle = it)) }
        )
    }
}

// =============================================================================
// TAB CONTENT - BEHAVIOR
// =============================================================================

@Composable
private fun BehaviorSettingsContent(
    settings: ReaderSettings,
    onSettingsChange: (ReaderSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SettingSectionCard(title = "Screen") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ToggleSetting(
                    title = "Keep Screen On",
                    subtitle = "Prevent screen from turning off",
                    checked = settings.keepScreenOn,
                    onCheckedChange = { onSettingsChange(settings.copy(keepScreenOn = it)) }
                )
                ToggleSetting(
                    title = "Immersive Mode",
                    subtitle = "Hide status bar while reading",
                    checked = settings.immersiveMode,
                    onCheckedChange = { onSettingsChange(settings.copy(immersiveMode = it)) }
                )
            }
        }

        SettingSectionCard(title = "Volume Keys") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ToggleSetting(
                    title = "Volume Key Navigation",
                    subtitle = "Use volume buttons to turn pages",
                    checked = settings.volumeKeyNavigation,
                    onCheckedChange = { onSettingsChange(settings.copy(volumeKeyNavigation = it)) }
                )
                AnimatedVisibility(visible = settings.volumeKeyNavigation) {
                    VolumeKeyDirectionSelector(
                        currentDirection = settings.volumeKeyDirection,
                        onDirectionChange = { onSettingsChange(settings.copy(volumeKeyDirection = it)) }
                    )
                }
            }
        }

        SettingSectionCard(title = "Scrolling") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ToggleSetting(
                    title = "Smooth Scroll",
                    subtitle = "Enable smooth scroll animations",
                    checked = settings.smoothScroll,
                    onCheckedChange = { onSettingsChange(settings.copy(smoothScroll = it)) }
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Scroll Speed", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = String.format("%.1fx", settings.scrollSensitivity), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = settings.scrollSensitivity,
                        onValueChange = { onSettingsChange(settings.copy(scrollSensitivity = it)) },
                        valueRange = 0.5f..2.0f,
                        colors = sliderColors()
                    )
                }
            }
        }

        SettingSectionCard(title = "Auto-Scroll") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ToggleSetting(
                    title = "Enable Auto-Scroll",
                    subtitle = "Hands-free reading mode",
                    checked = settings.autoScrollEnabled,
                    onCheckedChange = { onSettingsChange(settings.copy(autoScrollEnabled = it)) }
                )
                AnimatedVisibility(visible = settings.autoScrollEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Scroll Speed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = String.format("%.1f lines/sec", settings.autoScrollSpeed), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = settings.autoScrollSpeed,
                            onValueChange = { onSettingsChange(settings.copy(autoScrollSpeed = it)) },
                            valueRange = ReaderSettings.MIN_AUTO_SCROLL_SPEED..ReaderSettings.MAX_AUTO_SCROLL_SPEED,
                            colors = sliderColors()
                        )
                    }
                }
            }
        }

        ToggleSetting(
            title = "TTS Auto-Advance",
            subtitle = "Automatically play next chapter",
            checked = settings.ttsAutoAdvanceChapter,
            onCheckedChange = { onSettingsChange(settings.copy(ttsAutoAdvanceChapter = it)) }
        )

        SettingSectionCard(title = "Accessibility") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ToggleSetting(
                    title = "Force High Contrast",
                    checked = settings.forceHighContrast,
                    onCheckedChange = { onSettingsChange(settings.copy(forceHighContrast = it)) }
                )
                ToggleSetting(
                    title = "Reduce Motion",
                    checked = settings.reduceMotion,
                    onCheckedChange = { onSettingsChange(settings.copy(reduceMotion = it)) }
                )
                ToggleSetting(
                    title = "Larger Touch Targets",
                    checked = settings.largerTouchTargets,
                    onCheckedChange = { onSettingsChange(settings.copy(largerTouchTargets = it)) }
                )
            }
        }
    }
}

// =============================================================================
// REUSABLE COMPONENTS
// =============================================================================

@Composable
private fun SettingSectionCard(
    title: String,
    trailing: String? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShape.large,
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (trailing != null) {
                    Text(text = trailing, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                }
            }
            content()
        }
    }
}

@Composable
private fun SliderWithLabels(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    startLabel: String,
    endLabel: String,
    steps: Int = 0
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = startLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, steps = steps, modifier = Modifier.weight(1f), colors = sliderColors())
        Text(text = endLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = MaterialTheme.colorScheme.primary,
    activeTrackColor = MaterialTheme.colorScheme.primary,
    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainer
)

@Composable
private fun ToggleSetting(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(onClick = { onCheckedChange(!checked) }, shape = AppShape.large, color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                if (subtitle != null) { Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MaterialTheme.colorScheme.primary, uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant, uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer))
        }
    }
}

@Composable
private fun QuickOptionChip(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val backgroundColor by animateColorAsState(targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer, label = "chipBg")
    val textColor by animateColorAsState(targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, label = "chipText")

    Surface(onClick = onClick, modifier = modifier.height(38.dp), shape = AppShape.medium, color = backgroundColor, border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text = text, style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal), color = textColor)
        }
    }
}

@Composable
private fun FontFamilySelector(currentFont: FontFamily, onFontChange: (FontFamily) -> Unit) {
    val fontsByCategory = remember { FontFamily.getByCategory() }
    var expandedCategory by remember { mutableStateOf<FontCategory?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val popularFonts = listOf(FontFamily.SYSTEM_SERIF, FontFamily.LITERATA, FontFamily.ROBOTO, FontFamily.MERRIWEATHER, FontFamily.ATKINSON)
            items(popularFonts) { font ->
                val isSelected = font == currentFont
                Surface(onClick = { onFontChange(font) }, shape = AppShape.medium, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer, border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null) {
                    Text(text = font.displayName, style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal), color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), maxLines = 1)
                }
            }
        }

        fontsByCategory.entries.sortedBy { it.key.sortOrder }.forEach { (category, fonts) ->
            val isExpanded = expandedCategory == category
            val hasSelectedFont = fonts.any { it == currentFont }

            Surface(onClick = { expandedCategory = if (isExpanded) null else category }, shape = AppShape.medium, color = if (hasSelectedFont) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainer) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = category.displayName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = if (hasSelectedFont) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            if (hasSelectedFont) { Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(8.dp)) {} }
                        }
                        Icon(imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            fonts.forEach { font ->
                                val isSelected = font == currentFont
                                Surface(onClick = { onFontChange(font) }, shape = AppShape.small, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = font.displayName, style = MaterialTheme.typography.bodyMedium, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                                            if (font.description.isNotEmpty()) { Text(text = font.description, style = MaterialTheme.typography.labelSmall, color = if (isSelected) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                        }
                                        if (isSelected) { Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FontWeightSelector(currentWeight: ReaderFontWeight, onWeightChange: (ReaderFontWeight) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ReaderFontWeight.COMMON.forEach { weight ->
            val isSelected = weight == currentWeight
            val bgColor by animateColorAsState(targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, label = "weightBg")

            Surface(onClick = { onWeightChange(weight) }, modifier = Modifier.weight(1f), shape = AppShape.medium, color = bgColor, border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null) {
                Column(modifier = Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Aa", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight(weight.value)), color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Text(text = weight.displayName, style = MaterialTheme.typography.labelSmall, color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun TextAlignmentSelector(currentAlign: ReaderTextAlign, onAlignChange: (ReaderTextAlign) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ReaderTextAlign.entries.forEach { align ->
            val isSelected = align == currentAlign
            val icon = when (align) {
                ReaderTextAlign.LEFT -> Icons.AutoMirrored.Filled.FormatAlignLeft
                ReaderTextAlign.RIGHT -> Icons.AutoMirrored.Filled.FormatAlignRight
                ReaderTextAlign.CENTER -> Icons.Default.FormatAlignCenter
                ReaderTextAlign.JUSTIFY -> Icons.Default.FormatAlignJustify
            }

            Surface(onClick = { onAlignChange(align) }, modifier = Modifier.weight(1f), shape = AppShape.medium, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null) {
                Column(modifier = Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = icon, contentDescription = null, tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = align.displayName, style = MaterialTheme.typography.labelSmall, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun MaxWidthSelector(currentMaxWidth: MaxWidth, onMaxWidthChange: (MaxWidth) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MaxWidth.entries.forEach { maxWidth ->
            val isSelected = maxWidth == currentMaxWidth
            Surface(onClick = { onMaxWidthChange(maxWidth) }, shape = AppShape.medium, color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent, border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.width(when (maxWidth) { MaxWidth.NARROW -> 24.dp; MaxWidth.MEDIUM -> 32.dp; MaxWidth.LARGE -> 40.dp; MaxWidth.EXTRA_LARGE -> 48.dp; MaxWidth.FULL -> 56.dp }).height(4.dp).clip(AppShape.extraSmall).background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)))
                        Column {
                            Text(text = maxWidth.displayName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal), color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            Text(text = maxWidth.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                    if (isSelected) { Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ThemeGrid(currentTheme: ReaderTheme, onThemeChange: (ReaderTheme) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(ReaderTheme.LIGHT, ReaderTheme.SEPIA, ReaderTheme.DARK, ReaderTheme.AMOLED).forEach { theme ->
            val themeColors = remember(theme) { ReaderColors.fromTheme(theme) }
            val isSelected = theme == currentTheme
            val scale by animateFloatAsState(targetValue = if (isSelected) 1.05f else 1f, label = "themeScale")

            Surface(onClick = { onThemeChange(theme) }, modifier = Modifier.weight(1f).height(56.dp).scale(scale), shape = AppShape.medium, color = themeColors.background, border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Aa", style = MaterialTheme.typography.labelMedium, color = themeColors.text)
                        if (isSelected) { Spacer(modifier = Modifier.height(2.dp)); Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrightnessControl(brightness: Float, onBrightnessChange: (Float) -> Unit, onResetToSystem: () -> Unit) {
    val isSystemBrightness = brightness == ReaderSettings.BRIGHTNESS_SYSTEM
    var localBrightness by remember(brightness) { mutableFloatStateOf(if (isSystemBrightness) 0.5f else brightness) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = if (isSystemBrightness) "Using system brightness" else "${(localBrightness * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            Surface(onClick = { if (isSystemBrightness) onBrightnessChange(0.5f) else onResetToSystem() }, shape = AppShape.small, color = if (isSystemBrightness) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant) {
                Text(text = if (isSystemBrightness) "Manual" else "Auto", style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSystemBrightness) FontWeight.SemiBold else FontWeight.Normal), color = if (isSystemBrightness) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(imageVector = Icons.Default.BrightnessLow, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            Slider(value = localBrightness, onValueChange = { localBrightness = it }, onValueChangeFinished = { onBrightnessChange(localBrightness) }, enabled = !isSystemBrightness, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = if (isSystemBrightness) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.primary, activeTrackColor = if (isSystemBrightness) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.surface, disabledThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f), disabledActiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)))
            Icon(imageVector = Icons.Default.BrightnessHigh, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun VolumeKeyDirectionSelector(currentDirection: VolumeKeyDirection, onDirectionChange: (VolumeKeyDirection) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VolumeKeyDirection.entries.forEach { direction ->
            val isSelected = direction == currentDirection
            Surface(onClick = { onDirectionChange(direction) }, modifier = Modifier.weight(1f), shape = AppShape.medium, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = direction.displayName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal), color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Text(text = direction.description, style = MaterialTheme.typography.labelSmall, color = if (isSelected) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f), textAlign = TextAlign.Center)
                }
            }
        }
    }
}