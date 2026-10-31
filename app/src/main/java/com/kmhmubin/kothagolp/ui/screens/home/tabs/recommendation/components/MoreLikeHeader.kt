package com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.ForYouCopy

/** "More like [Novel ▾]": tonal chip; the name ellipsizes, the ▾ always stays. No ▾/menu with ≤ 1 option. */
@Composable
fun MoreLikeHeader(
    source: Novel,
    options: List<Novel>,
    onSelect: (Novel) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    val switchable = options.size > 1
    Row(
        modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 30.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = ForYouCopy.MORE_LIKE,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Box(Modifier.padding(start = 8.dp).weight(1f, fill = false)) {
            Surface(
                onClick = { open = true },
                enabled = switchable,
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Row(
                    Modifier.padding(start = 10.dp, end = if (switchable) 4.dp else 10.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = source.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (switchable) Icon(Icons.Rounded.ArrowDropDown, null, Modifier.size(24.dp))
                }
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { novel ->
                    DropdownMenuItem(
                        text = { Text(novel.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = { open = false; onSelect(novel) }
                    )
                }
            }
        }
    }
}
