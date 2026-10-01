package com.example.kurdishtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.model.CategoryFilter
import com.example.kurdishtv.ui.motion.CornerScale
import com.example.kurdishtv.ui.motion.ExpressiveMotion
import com.example.kurdishtv.ui.motion.ShapeMorph
import com.example.kurdishtv.ui.motion.staticCornerShape
import com.example.kurdishtv.ui.motion.tvClickable
import com.example.kurdishtv.ui.motion.expressiveFocusRing
import com.example.kurdishtv.ui.theme.M3ExpressivePolygons
import com.example.kurdishtv.ui.theme.M3ExpressiveShapes
import com.example.ui.theme.LocalAppColors

/**
 * The wide-screen navigation rail.
 *
 * The rail is the one persistent piece of chrome on a tablet, so its selection state
 * has to be unmistakable from across a room. Material's own `NavigationRailItem` draws
 * its indicator as a fixed pill, and a fixed pill is exactly the kind of static signal
 * that disappears in peripheral vision — so the items here are built directly and the
 * indicator behind the selected glyph is an Expressive shape that *changes*.
 *
 * Selecting a destination turns the pill into a very slightly lobed sun and swings it
 * a quarter turn on the way. The glyph sits on top of the indicator rather than inside
 * it, which is what makes an organic silhouette safe here: the icon can never be
 * pushed off-centre by a bumpy outline, no matter how decorative the shape gets.
 *
 * A one-shot spring rather than a permanent rotation is a deliberate choice. A rail
 * with six destinations would need six continuously running animations to spin them
 * all, every one of them invalidating a recomposition on each frame. The morph happens
 * once, on selection, and the focus ring — which is the one element the user is
 * actually looking for — is where the continuous motion went instead.
 *
 * Each item is focusable, so the rail is fully operable by D-pad, and the semantics
 * are set explicitly to replace what `NavigationRailItem` provided.
 */
@Composable
fun AdaptiveNavigationRail(
    selectedCategory: CategoryFilter,
    onCategorySelected: (CategoryFilter) -> Unit,
    /**
     * The same list the category chips are showing.
     *
     * The rail used to carry its own hard-coded subset — All, News, Music, Kids,
     * Favourites, HD — and that is a list that was wrong twice over. It offered
     * Music and Kids on a playlist that has neither (the chips had already been fixed
     * to hide empty tabs, so the two rows of navigation disagreed with each other and
     * with the data), and it left out Kurdish Culture, General, Sports, Documentary,
     * Quran and Religious — six of the twelve ways of browsing, unreachable from the
     * rail on exactly the wide screens where the rail exists.
     *
     * Taking the list from the same computation removes the class of bug rather than
     * this instance of it.
     */
    visibleCategories: List<CategoryFilter>,
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
                    // Rounded square, matching the logo tiles in the grid. The 8-lobed
                    // Sunny silhouette had no flat area to centre the glyph on, so the
                    // mark drifted off-centre inside its own tile.
                    shape = M3ExpressiveShapes.MediumCard,
                    color = colors.primary,
                    modifier = Modifier
                        .size(48.dp)
                        .tvClickable { onRefresh() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                    SvgIcon(
                        resId = KurdishTvIcons.Tv,
                        // "Refresh channels", not the app's name. This tile is a
                        // button — it runs [onRefresh] — and it announced itself as
                        // the app's title, so a screen reader read out the name of
                        // the app rather than the name of the thing you are about to
                        // do. The same correction is applied to the header tile.
                        contentDescription = "Refresh channels",
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
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // The categories scroll; the actions below them do not.
            //
            // The rail used to be one fixed-height column with a `Spacer(weight(1f))`
            // pushing Settings and Import to the bottom. Once the rail was given the
            // full twelve categories that stopped being safe: on a short window the
            // content is taller than the rail, and the list simply clipped — the lower
            // categories became unreachable, and so did the two buttons pinned below
            // them, with no way to scroll to any of it.
            //
            // Splitting it into a scrolling region and a fixed footer solves that
            // without relying on a `weight` inside a scrollable column, where the
            // spacer's size is not defined and collapses unpredictably when the
            // content overflows.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // `key` around each item, which `forEach` without it does not give.
                //
                // This is a plain `Column`, not a lazy list, so Compose matches the
                // items in it *by position*. `RailCategoryItem` remembers an
                // `interactionSource`, and `remember` inside a position-matched slot
                // means it belongs to the slot rather than to the category. So the
                // moment the visible
                // set changed shape — which it does as soon as the first catalogue
                // arrives and `CategoryBar` stops offering twelve tabs and starts
                // offering eight — every item below the first removal inherited the
                // interaction source of whatever used to be there.
                //
                // A stale `MutableInteractionSource` still reports the focus state it
                // was left in, so a destination that had never been focused could be
                // drawn with a focus ring, and the ring could be reading the *wrong*
                // source entirely: the tap target and the highlight disagreeing, which
                // is the one failure a D-pad UI cannot have.
                visibleCategories.forEach { category ->
                    key(category) {
                        RailCategoryItem(
                        category = category,
                            isSelected = selectedCategory == category,
                            onClick = { onCategorySelected(category) }
                        )
                    }
                }
            }

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

/** One rail destination: a selection indicator, a glyph, and a label. */
@Composable
private fun RailCategoryItem(
    category: CategoryFilter,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    // The click keeps the focus target; the ring observes it. See `expressiveFocusRing`.
    //
    // Keyed on the category, and with no separate `isFocused` mirror of it: the item
    // draws its state from `isSelected` alone, so a `mutableStateOf` fed by the
    // ring's `onFocusChanged` and read by nothing invalidated this item's composition
    // on every arrow press along the rail, to produce a picture identical to the one
    // already on screen.
    val focusSource = remember(category) { MutableInteractionSource() }

    // The indicator is a fixed 60x34dp box, so its longest safe radius is 17dp --
    // half the short side. A 20dp "active" radius made the four corners overlap and
    // the selection pill render as a lopsided blob. Sized off the box instead.
    val indicatorShape: Shape = staticCornerShape(
        rest = CornerScale.uniform(ShapeMorph.cornerRadius(34.dp, 0.40f)),
        active = CornerScale.uniform(ShapeMorph.cornerRadius(34.dp, 0.40f)),
        isActive = isSelected
    )
    // No `clip` on this Column. It used to clip to a 26dp rounded square, which cut
    // the focus ring in half: the ring is drawn *outset* from the element, and a clip
    // applied before it in the chain removes everything outside the element's own
    // bounds. Nothing inside needed it — the selection pill is a 60x34dp box centred
    // in an 80dp column, and the label is a single line.
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            // One focus target on the item: the click's own, watched by the ring below.
            .tvClickable(
                interactionSource = focusSource,
                pressedFill = colors.primary.copy(alpha = ExpressiveMotion.Press.heldAlpha),
                onClick = onClick
            )
            .expressiveFocusRing(
                ringColor = colors.primary,
                interactionSource = focusSource,
                scrim = colors.focusScrim,
                restShape = M3ExpressivePolygons.Square,
                ringShape = M3ExpressivePolygons.Cookie6Sided
            )
            .semantics {
                role = Role.Tab
                contentDescription = category.displayName
            }
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 60.dp, height = 34.dp)
                .then(
                    if (isSelected) {
                        Modifier.background(colors.primary, indicatorShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            SvgIcon(
                resId = categoryIcon(category),
                contentDescription = null,
                tint = if (isSelected) colors.onPrimary else colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = category.displayName.split(" ").first(),
            fontSize = 10.sp,
            color = if (isSelected) colors.primary else colors.textSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun RailActionButton(
    iconRes: Int,
    description: String,
    onClick: () -> Unit
) {
    // A 12-lobe Burst here rendered as a spiked star: its points overlapped the rail
    // items above it, and with a gear glyph inside it was impossible to tell the button
    // from its icon. `AppIconButton` keeps its silhouette to a circle and a rounded
    // square for exactly that reason.
    AppIconButton(
        iconRes = iconRes,
        contentDescription = description,
        onClick = onClick,
        style = AppIconButtonStyle.Tonal,
        size = 40.dp,
        iconSize = 19.dp
    )
}
