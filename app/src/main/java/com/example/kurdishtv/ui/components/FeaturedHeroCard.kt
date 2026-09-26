package com.example.kurdishtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.ui.motion.bouncyClickable
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkCardBorderGlow
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.KurdishAmber
import com.example.ui.theme.KurdishRed
import com.example.ui.theme.KurdishSunGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun FeaturedHeroCard(
    channel: Channel,
    onWatchClick: (Channel) -> Unit,
    onFavoriteToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(M3ExpressiveShapes.AsymmetricHero)
            .border(1.5.dp, DarkCardBorderGlow, M3ExpressiveShapes.AsymmetricHero)
            .bouncyClickable(scaleDown = 0.97f) { onWatchClick(channel) },
        shape = M3ExpressiveShapes.AsymmetricHero,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            DarkSurfaceElevated,
                            DarkSurfaceVariant,
                            DarkSurface
                        ),
                        radius = 800f
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LiveBadge()
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = M3ExpressiveShapes.Pill,
                            color = KurdishAmber.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "FEATURED STREAM",
                                color = KurdishSunGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = DarkSurfaceElevated,
                        modifier = Modifier.size(36.dp)
                    ) {
                        IconButton(onClick = { onFavoriteToggle(channel.id) }) {
                            SvgIcon(
                                resId = if (channel.isFavorite) KurdishTvIcons.FavoriteFilledRes else KurdishTvIcons.FavoriteOutline,
                                contentDescription = "Toggle favorite",
                                tint = if (channel.isFavorite) KurdishRed else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = M3ExpressiveShapes.MediumCard,
                        color = DarkSurfaceElevated,
                        modifier = Modifier
                            .size(68.dp)
                            .border(1.dp, DarkCardBorder, M3ExpressiveShapes.MediumCard)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (!channel.logoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(channel.logoUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = channel.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.padding(8.dp)
                                )
                            } else {
                                SvgIcon(
                                    resId = KurdishTvIcons.Tv,
                                    contentDescription = null,
                                    tint = KurdishSunGold,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = channel.name,
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${channel.category} • ${channel.quality}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onWatchClick(channel) },
                    shape = M3ExpressiveShapes.Pill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KurdishSunGold,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SvgIcon(
                        resId = KurdishTvIcons.PlayRes,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Watch Live Now",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
