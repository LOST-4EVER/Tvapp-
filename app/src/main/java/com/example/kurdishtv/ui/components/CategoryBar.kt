package com.example.kurdishtv.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kurdishtv.model.CategoryFilter
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.model.ChannelFilterEngine
import com.example.kurdishtv.ui.motion.ExpressiveMotion
import com.example.kurdishtv.ui.motion.ShapeMorph
import com.example.kurdishtv.ui.motion.expressiveFocusRing
import com.example.kurdishtv.ui.motion.tvClickable
import com.example.kurdishtv.ui.theme.M3ExpressivePolygons
import com.example.ui.theme.LocalAppColors

@Composable
fun CategoryBar(
    selectedCategory: CategoryFilter,
    onCategorySelected: (CategoryFilter) -> Unit,
    channels: List<Channel> = emptyList(),
    /**
     * Called with the categories that actually have channels behind them.
     *
     * The navigation rail needs the same answer this composable works out for itself.
     * It used to keep its own hard-coded list, which meant the two rows of navigation
     * disagreed: the rail offered tabs for categories the chips had hidden as empty,
     * and omitted six of the twelve entirely. Reporting the list from the one place
     * that computes it is what keeps them in agreement.
     */
    onVisibleCategories: (List<CategoryFilter>) -> Unit = {},
    modifier: Modifier = Modifier,
    /**
     * Shorten the chips for a short window.
     *
     * A phone on its side has around 360dp of height. Between them the offline
     * banner, the brand row, the search field and the category row were spending
     * well over half of it before a single channel appeared, and the chips were the
     * most compressible part of that: they carry one short word and nothing else.
     */
    compact: Boolean = false
) {
    val colors = LocalAppColors.current

    // Hide tabs that would show nothing.
    //
    // The Sports tab had no channel in the curated catalog (its one stream 404s
    // and was removed), so tapping it produced an empty grid with no explanation.
    // Computing the counts from the live list means a tab disappears when it has
    // no content and reappears as soon as an imported playlist supplies some —
    // which is also why the current selection is preserved if it still matches.
    // Not wrapped in `remember(channels)`.
    //
    // `remember` compares its key with `equals`, and a `List<Channel>` of several
    // hundred nine-field data classes is compared field by field — on every
    // recomposition of this bar, which is the browse screen's permanent chrome, so
    // on every keystroke of the search field. That is several hundred deep
    // comparisons per character typed, to decide whether a list that had not
    // changed had changed.
    //
    // [ChannelFilterEngine.countsByCategory] memoises by list *identity*, which is
    // both the cheap test and the correct one: a merge, a favourite rebuild or a
    // re-filter all replace the list wholesale, so a new object is exactly the
    // signal that the counts are stale.
    val counts = if (channels.isEmpty()) {
        // Before the list has loaded, show everything rather than flickering
        // tabs in and out as the merge completes.
        null
    } else {
        // One pass over the list, not one pass per category. Asking
        // `filter(channels, category, "")` for each category in turn meant
        // reading every channel twelve times and allocating twelve result lists,
        // and it re-ran on every change to the channel list — which is to say, on
        // every favourite toggle, for a number nobody looks at changing.
        ChannelFilterEngine.countsByCategory(channels)
    }
    // Keyed on the selection as well as the counts. The fallback below depends on
    // both, and keying only on `counts` meant that moving the selection to a category
    // the current list has no channels for reused the previous tab list — so the
    // "keep the selected tab" rule below could not actually fire.
    val visible = remember(counts, selectedCategory) {
        when {
            counts == null -> CategoryFilter.entries
            // Keep the selected tab even when the current list has nothing behind
            // it, so the selection is never silently dropped out from under the
            // viewer: the chip and the matching rail item stay visible and
            // highlighted, and the grid explains the empty category rather than
            // the navigation forgetting which one is open.
            //
            // This used to branch on whether the selected category had a non-zero
            // count, and the branch that ran when it did not dropped the tab — so
            // the `|| it == selectedCategory` below could only ever run in the one
            // case where it could not matter, and the empty-selected case it was
            // written for was exactly the one it skipped.
            else -> CategoryFilter.entries.filter { counts[it] != 0 || it == selectedCategory }
        }
    }

    // Reported upward in an effect rather than during composition, so the rail is
    // never written to from inside this composable's own body — a state write
    // during composition is what Compose calls a side effect, and it re-runs the
    // composition it came from.
    LaunchedEffect(visible) { onVisibleCategories(visible) }

    // There are more tabs than fit on a phone, and the row starts at the beginning,
    // so a non-default selection (a restored "start category", or a tap on a tab
    // further right) could be entirely off-screen with nothing to suggest it
    // existed. Bring it into view whenever the selection changes.
    val listState = rememberLazyListState()
    LaunchedEffect(selectedCategory, visible) {
        val index = visible.indexOf(selectedCategory)
        if (index < 0) return@LaunchedEffect
        // Only when the chip is genuinely out of view.
        //
        // `animateScrollToItem` scrolls its target to the leading edge, so running it
        // on every selection change dragged the whole row along as the viewer arrowed
        // from chip to chip: the chips under the D-pad appeared to slide out from
        // under the highlight. A chip that is already on screen needs nothing, and on
        // a wide window that is every chip there is.
        val onScreen = listState.layoutInfo.visibleItemsInfo.any { item -> item.index == index }
        if (onScreen) return@LaunchedEffect
        listState.scrollToItem(index)
    }

    LazyRow(
        modifier = modifier,
        state = listState,
        // 8dp between chips, and never less. The focus ring below reaches 7dp past
        // the chip it marks (see `FocusRingGeometryTest`), so a 6dp gap let a focused
        // chip's scrim band paint a sliver into its neighbour. The compact layout
        // saves its vertical space from the chrome's height, not from a gap narrow
        // enough to collide.
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = if (compact) 3.dp else 6.dp)
    ) {
        items(visible, key = { it.name }) { category ->
            val isSelected = selectedCategory == category
            // Shared by the click and the ring below, so the two cannot disagree
            // about where the highlight is.
            val chipSource = remember(category) { MutableInteractionSource() }

            // Selection switches these three instantly. Each was a
            // `animateColorAsState` / `animateDpAsState` on a spring, and selecting a
            // category is the single most repeated gesture in the app, so these were
            // also the transitions that ran most often — three per chip press across
            // twelve chips.
            val backgroundColor = if (isSelected) colors.primary else colors.surfaceHigh
            val contentColor = if (isSelected) colors.onPrimary else colors.textSecondary

            // A true pill — a 50% corner radius — so it is a pill whatever the label
            // length is. Selection is carried by fill, label weight and elevation
            // together, which survives being read at an angle across a room in a way
            // a tint shift on its own does not.
            val pillShape = ShapeMorph.pill

            Surface(
                shape = pillShape,
                color = backgroundColor,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = if (isSelected) colors.primary else colors.border,
                        shape = pillShape
                    )
                    .tvClickable(
                        interactionSource = chipSource,
                        pressedFill = colors.primary.copy(alpha = ExpressiveMotion.Press.heldAlpha),
                        pressedShape = pillShape
                    ) {
                        onCategorySelected(category)
                    }
                    // A focus ring, which this row did not have at all.
                    //
                    // These chips are the app's primary navigation and `tvClickable`
                    // gives each one a focus target, so the D-pad lands on them — and
                    // a chip carried no focus mark of any kind, so arrowing along the
                    // row moved an invisible highlight and the viewer had no way to
                    // tell which of twelve categories they were about to open. This is
                    // the one row where focus matters most: it is the first thing the
                    // remote reaches on the browse screen, and the thing a viewer
                    // arrows along without thinking.
                    .expressiveFocusRing(
                        ringColor = colors.primary,
                        interactionSource = chipSource,
                        scrim = colors.focusScrim,
                        restShape = M3ExpressivePolygons.Square,
                        ringShape = ShapeMorph.focusRing
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(
                        horizontal = if (compact) 11.dp else 16.dp,
                        vertical = if (compact) 6.dp else 10.dp
                    )
                ) {
                    SvgIcon(
                        resId = categoryIcon(category),
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(if (compact) 14.dp else 16.dp)
                    )
                    Spacer(modifier = Modifier.width(if (compact) 5.dp else 6.dp))
                    Text(
                        text = category.displayName,
                        color = contentColor,
                        fontSize = if (compact) 12.sp else 13.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        maxLines = 1
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
    CategoryFilter.DOCUMENTARY -> KurdishTvIcons.Movie
    CategoryFilter.QURAN -> KurdishTvIcons.Quran
    CategoryFilter.RELIGIOUS -> KurdishTvIcons.Religious
    CategoryFilter.FAVORITES -> KurdishTvIcons.FavoriteFilledRes
    CategoryFilter.HD -> KurdishTvIcons.Signal
}
