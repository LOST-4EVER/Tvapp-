package com.example.kurdishtv.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.ui.motion.CornerScale
import com.example.kurdishtv.ui.motion.ExpressiveMotion
import com.example.kurdishtv.ui.motion.rememberMorphingCorners
import com.example.ui.theme.LocalAppColors

@Composable
fun SearchBarM3(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    // The field rests as a pill and squares off as it takes focus. A search box that
    // changes outline is far easier to notice on a television than one that only
    // recolours its border, and the field holds a placeholder string, so this stays
    // on the corner scale rather than reaching for a lobed silhouette.
    val focused = remember { mutableStateOf(false) }
    val fieldShape = rememberMorphingCorners(
        // 28dp is the pill for a 56dp-tall single-line field; 20dp squares it off
        // without the corners eating into the placeholder text.
        rest = CornerScale.uniform(28.dp),
        active = CornerScale.uniform(18.dp),
        isActive = focused.value,
        spec = ExpressiveMotion.spatialDefault
    )

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = "Search Kurdish channels…",
                color = colors.textTertiary,
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            SvgIcon(
                resId = KurdishTvIcons.Search,
                contentDescription = "Search channels",
                tint = colors.primary,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    SvgIcon(
                        resId = KurdishTvIcons.Close,
                        contentDescription = "Clear search",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        singleLine = true,
        shape = fieldShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceElevated,
            unfocusedContainerColor = colors.surfaceElevated,
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.border,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            cursorColor = colors.primary
        ),
        modifier = modifier
            .testTag("search_input")
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .onFocusChanged { focused.value = it.isFocused }
    )
}
