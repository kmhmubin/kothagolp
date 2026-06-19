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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import com.kmhmubin.kothagolp.ui.screens.reader.model.WordSelection
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

private sealed class DictView {
    object Actions : DictView()
    object Loading : DictView()
    data class Result(
        val word: String,
        val phonetic: String?,
        val meanings: List<DictMeaning>
    ) : DictView()
    data class Error(val message: String) : DictView()
}

private data class DictMeaning(
    val partOfSpeech: String,
    val definitions: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextSelectionPopup(
    selection: WordSelection,
    onDismiss: () -> Unit,
    onHighlight: (color: String) -> Unit,
    onRemoveHighlight: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var dictView by remember { mutableStateOf<DictView>(DictView.Actions) }

    val dismiss: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    fun fetchDictionary(word: String) {
        dictView = DictView.Loading
        scope.launch(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(word.lowercase().trim(), "UTF-8")
                val conn = URL("https://api.dictionaryapi.dev/api/v2/entries/en/$encoded")
                    .openConnection()
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val json = conn.getInputStream().bufferedReader().readText()
                val arr = JSONArray(json)
                val entry = arr.getJSONObject(0)
                val phonetic = runCatching { entry.getString("phonetic") }.getOrNull()
                    ?.takeIf { it.isNotBlank() }
                val meaningsArr = entry.getJSONArray("meanings")
                val meanings = buildList {
                    for (i in 0 until meaningsArr.length()) {
                        val m = meaningsArr.getJSONObject(i)
                        val pos = m.getString("partOfSpeech")
                        val defs = m.getJSONArray("definitions")
                        val defList = buildList {
                            for (j in 0 until minOf(defs.length(), 3)) {
                                add(defs.getJSONObject(j).getString("definition"))
                            }
                        }
                        add(DictMeaning(pos, defList))
                    }
                }
                withContext(Dispatchers.Main) {
                    dictView = DictView.Result(word, phonetic, meanings)
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    dictView = DictView.Error("No definition found for \"$word\"")
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            when (val view = dictView) {
                is DictView.Actions -> {
                    Text(
                        text = "\"${selection.word}\"",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(selection.word))
                                dismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Outlined.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy")
                        }

                        FilledTonalButton(
                            onClick = { fetchDictionary(selection.word) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dictionary")
                        }
                    }

                    if (selection.existingHighlightId != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = {
                                onRemoveHighlight()
                                dismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Remove Highlight")
                        }
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Highlight",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HIGHLIGHT_COLORS.forEach { (hex, _) ->
                                val color = try {
                                    Color(hex.toColorInt())
                                } catch (_: Exception) {
                                    Color(0xFFFFD54F)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            2.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            CircleShape
                                        )
                                        .clickable {
                                            onHighlight(hex)
                                            dismiss()
                                        }
                                )
                            }
                        }
                    }
                }

                is DictView.Loading -> {
                    DictHeader(word = selection.word, onBack = { dictView = DictView.Actions })
                    Spacer(modifier = Modifier.height(32.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }

                is DictView.Result -> {
                    DictHeader(word = view.word, onBack = { dictView = DictView.Actions })
                    view.phonetic?.let { phonetic ->
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
                        modifier = Modifier
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        view.meanings.forEach { meaning ->
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

                is DictView.Error -> {
                    DictHeader(word = selection.word, onBack = { dictView = DictView.Actions })
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = view.message,
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
private fun DictHeader(word: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = word,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
