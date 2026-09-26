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
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
fun AdaptiveNavigationRail(
    selectedCategory: CategoryFilter,
    onCategorySelected: (CategoryFilter) -> Unit,
    onOpenImport: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    NavigationRail(
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp, bottom = 10.dp)
            ) {
                Surface(
                    shape = M3ExpressiveShapes.Sunny,
                    color = colors.primary,
                    modifier = Modifier
                        .size(48.dp)
                        .bouncyClickable { onRefresh() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        SvgIcon(
                            resId = KurdishTvIcons.Tv,
                            contentDescription = "Kurdish TV Live",
                            tint = colors.onPrimary,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Kurdish",
                    color = colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "TV",
                    color = colors.textSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        modifier = modifier
            .fillMaxHeight()
            .width(92.dp)
            .background(colors.surface)
            .border(width = 1.dp, color = colors.border)
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
            modifier = Modifier
                .fillMaxHeight()
                .padding(bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            railCategories.forEach { category ->
                val isSelected = selectedCategory == category
                NavigationRailItem(
                    selected = isSelected,
                    onClick = { onCategorySelected(category) },
                    icon = {
                        SvgIcon(
                            resId = categoryIcon(category),
                            contentDescription = category.displayName,
                            tint = if (isSelected) colors.onPrimary else colors.textSecondary,
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
                        indicatorColor = colors.primary,
                        selectedIconColor = colors.onPrimary,
                        selectedTextColor = colors.primary,
                        unselectedIconColor = colors.textSecondary,
                        unselectedTextColor = colors.textSecondary
                    )
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            RailActionButton(
                iconRes = KurdishTvIcons.Settings,
                description = "Settings",
                onClick = onOpenSettings
            )
            Spacer(modifier = Modifier.height(10.dp))
            RailActionButton(
                iconRes = KurdishTvIcons.AddLink,
                description = "Import playlist",
                onClick = onOpenImport
            )
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun RailActionButton(
    iconRes: Int,
    description: String,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    Surface(
        shape = M3ExpressiveShapes.Burst,
        color = colors.surfaceElevated,
        modifier = Modifier
            .size(40.dp)
            .border(1.dp, colors.border, M3ExpressiveShapes.Burst)
            .bouncyClickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            SvgIcon(
                resId = iconRes,
                contentDescription = description,
                tint = colors.primary,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}
