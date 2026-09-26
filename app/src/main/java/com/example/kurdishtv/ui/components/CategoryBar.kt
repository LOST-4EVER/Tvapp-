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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.model.CategoryFilter
import com.example.kurdishtv.ui.motion.bouncyClickable
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.LocalAppColors

@Composable
fun CategoryBar(
    selectedCategory: CategoryFilter,
    onCategorySelected: (CategoryFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)
    ) {
        items(CategoryFilter.entries, key = { it.name }) { category ->
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

            Surface(
                shape = M3ExpressiveShapes.Pill,
                color = backgroundColor,
                shadowElevation = elevation,
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = if (isSelected) colors.primary else colors.border,
                        shape = M3ExpressiveShapes.Pill
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
    CategoryFilter.QURAN -> KurdishTvIcons.Globe
    CategoryFilter.FAVORITES -> KurdishTvIcons.FavoriteFilledRes
    CategoryFilter.HD -> KurdishTvIcons.Signal
}
