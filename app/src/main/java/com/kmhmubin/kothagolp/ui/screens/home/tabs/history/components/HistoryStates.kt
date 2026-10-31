package com.kmhmubin.kothagolp.ui.screens.home.tabs.history.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kmhmubin.kothagolp.ui.screens.home.tabs.history.HistoryCopy

/** Placeholder card shaped like the real list, so nothing jumps when rows arrive. */
@Composable
fun HistorySkeleton(modifier: Modifier = Modifier) {
    val sk = MaterialTheme.colorScheme.surfaceContainerHigh
    val bar = RoundedCornerShape(8.dp)
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp).size(60.dp, 12.dp).clip(bar).background(sk))
        Column(
            Modifier.padding(horizontal = 12.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(vertical = 4.dp)
        ) {
            listOf(0.7f to 0.5f, 0.6f to 0.45f, 0.75f to 0.4f).forEach { (a, b) ->
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp, 64.dp).clip(RoundedCornerShape(10.dp)).background(sk))
                    Column(Modifier.weight(1f)) {
                        Box(Modifier.fillMaxWidth(a).height(14.dp).clip(bar).background(sk))
                        Box(Modifier.padding(top = 8.dp).fillMaxWidth(b).height(11.dp).clip(bar).background(sk))
                        Box(Modifier.padding(top = 8.dp).width(60.dp).height(10.dp).clip(bar).background(sk))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryEmpty(onFindNovels: () -> Unit, modifier: Modifier = Modifier) = CenterMessage(
    shape = MaterialShapes.Cookie9Sided.toShape(),
    shapeColor = MaterialTheme.colorScheme.primaryContainer,
    iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
    icon = Icons.Rounded.History,
    title = HistoryCopy.EMPTY_TITLE,
    body = HistoryCopy.EMPTY_BODY,
    modifier = modifier,
) { Button(onClick = onFindNovels) { Text(HistoryCopy.FIND_NOVELS) } }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryNoMatch(query: String, onSearchAllSources: () -> Unit, modifier: Modifier = Modifier) = CenterMessage(
    shape = MaterialShapes.Sunny.toShape(),
    shapeColor = MaterialTheme.colorScheme.secondaryContainer,
    iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
    icon = Icons.Rounded.Search,
    title = HistoryCopy.noMatch(query),
    body = HistoryCopy.NO_MATCH_BODY,
    modifier = modifier,
) { FilledTonalButton(onClick = onSearchAllSources) { Text(HistoryCopy.SEARCH_ALL_SOURCES) } }

@Composable
private fun CenterMessage(
    shape: Shape,
    shapeColor: Color,
    iconColor: Color,
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier,
    action: @Composable () -> Unit,
) {
    // The shape gently scales in on appear.
    val scale = remember { Animatable(0.6f) }
    val spec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(Unit) { scale.animateTo(1f, spec) }
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 50.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(96.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value }.clip(shape).background(shapeColor),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = iconColor, modifier = Modifier.size(36.dp)) }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 18.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
        )
        action()
    }
}

/** The only action that asks first: it can't be undone. Cancel is the safe default on the left. */
@Composable
fun HistoryClearDialog(count: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.DeleteSweep, null, tint = colors.error) },
        title = { Text(HistoryCopy.CLEAR_TITLE) },
        text = { Text(HistoryCopy.clearBody(count)) },
        dismissButton = { TextButton(onClick = onDismiss) { Text(HistoryCopy.CANCEL) } },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = colors.errorContainer, contentColor = colors.onErrorContainer),
            ) { Text(HistoryCopy.CLEAR_CONFIRM) }
        },
    )
}
