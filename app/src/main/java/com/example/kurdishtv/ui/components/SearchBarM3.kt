package com.example.kurdishtv.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
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
    val focusManager = LocalFocusManager.current

    // The field rests as a pill and squares off as it takes focus. A search box that
    // changes outline is far easier to notice on a television than one that only
    // recolours its border, and the field holds a placeholder string, so this stays
    // on the corner scale rather than reaching for a lobed silhouette.
    val focused = remember { mutableStateOf(false) }
    val fieldShape = rememberMorphingCorners(
        // Sized off the field's own height rather than picked. A Material single-line
        // text field is 56dp, so the longest legal radius is 28dp -- exactly the value
        // this used, which left the four corner arcs meeting precisely on the centre
        // line and rendered the resting pill slightly pinched at all four corners.
        // 26dp is the largest radius that stays strictly inside it.
        rest = CornerScale.uniform(26.dp),
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
        // The keyboard's own Search/Enter key. Without an IME action the field gets
        // the default one, which on most IMEs is a bare newline that does nothing —
        // so the one control on the screen that submits a search had no way to be
        // submitted. `Done` is also what dismisses the keyboard, which is the
        // behaviour a viewer expects from a single-line field once they have seen
        // the results narrow down.
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            // Drop the focus, which is what closes the IME. Setting the local
            // `focused` flag alone would square the field off while the keyboard
            // stayed on screen — the exact opposite of what was asked for.
            onSearch = { focusManager.clearFocus() }
        ),
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
            // Clear focus when the keyboard's Search key is pressed. Handled through
            // the same state the shape animation reads, so the field visibly squares
            // off again as the keyboard leaves rather than staying focused behind it.
            .onFocusChanged { focused.value = it.isFocused }
    )
}
