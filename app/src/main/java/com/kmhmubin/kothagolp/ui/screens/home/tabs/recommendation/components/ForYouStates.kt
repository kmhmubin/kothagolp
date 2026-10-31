package com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.ForYouCopy
import com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.SeedingProgress

private val SkeletonShape = RoundedCornerShape(28.dp)

/** Wavy loader + skeletons shaped like the carousel and a row. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ForYouLoading(modifier: Modifier = Modifier) {
    val sk = MaterialTheme.colorScheme.surfaceContainerLow
    Column(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(top = 30.dp, bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            LoadingIndicator(Modifier.size(48.dp))
            Text(
                ForYouCopy.LOADING_TITLE,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.width(200.dp).height(150.dp).clip(SkeletonShape).background(sk))
            Box(Modifier.width(74.dp).height(150.dp).clip(SkeletonShape).background(sk))
            Box(Modifier.width(24.dp).height(150.dp).clip(SkeletonShape).background(sk))
        }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) { Box(Modifier.width(106.dp).height(156.dp).clip(CoverShape).background(sk)) }
        }
    }
}

/** First-run discovery ("seeding"): wavy loader, plain words, progress across sources. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ForYouFirstLoad(progress: SeedingProgress?, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LoadingIndicator(Modifier.size(72.dp))
        Text(ForYouCopy.LOADING_TITLE, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        progress?.let { p ->
            Text(
                ForYouCopy.lookingThrough(p.currentProvider),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            LinearWavyProgressIndicator(
                progress = { p.currentIndex.toFloat() / p.totalProviders.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(
            ForYouCopy.LOADING_BODY,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Empty library, nothing to show yet. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ForYouFirstTime(onFindNovels: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(96.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(36.dp))
        }
        Text(
            ForYouCopy.EMPTY_TITLE,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 18.dp)
        )
        Text(
            ForYouCopy.EMPTY_BODY,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )
        Button(onClick = onFindNovels, modifier = Modifier.padding(top = 16.dp)) { Text(ForYouCopy.EMPTY_ACTION) }
    }
}

/** Quiet card used by the AI row when it has nothing to show: off, loading or error. */
@Composable
fun AiNoticeRow(text: String, action: String?, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(Modifier.padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(vertical = 10.dp))
            if (action != null) TextButton(onClick = onAction) { Text(action, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
fun AiOffRow(onTurnOn: () -> Unit, modifier: Modifier = Modifier) =
    AiNoticeRow(ForYouCopy.AI_OFF, ForYouCopy.AI_TURN_ON, onTurnOn, modifier)
