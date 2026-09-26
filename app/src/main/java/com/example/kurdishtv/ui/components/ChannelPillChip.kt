package com.example.kurdishtv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.model.CategoryFilter
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.KurdishSunGold

@Composable
fun ChannelPillChip(
    filter: CategoryFilter,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) KurdishSunGold else DarkSurfaceVariant,
        label = "pillBg"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
        label = "pillText"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) KurdishSunGold else DarkCardBorder,
        label = "pillBorder"
    )

    Box(
        modifier = modifier
            .testTag("filter_pill_${filter.name.lowercase()}")
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (filter == CategoryFilter.FAVORITES) {
                Icon(
                    imageVector = KurdishTvIcons.FavoriteFilled,
                    contentDescription = null,
                    tint = if (isSelected) Color.Black else KurdishSunGold,
                    modifier = Modifier.padding(end = 6.dp)
                )
            } else if (filter == CategoryFilter.HD) {
                Icon(
                    imageVector = KurdishTvIcons.Signal,
                    contentDescription = null,
                    tint = if (isSelected) Color.Black else KurdishSunGold,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
            Text(
                text = filter.displayName,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
