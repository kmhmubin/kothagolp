package com.kmhmubin.kothagolp.ui.screens.reader.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kmhmubin.kothagolp.domain.model.MaxWidth
import com.kmhmubin.kothagolp.domain.model.ReadingDirection
import com.kmhmubin.kothagolp.domain.model.AuthorNoteDisplayMode
import com.kmhmubin.kothagolp.ui.screens.reader.logic.BlockType
import com.kmhmubin.kothagolp.ui.screens.reader.model.ReaderDisplayItem
import com.kmhmubin.kothagolp.ui.screens.reader.model.ReaderUiState
import com.kmhmubin.kothagolp.ui.screens.reader.model.SentenceBoundsInSegment
import com.kmhmubin.kothagolp.ui.screens.reader.model.TTSScrollEdge
import com.kmhmubin.kothagolp.ui.screens.reader.theme.FontProvider
import com.kmhmubin.kothagolp.ui.screens.reader.theme.ReaderColors
import com.kmhmubin.kothagolp.domain.model.TextAlign as ReaderTextAlign

@Composable
fun ReaderContainer(
    uiState: ReaderUiState,
    colors: ReaderColors,
    listState: LazyListState,
    isScrollBounded: Boolean = false,
    highlightedDisplayIndex: Int = -1,
    ensureVisibleIndex: Int? = null,
    onEnsureVisibleHandled: () -> Unit = {},
    onSentenceBoundsUpdated: ((Int, Float, Float) -> Unit)? = null,
    currentSentenceBounds: SentenceBoundsInSegment = SentenceBoundsInSegment.INVALID,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onRetryChapter: (Int) -> Unit,
    onAddHighlight: ((text: String, color: String) -> Unit)? = null,
    onRemoveHighlight: ((id: Long) -> Unit)? = null,
    onUpdateNote: ((id: Long, note: String?) -> Unit)? = null,
    onChangeHighlightColor: ((id: Long, color: String) -> Unit)? = null
) {
    val settings = uiState.settings
    val density = LocalDensity.current

    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }
    var pendingSelectionText by remember { mutableStateOf<String?>(null) }
    var selectionSegmentId by remember { mutableStateOf<String?>(null) }
    var selectionStart by remember { mutableStateOf(-1) }
    var selectionEnd by remember { mutableStateOf(-1) }

    val clipboard = LocalClipboardManager.current
    val clearSelection: () -> Unit = {
        pendingSelectionText = null
        selectionSegmentId = null
        selectionStart = -1
        selectionEnd = -1
    }

    // Pre-group highlights by segmentId: O(1) lookup per item instead of O(n) filter per item
    val highlightsBySegment = remember(uiState.textHighlights) {
        uiState.textHighlights.groupBy { it.segmentId }
    }

    val layoutDirection = remember(settings.readingDirection) {
        when (settings.readingDirection) {
            ReadingDirection.RTL -> LayoutDirection.Rtl
            else -> LayoutDirection.Ltr
        }
    }

    val fontFamily = remember(settings.fontFamily) {
        FontProvider.getFontFamily(settings.fontFamily)
    }

    val textAlign = remember(settings.textAlign, settings.readingDirection) {
        mapTextAlign(settings.textAlign, settings.readingDirection)
    }

    val fontWeight = remember(settings.fontWeight) {
        mapFontWeight(settings.fontWeight)
    }

    val effectiveColors = remember(colors, settings.forceHighContrast) {
        if (settings.forceHighContrast) {
            colors.copy(
                text = if (colors.isDarkTheme) Color.White else Color.Black,
                textSecondary = if (colors.isDarkTheme)
                    Color.White.copy(alpha = 0.9f)
                else
                    Color.Black.copy(alpha = 0.9f)
            )
        } else {
            colors
        }
    }

    val horizontalPadding = remember(settings.marginHorizontal, settings.largerTouchTargets) {
        if (settings.largerTouchTargets) {
            (settings.marginHorizontal - 4).coerceAtLeast(8).dp
        } else {
            settings.marginHorizontal.dp
        }
    }

    val paragraphSpacing = remember(settings.paragraphSpacing, settings.fontSize) {
        (settings.fontSize * settings.paragraphSpacing * 0.5f).dp
    }

    val maxWidth = remember(settings.maxWidth) {
        when (settings.maxWidth) {
            MaxWidth.NARROW -> 480.dp
            MaxWidth.MEDIUM -> 600.dp
            MaxWidth.LARGE -> 720.dp
            MaxWidth.EXTRA_LARGE -> 900.dp
            MaxWidth.FULL -> Dp.Unspecified
        }
    }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    val touchTargetPadding = if (settings.largerTouchTargets) 8.dp else 0.dp

    // Track max insets ever seen — system bars report 0 when hidden (immersive mode).
    // Letting contentPadding shrink shifts the LazyColumn reading position on every chrome toggle.
    // By only ever increasing, the padding is stable once bars first appear.
    var maxTopInset by remember { mutableStateOf(0.dp) }
    var maxBottomInset by remember { mutableStateOf(0.dp) }
    val currentTopInset = statusBarPadding.calculateTopPadding()
    val currentBottomInset = navBarPadding.calculateBottomPadding()
    if (currentTopInset > maxTopInset) maxTopInset = currentTopInset
    if (currentBottomInset > maxBottomInset) maxBottomInset = currentBottomInset

    val topPadding = maxTopInset + settings.marginVertical.dp
    val bottomPadding = maxBottomInset + 100.dp + settings.marginVertical.dp + touchTargetPadding

    val topPaddingPx = with(density) { topPadding.roundToPx() }
    val bottomPaddingPx = with(density) { bottomPadding.roundToPx() }

    // Create bounded scroll connection with SENTENCE bounds
    val ttsBoundedScrollConnection = rememberTTSBoundedScrollConnection(
        listState = listState,
        highlightedIndex = highlightedDisplayIndex,
        isActive = isScrollBounded,
        topPadding = topPaddingPx,
        bottomPadding = bottomPaddingPx,
        sentenceTopOffset = currentSentenceBounds.topOffset,
        sentenceBottomOffset = currentSentenceBounds.bottomOffset
    )

    val flingBehavior = rememberCustomFlingBehavior(
        sensitivity = settings.scrollSensitivity,
        smoothScroll = settings.smoothScroll,
        reduceMotion = settings.reduceMotion
    )

    // QuickNovel-style auto-scroll: scroll when sentence would go off-screen
    // Position at opposite edge (top->bottom flip, bottom->top flip)
    LaunchedEffect(ensureVisibleIndex, currentSentenceBounds) {
        if (ensureVisibleIndex == null || ensureVisibleIndex < 0) return@LaunchedEffect

        val layoutInfo = listState.layoutInfo
        val targetItem = layoutInfo.visibleItemsInfo.find { it.index == ensureVisibleIndex }

        val viewportTop = layoutInfo.viewportStartOffset + topPaddingPx
        val viewportBottom = layoutInfo.viewportEndOffset - bottomPaddingPx
        val viewportHeight = viewportBottom - viewportTop

        if (targetItem == null) {
            // Item completely off-screen, scroll to bring it into view
            // Use flip behavior: if it was below, put at top; if above, put at bottom
            try {
                // Calculate where to position the item
                val scrollOffset = when (uiState.lastTTSScrollEdge) {
                    TTSScrollEdge.BOTTOM -> {
                        // Sentence went off bottom, put it at top of viewport
                        topPaddingPx
                    }
                    TTSScrollEdge.TOP -> {
                        // Sentence went off top, put it at bottom of viewport
                        // This means we need negative offset so item appears near bottom
                        -(viewportHeight - (currentSentenceBounds.height.toInt().coerceAtLeast(100)))
                    }
                    TTSScrollEdge.NONE -> {
                        // Default: put at upper third of viewport
                        topPaddingPx + (viewportHeight / 4)
                    }
                }

                listState.animateScrollToItem(
                    index = ensureVisibleIndex,
                    scrollOffset = -scrollOffset.coerceAtLeast(0)
                )
            } catch (e: Exception) {
                // Fallback
                try {
                    listState.animateScrollToItem(ensureVisibleIndex)
                } catch (_: Exception) { }
            }
        } else if (currentSentenceBounds.isValid) {
            // Item is visible, but check if SENTENCE is visible
            val sentenceTopInViewport = targetItem.offset + currentSentenceBounds.topOffset.toInt()
            val sentenceBottomInViewport = targetItem.offset + currentSentenceBounds.bottomOffset.toInt()

            when {
                sentenceBottomInViewport > viewportBottom -> {
                    // Sentence is below viewport - scroll to put it at TOP
                    val scrollAmount = sentenceTopInViewport - viewportTop - 20 // Small margin
                    try {
                        listState.animateScrollBy(scrollAmount.toFloat())
                    } catch (e: Exception) { }
                }
                sentenceTopInViewport < viewportTop -> {
                    // Sentence is above viewport - scroll to put it at BOTTOM
                    val targetBottom = viewportBottom - currentSentenceBounds.height.toInt() - 20
                    val scrollAmount = sentenceTopInViewport - targetBottom
                    try {
                        listState.animateScrollBy(scrollAmount.toFloat())
                    } catch (e: Exception) { }
                }
            }
        }

        onEnsureVisibleHandled()
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            val content: @Composable () -> Unit = {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (maxWidth != Dp.Unspecified) {
                                Modifier.widthIn(max = maxWidth)
                            } else {
                                Modifier
                            }
                        )
                        .then(
                            if (isScrollBounded) {
                                Modifier.nestedScroll(ttsBoundedScrollConnection)
                            } else {
                                Modifier
                            }
                        ),
                    contentPadding = PaddingValues(
                        top = topPadding,
                        bottom = bottomPadding
                    ),
                    flingBehavior = flingBehavior,
                    userScrollEnabled = true
                ) {
                    itemsIndexed(
                        items = uiState.displayItems,
                        key = { _, item -> item.itemId },
                        contentType = { _, item -> item::class }
                    ) { index, item ->
                        when (item) {
                            is ReaderDisplayItem.ChapterHeader -> {
                                ChapterHeaderItem(
                                    item = item,
                                    colors = effectiveColors,
                                    fontFamily = fontFamily,
                                    horizontalPadding = horizontalPadding,
                                    largerTouchTargets = settings.largerTouchTargets
                                )
                            }

                            is ReaderDisplayItem.HorizontalRule -> {
                                HorizontalRuleItem(
                                    item = item,
                                    colors = effectiveColors,
                                    horizontalPadding = horizontalPadding
                                )
                            }

                            is ReaderDisplayItem.SceneBreak -> {
                                SceneBreakItem(
                                    item = item,
                                    colors = effectiveColors,
                                    horizontalPadding = horizontalPadding
                                )
                            }

                            is ReaderDisplayItem.AuthorNote -> {
                                val authorNoteDisplayMode = remember {
                                    AuthorNoteDisplayMode.COLLAPSED
                                }

                                AuthorNoteItem(
                                    item = item,
                                    colors = effectiveColors,
                                    displayMode = authorNoteDisplayMode,
                                    fontFamily = fontFamily,
                                    fontSize = settings.fontSize,
                                    horizontalPadding = horizontalPadding,
                                    paragraphSpacing = paragraphSpacing,
                                    primaryColor = MaterialTheme.colorScheme.primary
                                )
                            }

                            is ReaderDisplayItem.Table -> {
                                TableItem(
                                    item = item,
                                    settings = settings,
                                    fontFamily = fontFamily,
                                    colors = effectiveColors,
                                    horizontalPadding = horizontalPadding,
                                    paragraphSpacing = paragraphSpacing
                                )
                            }

                            is ReaderDisplayItem.List -> {
                                ListItem(
                                    item = item,
                                    settings = settings,
                                    fontFamily = fontFamily,
                                    fontWeight = fontWeight,
                                    colors = effectiveColors,
                                    horizontalPadding = horizontalPadding,
                                    paragraphSpacing = paragraphSpacing
                                )
                            }

                            is ReaderDisplayItem.Segment -> {
                                when (item.segment.blockType) {
                                    BlockType.BLOCKQUOTE -> {
                                        BlockquoteSegmentItem(
                                            item = item,
                                            settings = settings,
                                            fontFamily = fontFamily,
                                            fontWeight = fontWeight,
                                            textColor = effectiveColors.text,
                                            horizontalPadding = horizontalPadding,
                                            paragraphSpacing = paragraphSpacing,
                                            colors = effectiveColors,
                                        )
                                    }
                                    BlockType.CODE_BLOCK -> {
                                        CodeBlockSegmentItem(
                                            item = item,
                                            displayIndex = index,
                                            currentSentenceHighlight = uiState.currentSentenceHighlight,
                                            isTTSActive = uiState.isTTSActive,
                                            highlightEnabled = uiState.ttsSettings.highlightSentence,
                                            settings = settings,
                                            textColor = effectiveColors.text,
                                            highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                            horizontalPadding = horizontalPadding,
                                            paragraphSpacing = paragraphSpacing
                                        )
                                    }
                                    BlockType.SYSTEM_MESSAGE -> {
                                        SystemMessageSegmentItem(
                                            item = item,
                                            displayIndex = index,
                                            currentSentenceHighlight = uiState.currentSentenceHighlight,
                                            isTTSActive = uiState.isTTSActive,
                                            highlightEnabled = uiState.ttsSettings.highlightSentence,
                                            settings = settings,
                                            textColor = effectiveColors.text,
                                            highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                            horizontalPadding = horizontalPadding,
                                            paragraphSpacing = paragraphSpacing
                                        )
                                    }
                                    BlockType.NORMAL -> {
                                        SegmentItem(
                                            item = item,
                                            displayIndex = index,
                                            currentSentenceHighlight = uiState.currentSentenceHighlight,
                                            isTTSActive = uiState.isTTSActive,
                                            highlightEnabled = uiState.ttsSettings.highlightSentence,
                                            settings = settings,
                                            fontFamily = fontFamily,
                                            fontWeight = fontWeight,
                                            textAlign = textAlign,
                                            textColor = effectiveColors.text,
                                            highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                            horizontalPadding = horizontalPadding,
                                            paragraphSpacing = paragraphSpacing,
                                            linkColor = effectiveColors.linkColor,
                                            onLinkClick = null,
                                            textHighlights = highlightsBySegment[item.segment.id] ?: emptyList(),
                                            onSentenceBoundsCalculated = onSentenceBoundsUpdated,
                                            onTextSelected = { text, start, end, segId, _, existingId ->
                                                selectionSegmentId = segId
                                                selectionStart = start
                                                selectionEnd = end
                                                if (existingId != null) {
                                                    // Saved highlight → open popup immediately
                                                    val existing = uiState.textHighlights.find { it.id == existingId }
                                                    pendingSelectionText = existing?.text ?: text
                                                }
                                                // New word: handles + floating toolbar; the sheet
                                                // opens only from More / tapping the selection.
                                            },
                                            tempSelectionStart = if (item.segment.id == selectionSegmentId) selectionStart else -1,
                                            tempSelectionEnd = if (item.segment.id == selectionSegmentId) selectionEnd else -1,
                                            onHandleDragStart = if (item.segment.id == selectionSegmentId) {
                                                { pendingSelectionText = null }
                                            } else null,
                                            onHandleDragUpdate = if (item.segment.id == selectionSegmentId) { s, e ->
                                                selectionStart = s
                                                selectionEnd = e
                                            } else null,
                                            onSelectionTapped = if (item.segment.id == selectionSegmentId) { text ->
                                                if (text.isNotBlank()) pendingSelectionText = text
                                            } else null,
                                            hasActiveSelection = selectionSegmentId != null,
                                            onClearSelection = clearSelection,
                                            onQuickCopy = if (item.segment.id == selectionSegmentId) { text ->
                                                clipboard.setText(AnnotatedString(text))
                                                clearSelection()
                                            } else null,
                                            onQuickHighlight = if (item.segment.id == selectionSegmentId) { text ->
                                                onAddHighlight?.invoke(text, DEFAULT_HIGHLIGHT_COLOR)
                                                clearSelection()
                                            } else null
                                        )
                                    }
                                }
                            }

                            is ReaderDisplayItem.Image -> {
                                ChapterImageItem(
                                    item = item,
                                    colors = effectiveColors,
                                    horizontalPadding = horizontalPadding,
                                    baseUrl = uiState.currentChapterUrl,
                                    onImageClick = { url ->
                                        fullScreenImageUrl = url
                                    }
                                )
                            }

                            is ReaderDisplayItem.ChapterDivider -> {
                                ChapterDividerItem(
                                    item = item,
                                    colors = effectiveColors,
                                    infiniteScrollEnabled = uiState.infiniteScrollEnabled,
                                    horizontalPadding = horizontalPadding,
                                    largerTouchTargets = settings.largerTouchTargets,
                                    onPrevious = onPrevious,
                                    onNext = onNext,
                                    onBackToDetails = onBack
                                )
                            }

                            is ReaderDisplayItem.LoadingIndicator -> {
                                LoadingIndicatorItem(colors = effectiveColors)
                            }

                            is ReaderDisplayItem.ErrorIndicator -> {
                                ErrorIndicatorItem(
                                    error = item.error,
                                    colors = colors,
                                    onRetry = { onRetryChapter(item.chapterIndex) }
                                )
                            }
                        }
                    }
                }
            }

            content()

            fullScreenImageUrl?.let { url ->
                ImageViewerDialog(
                    imageUrl = url,
                    onDismiss = { fullScreenImageUrl = null }
                )
            }

            pendingSelectionText?.let { text ->
                val existingHighlight = uiState.textHighlights.find { h -> h.text.trim() == text }
                TextSelectionPopup(
                    selectedText = text,
                    existingHighlight = existingHighlight,
                    onDismiss = {
                        pendingSelectionText = null
                        selectionSegmentId = null
                        selectionStart = -1
                        selectionEnd = -1
                    },
                    onHighlight = { color ->
                        if (existingHighlight != null) {
                            onChangeHighlightColor?.invoke(existingHighlight.id, color)
                        } else {
                            onAddHighlight?.invoke(text, color)
                        }
                    },
                    onRemoveHighlight = {
                        existingHighlight?.let { h -> onRemoveHighlight?.invoke(h.id) }
                    },
                    onUpdateNote = onUpdateNote
                )
            }
        }
    }
}

// =============================================================================
// MAPPING FUNCTIONS
// =============================================================================

private fun mapTextAlign(
    readerTextAlign: ReaderTextAlign,
    readingDirection: ReadingDirection
): TextAlign {
    return when (readerTextAlign) {
        ReaderTextAlign.LEFT -> {
            if (readingDirection == ReadingDirection.RTL) TextAlign.End else TextAlign.Start
        }
        ReaderTextAlign.RIGHT -> {
            if (readingDirection == ReadingDirection.RTL) TextAlign.Start else TextAlign.End
        }
        ReaderTextAlign.CENTER -> TextAlign.Center
        ReaderTextAlign.JUSTIFY -> TextAlign.Justify
    }
}

private fun mapFontWeight(readerFontWeight: com.kmhmubin.kothagolp.domain.model.FontWeight): FontWeight {
    return when (readerFontWeight) {
        com.kmhmubin.kothagolp.domain.model.FontWeight.THIN -> FontWeight.Thin
        com.kmhmubin.kothagolp.domain.model.FontWeight.EXTRA_LIGHT -> FontWeight.ExtraLight
        com.kmhmubin.kothagolp.domain.model.FontWeight.LIGHT -> FontWeight.Light
        com.kmhmubin.kothagolp.domain.model.FontWeight.REGULAR -> FontWeight.Normal
        com.kmhmubin.kothagolp.domain.model.FontWeight.MEDIUM -> FontWeight.Medium
        com.kmhmubin.kothagolp.domain.model.FontWeight.SEMI_BOLD -> FontWeight.SemiBold
        com.kmhmubin.kothagolp.domain.model.FontWeight.BOLD -> FontWeight.Bold
        com.kmhmubin.kothagolp.domain.model.FontWeight.EXTRA_BOLD -> FontWeight.ExtraBold
        com.kmhmubin.kothagolp.domain.model.FontWeight.BLACK -> FontWeight.Black
    }
}

/** Default color for the toolbar's one-tap Highlight action. */
private const val DEFAULT_HIGHLIGHT_COLOR = "#FFD54F"
