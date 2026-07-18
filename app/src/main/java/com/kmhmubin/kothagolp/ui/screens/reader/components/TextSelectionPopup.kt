package com.kmhmubin.kothagolp.ui.screens.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.kmhmubin.kothagolp.ui.screens.reader.model.TextHighlight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL
import java.net.URLEncoder

private val HIGHLIGHT_COLORS = listOf(
    "#FFD54F" to "Yellow",
    "#81C784" to "Green",
    "#64B5F6" to "Blue",
    "#F48FB1" to "Pink",
    "#FFB74D" to "Orange"
)

private sealed class SheetView {
    object Actions : SheetView()
    object NoteEdit : SheetView()
    object HighlightColors : SheetView()
    object DictLoading : SheetView()
    data class DictResult(val word: String, val phonetic: String?, val meanings: List<DictMeaning>) : SheetView()
    data class DictError(val message: String) : SheetView()
}

private data class DictMeaning(val partOfSpeech: String, val definitions: List<String>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextSelectionPopup(
    selectedText: String,
    existingHighlight: TextHighlight? = null,
    /** Open straight into the dictionary lookup instead of the actions view. */
    startInDictionary: Boolean = false,
    /** Open straight into the note editor (used right after creating a highlight). */
    startInNoteEdit: Boolean = false,
    onDismiss: () -> Unit,
    onSelectAll: (() -> Unit)? = null,
    onHighlight: (color: String) -> Unit,
    onRemoveHighlight: () -> Unit,
    onUpdateNote: ((id: Long, note: String?) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var view by remember {
        mutableStateOf<SheetView>(
            when {
                startInDictionary -> SheetView.DictLoading
                startInNoteEdit -> SheetView.NoteEdit
                else -> SheetView.Actions
            }
        )
    }
    var noteText by remember { mutableStateOf(existingHighlight?.userNote ?: "") }

    val dismiss: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    val firstWord = remember(selectedText) { selectedText.trim().split("\\s+".toRegex()).firstOrNull() ?: selectedText }

    fun fetchDictionary(word: String) {
        view = SheetView.DictLoading
        scope.launch(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(word.lowercase().trim(), "UTF-8")
                val conn = URL("https://api.dictionaryapi.dev/api/v2/entries/en/$encoded").openConnection()
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val json = conn.getInputStream().bufferedReader().readText()
                val arr = JSONArray(json)
                val entry = arr.getJSONObject(0)
                val phonetic = runCatching { entry.getString("phonetic") }.getOrNull()?.takeIf { it.isNotBlank() }
                val meaningsArr = entry.getJSONArray("meanings")
                val meanings = buildList {
                    for (i in 0 until meaningsArr.length()) {
                        val m = meaningsArr.getJSONObject(i)
                        val pos = m.getString("partOfSpeech")
                        val defs = m.getJSONArray("definitions")
                        add(DictMeaning(pos, buildList {
                            for (j in 0 until minOf(defs.length(), 3)) {
                                add(defs.getJSONObject(j).getString("definition"))
                            }
                        }))
                    }
                }
                withContext(Dispatchers.Main) { view = SheetView.DictResult(word, phonetic, meanings) }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { view = SheetView.DictError("No definition found for \"$word\"") }
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (startInDictionary) fetchDictionary(firstWord)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            when (val v = view) {
                is SheetView.Actions -> ActionsView(
                    selectedText = selectedText,
                    existingHighlight = existingHighlight,
                    onCopy = { clipboard.setText(AnnotatedString(selectedText)); dismiss() },
                    onSelectAll = onSelectAll?.let { sa -> { sa() } },
                    onDictionary = { fetchDictionary(firstWord) },
                    onShowColors = { view = SheetView.HighlightColors },
                    onRemoveHighlight = { onRemoveHighlight(); dismiss() },
                    onEditNote = { view = SheetView.NoteEdit }
                )

                is SheetView.HighlightColors -> HighlightColorsView(
                    existingHighlight = existingHighlight,
                    onBack = { view = SheetView.Actions },
                    onColorSelected = { color -> onHighlight(color); dismiss() }
                )

                is SheetView.NoteEdit -> NoteEditView(
                    label = selectedText.take(60).let { if (selectedText.length > 60) "$it…" else it },
                    noteText = noteText,
                    onNoteChange = { noteText = it },
                    onBack = { view = SheetView.Actions },
                    onSave = {
                        val id = existingHighlight?.id ?: return@NoteEditView
                        onUpdateNote?.invoke(id, noteText.ifBlank { null })
                        dismiss()
                    }
                )

                is SheetView.DictLoading -> {
                    DictHeader(word = firstWord, onBack = { view = SheetView.Actions })
                    Spacer(modifier = Modifier.height(32.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }

                is SheetView.DictResult -> {
                    DictHeader(word = v.word, onBack = { view = SheetView.Actions })
                    v.phonetic?.let { phonetic ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = phonetic,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = FontStyle.Italic
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        v.meanings.forEach { meaning ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = meaning.partOfSpeech,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                meaning.definitions.forEachIndexed { i, def ->
                                    Text(
                                        text = "${i + 1}. $def",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                is SheetView.DictError -> {
                    DictHeader(word = firstWord, onBack = { view = SheetView.Actions })
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = v.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ActionsView(
    selectedText: String,
    existingHighlight: TextHighlight?,
    onCopy: () -> Unit,
    onSelectAll: (() -> Unit)? = null,
    onDictionary: () -> Unit,
    onShowColors: () -> Unit,
    onRemoveHighlight: () -> Unit,
    onEditNote: () -> Unit
) {
    Text(
        text = "\"$selectedText\"",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurface
    )

    if (!existingHighlight?.userNote.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = existingHighlight!!.userNote!!,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Row 1: Copy | Dictionary
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(onClick = onCopy, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Copy")
        }
        FilledTonalButton(onClick = onDictionary, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Dictionary")
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Row 2: Highlight | (Edit Note or Select All)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(onClick = onShowColors, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.Colorize, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (existingHighlight != null) "Change Color" else "Highlight")
        }
        if (existingHighlight != null) {
            FilledTonalButton(onClick = onEditNote, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (existingHighlight.userNote.isNullOrBlank()) "Add Note" else "Edit Note")
            }
        } else if (onSelectAll != null) {
            FilledTonalButton(onClick = onSelectAll, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.SelectAll, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Select All")
            }
        }
    }

    if (existingHighlight != null) {
        Spacer(modifier = Modifier.height(8.dp))
        FilledTonalButton(
            onClick = onRemoveHighlight,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Remove Highlight")
        }
    }
}

@Composable
private fun NoteEditView(
    label: String,
    noteText: String,
    onNoteChange: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "\"$label\"",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = noteText,
        onValueChange = onNoteChange,
        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
        placeholder = { Text("Add a note…", style = MaterialTheme.typography.bodyMedium) },
        textStyle = MaterialTheme.typography.bodyMedium,
        minLines = 3,
        maxLines = 6
    )

    Spacer(modifier = Modifier.height(12.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onBack) { Text("Cancel") }
        Spacer(modifier = Modifier.width(8.dp))
        FilledTonalButton(onClick = onSave) { Text("Save") }
    }
}

@Composable
private fun HighlightColorRow(onColorSelected: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        HIGHLIGHT_COLORS.forEach { (hex, _) ->
            val color = try { Color(hex.toColorInt()) } catch (_: Exception) { Color(0xFFFFD54F) }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                    .clickable { onColorSelected(hex) }
            )
        }
    }
}

@Composable
private fun HighlightColorsView(
    existingHighlight: TextHighlight?,
    onBack: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (existingHighlight != null) "Change Color" else "Highlight",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    HighlightColorRow(onColorSelected = onColorSelected)
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun DictHeader(word: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = word, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
