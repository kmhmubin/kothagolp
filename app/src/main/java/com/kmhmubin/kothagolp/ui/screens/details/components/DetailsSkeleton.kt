package com.kmhmubin.kothagolp.ui.screens.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.kmhmubin.kothagolp.ui.components.SkeletonBox

/**
 * Placeholder shaped like the loaded details screen (header, actions, stats,
 * synopsis, chapter rows) so slow sources don't show a blank page.
 */
@Composable
fun DetailsSkeleton(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(top = 12.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        Spacer(Modifier.height(20.dp))

        Row {
            SkeletonBox(Modifier.size(width = 130.dp, height = 195.dp), RoundedCornerShape(12.dp))
            Spacer(Modifier.width(16.dp))
            Column(
                modifier = Modifier.height(195.dp), // match cover so the weight spacer is bounded
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SkeletonBox(Modifier.fillMaxWidth().height(24.dp))
                SkeletonBox(Modifier.fillMaxWidth(0.7f).height(24.dp))
                Spacer(Modifier.height(4.dp))
                SkeletonBox(Modifier.width(120.dp).height(18.dp))
                SkeletonBox(Modifier.width(90.dp).height(26.dp), RoundedCornerShape(13.dp))
                Spacer(Modifier.weight(1f))
                SkeletonBox(Modifier.fillMaxWidth().height(48.dp), RoundedCornerShape(16.dp))
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBox(Modifier.weight(1f).height(52.dp), RoundedCornerShape(16.dp))
            SkeletonBox(Modifier.weight(1f).height(52.dp), RoundedCornerShape(16.dp))
        }
        Spacer(Modifier.height(12.dp))
        SkeletonBox(Modifier.fillMaxWidth().height(44.dp), RoundedCornerShape(16.dp))

        Spacer(Modifier.height(16.dp))
        SkeletonBox(Modifier.fillMaxWidth().height(104.dp), RoundedCornerShape(20.dp))

        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonBox(Modifier.width(90.dp).height(22.dp))
            SkeletonBox(Modifier.fillMaxWidth().height(14.dp))
            SkeletonBox(Modifier.fillMaxWidth().height(14.dp))
            SkeletonBox(Modifier.fillMaxWidth(0.6f).height(14.dp))
        }

        Spacer(Modifier.height(20.dp))
        repeat(4) {
            Box(Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.CenterStart) {
                SkeletonBox(Modifier.fillMaxWidth(0.85f).height(18.dp))
            }
        }
    }
}
