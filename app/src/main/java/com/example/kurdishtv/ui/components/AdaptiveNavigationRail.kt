package com.example.kurdishtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.model.CategoryFilter
import com.example.kurdishtv.ui.motion.bouncyClickable
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.KurdishSunGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AdaptiveNavigationRail(
    selectedCategory: CategoryFilter,
    onCategorySelected: (CategoryFilter) -> Unit,
    onOpenImport: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        containerColor = DarkSurface,
        contentColor = TextPrimary,
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = KurdishSunGold,
                    modifier = Modifier
                        .size(46.dp)
                        .bouncyClickable(onClick = onRefresh)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        SvgIcon(
                            resId = KurdishTvIcons.Tv,
                            contentDescription = "Kurdish TV Live",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Kurdish TV",
                    color = KurdishSunGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        modifier = modifier
            .fillMaxHeight()
            .width(88.dp)
            .border(width = 1.dp, color = DarkCardBorder)
    ) {
        val railCategories = listOf(
            CategoryFilter.ALL,
            CategoryFilter.NEWS,
            CategoryFilter.MUSIC,
            CategoryFilter.KIDS,
            CategoryFilter.FAVORITES,
            CategoryFilter.HD
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            railCategories.forEach { category ->
                val isSelected = selectedCategory == category
                NavigationRailItem(
                    selected = isSelected,
                    onClick = { onCategorySelected(category) },
                    icon = {
                        val iconRes = when (category) {
                            CategoryFilter.ALL -> KurdishTvIcons.Tv
                            CategoryFilter.NEWS -> KurdishTvIcons.News
                            CategoryFilter.MUSIC -> KurdishTvIcons.Music
                            CategoryFilter.KIDS -> KurdishTvIcons.Kids
                            CategoryFilter.FAVORITES -> KurdishTvIcons.FavoriteFilledRes
                            CategoryFilter.HD -> KurdishTvIcons.Signal
                            else -> KurdishTvIcons.Globe
                        }
                        SvgIcon(
                            resId = iconRes,
                            contentDescription = category.displayName,
                            tint = if (isSelected) Color.Black else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = {
                        Text(
                            text = category.displayName.split(" ").first(),
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationRailItemDefaults.colors(
                        indicatorColor = KurdishSunGold,
                        selectedTextColor = KurdishSunGold,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.bouncyClickable(scaleDown = 0.90f) {
                        onCategorySelected(category)
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Surface(
                shape = CircleShape,
                color = DarkSurfaceElevated,
                modifier = Modifier
                    .size(38.dp)
                    .border(1.dp, DarkCardBorder, CircleShape)
                    .bouncyClickable(onClick = onOpenImport)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    SvgIcon(
                        resId = KurdishTvIcons.AddLink,
                        contentDescription = "Import",
                        tint = KurdishSunGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
