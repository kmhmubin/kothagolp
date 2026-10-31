package com.kmhmubin.kothagolp.ui.screens.home.tabs.recommendation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kmhmubin.kothagolp.recommendation.TagNormalizer
import com.kmhmubin.kothagolp.recommendation.TagNormalizer.TagCategory

private val GenrePolygons = listOf(
    MaterialShapes.Cookie9Sided, MaterialShapes.Sunny, MaterialShapes.Clover4Leaf,
    MaterialShapes.Pill, MaterialShapes.Arch, MaterialShapes.Gem
)

/** "Browse by genre": 2-row grid of 70dp Material shapes in cycling tonal colours, label below. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GenreShapes(
    tags: List<TagCategory>,
    onClick: (TagCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val tones = listOf(
        cs.primaryContainer to cs.onPrimaryContainer,
        cs.tertiaryContainer to cs.onTertiaryContainer,
        cs.secondaryContainer to cs.onSecondaryContainer
    )
    val shapes = GenrePolygons.map { it.toShape() }
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        modifier = modifier.height(204.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(tags) { i, tag ->
            val name = TagNormalizer.getDisplayName(tag)
            val (bg, fg) = tones[i % tones.size]
            Column(
                Modifier.width(80.dp).clickable { onClick(tag) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    Modifier.size(70.dp).clip(shapes[i % shapes.size]).background(bg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(name.take(1), style = MaterialTheme.typography.headlineSmall, color = fg, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
