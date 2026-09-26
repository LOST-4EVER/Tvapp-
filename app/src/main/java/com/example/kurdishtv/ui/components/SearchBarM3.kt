package com.example.kurdishtv.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.KurdishSunGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SearchBarM3(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = "Search Kurdish channels (Rudaw, KurdSat...)",
                color = TextSecondary,
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            SvgIcon(
                resId = KurdishTvIcons.Search,
                contentDescription = "Search channels",
                tint = KurdishSunGold,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    SvgIcon(
                        resId = KurdishTvIcons.Close,
                        contentDescription = "Clear search",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        singleLine = true,
        shape = M3ExpressiveShapes.Pill,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DarkSurfaceElevated,
            unfocusedContainerColor = DarkSurfaceElevated,
            focusedBorderColor = KurdishSunGold,
            unfocusedBorderColor = DarkCardBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = KurdishSunGold
        ),
        modifier = modifier
            .testTag("search_input")
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    )
}
