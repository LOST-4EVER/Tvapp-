package com.example.kurdishtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.kurdishtv.ui.motion.ShapeMorph
import com.example.kurdishtv.ui.motion.shimmerEffect
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.LocalAppColors

/**
 * High-performance, battery-efficient skeleton loading state for the channel grid.
 *
 * Rendered when the channel list is initially loading or undergoing a full sync.
 * Replaces jarring blank flashes with clean, responsive placeholders.
 */
@Composable
fun ChannelSkeletonGrid(
    minCellSize: Dp,
    showHero: Boolean = true,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = minCellSize),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        if (showHero && !compact) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                HeroSkeletonCard()
            }
        }

        items(12) {
            ChannelCardSkeleton(compact = compact)
        }
    }
}

@Composable
fun HeroSkeletonCard(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current

    Surface(
        shape = M3ExpressiveShapes.AsymmetricHero,
        color = colors.surface,
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .border(1.dp, colors.border, M3ExpressiveShapes.AsymmetricHero)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(M3ExpressiveShapes.LogoTile)
                    .shimmerEffect(M3ExpressiveShapes.LogoTile)
            )

            Spacer(modifier = Modifier.width(20.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(18.dp)
                        .clip(M3ExpressiveShapes.BadgePill)
                        .shimmerEffect(M3ExpressiveShapes.BadgePill)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(24.dp)
                        .clip(M3ExpressiveShapes.Pill)
                        .shimmerEffect(M3ExpressiveShapes.Pill)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.40f)
                        .height(14.dp)
                        .clip(M3ExpressiveShapes.Pill)
                        .shimmerEffect(M3ExpressiveShapes.Pill)
                )
            }
        }
    }
}

@Composable
fun ChannelCardSkeleton(
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    Surface(
        shape = M3ExpressiveShapes.MediumCard,
        color = colors.surface,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, colors.border, M3ExpressiveShapes.MediumCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 8.dp else 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(M3ExpressiveShapes.SmallCard)
                    .shimmerEffect(M3ExpressiveShapes.SmallCard)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(if (compact) 12.dp else 16.dp)
                    .clip(M3ExpressiveShapes.Pill)
                    .shimmerEffect(M3ExpressiveShapes.Pill)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(10.dp)
                        .clip(M3ExpressiveShapes.Pill)
                        .shimmerEffect(M3ExpressiveShapes.Pill)
                )

                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(10.dp)
                        .clip(M3ExpressiveShapes.Pill)
                        .shimmerEffect(M3ExpressiveShapes.Pill)
                )
            }
        }
    }
}
