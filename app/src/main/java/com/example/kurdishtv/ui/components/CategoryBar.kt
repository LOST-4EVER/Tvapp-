package com.example.kurdishtv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.model.CategoryFilter
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.model.ChannelFilterEngine
import com.example.kurdishtv.ui.motion.LocalReduceMotion
import com.example.kurdishtv.ui.motion.bouncyClickable
import com.example.kurdishtv.ui.motion.rememberMorphingPillShape
import com.example.ui.theme.LocalAppColors

@Composable
fun CategoryBar(
    selectedCategory: CategoryFilter,
    onCategorySelected: (CategoryFilter) -> Unit,
    channels: List<Channel> = emptyList(),
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val reduceMotion = LocalReduceMotion.current

    // Hide tabs that would show nothing.
    //
    // The Sports tab had no channel in the curated catalog (its one stream 404s
    // and was removed), so tapping it produced an empty grid with no explanation.
    // Computing the counts from the live list means a tab disappears when it has
    // no content and reappears as soon as an imported playlist supplies some —
    // which is also why the current selection is preserved if it still matches.
    val counts = remember(channels) {
        if (channels.isEmpty()) {
            // Before the list has loaded, show everything rather than flickering
            // tabs in and out as the merge completes.
            null
        } else {
            CategoryFilter.entries.associateWith { category ->
                ChannelFilterEngine.filter(channels, category, "").size
            }
        }
    }
    val visible = remember(counts) {
        when {
            counts == null -> CategoryFilter.entries
            selectedCategory.let { sel -> counts[sel] ?: 0 } > 0 ->
                // Keep the selected tab even if it is empty, so the selection is
                // never silently changed out from under the user.
                CategoryFilter.entries.filter { counts[it] != 0 || it == selectedCategory }
            else -> CategoryFilter.entries.filter { counts[it] != 0 }
        }
    }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)
    ) {
        items(visible, key = { it.name }) { category ->
            val isSelected = selectedCategory == category

            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) colors.primary else colors.surfaceVariant,
                animationSpec = spring(),
                label = "CategoryPillBackground"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) colors.onPrimary else colors.textPrimary,
                animationSpec = spring(),
                label = "CategoryPillContent"
            )
            val elevation by animateDpAsState(
                targetValue = if (isSelected) 6.dp else 0.dp,
                animationSpec = spring(),
                label = "CategoryPillElevation"
            )

            // The pill's silhouette animates on selection, so the change is
            // legible without relying on colour alone.
            val pillShape = rememberMorphingPillShape(
                selected = isSelected,
                reduceMotion = reduceMotion
            )

            Surface(
                shape = pillShape,
                color = backgroundColor,
                shadowElevation = elevation,
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = if (isSelected) colors.primary else colors.border,
                        shape = pillShape
                    )
                    .bouncyClickable(scaleDown = 0.90f) {
                        onCategorySelected(category)
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    SvgIcon(
                        resId = categoryIcon(category),
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = category.displayName,
                        color = contentColor,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

internal fun categoryIcon(category: CategoryFilter): Int = when (category) {
    CategoryFilter.ALL -> KurdishTvIcons.Tv
    CategoryFilter.NEWS -> KurdishTvIcons.News
    CategoryFilter.KURDISH -> KurdishTvIcons.Globe
    CategoryFilter.GENERAL -> KurdishTvIcons.LiveTv
    CategoryFilter.MUSIC -> KurdishTvIcons.Music
    CategoryFilter.KIDS -> KurdishTvIcons.Kids
    CategoryFilter.SPORT -> KurdishTvIcons.Sports
    CategoryFilter.DOCUMENTARY -> KurdishTvIcons.Tune
    CategoryFilter.QURAN -> KurdishTvIcons.Quran
    CategoryFilter.RELIGIOUS -> KurdishTvIcons.Religious
    CategoryFilter.FAVORITES -> KurdishTvIcons.FavoriteFilledRes
    CategoryFilter.HD -> KurdishTvIcons.Signal
}
