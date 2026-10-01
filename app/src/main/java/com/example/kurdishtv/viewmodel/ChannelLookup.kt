package com.example.kurdishtv.viewmodel

import com.example.kurdishtv.model.Channel

/**
 * The channels named by [ids], in the order [ids] gives them, skipping any that are
 * no longer in [channels].
 *
 * ## Why not a map
 *
 * This reads about a dozen ids out of a catalogue that runs to well over a thousand
 * channels, and it was written three times as
 *
 * ```
 * val byId = channels.associateBy { it.id }
 * ids.mapNotNull { byId[it] }
 * ```
 *
 * which builds a hash entry for **every channel in the catalogue** — a `Channel` key,
 * a table insertion and a string hash per entry — to answer about a dozen questions.
 * Almost every entry built is never read. The cost is paid on every cold start, on
 * every merge, and on every single channel selection, and it scales with the
 * catalogue rather than with the handful of ids actually being asked about.
 *
 * This walks the catalogue once instead, and the membership test is against a set
 * built from [ids] — at most a dozen entries, which is the size the question really
 * is. So the work is `O(catalogue)` in the unavoidable read and `O(ids)` in
 * allocation, rather than `O(catalogue)` in allocation as well.
 *
 * ## Why one pass and not a lookup per id
 *
 * The obvious alternative is `ids.mapNotNull { id -> channels.firstOrNull { it.id == id } }`,
 * which allocates nothing at all — at the cost of a full scan per id, so twelve scans
 * of a thousand channels to answer the same question. One pass with a small set beats
 * both: the catalogue is read once, and the only thing allocated is the answer.
 *
 * Free and pure, so the ordering and the skipping are pinned by
 * `ChannelLookupTest` rather than left to be checked by eye.
 *
 * @param ids ids to resolve, most-recent-first as the caller stores them. Duplicates
 *   are kept: the caller asked for a sequence, not a set.
 * @return the matching channels in `ids` order, with no entries for ids the
 *   catalogue no longer holds — which is the normal case after a refresh, since a
 *   source going down or a playlist being edited can remove a channel.
 */
internal fun channelsForIds(channels: List<Channel>, ids: List<String>): List<Channel> {
    if (ids.isEmpty() || channels.isEmpty()) return emptyList()

    // Small by construction: the watch history is capped, and a caller passing a
    // large list has asked a different question than this one answers well.
    val wanted = ids.toHashSet()

    // One pass, collecting only matches. Iterating the catalogue rather than the ids
    // is what keeps this to a single read of a list that may be over a thousand long.
    val found = HashMap<String, Channel>(wanted.size * 2)
    for (channel in channels) {
        val id = channel.id
        if (wanted.contains(id) && !found.containsKey(id)) {
            found[id] = channel
        }
    }
    if (found.isEmpty()) return emptyList()

    // Re-projected into `ids` order, because the history is most-recent-first and the
    // row is drawn in that order. Collecting during the scan would give catalogue
    // order instead, which is not what the caller asked for.
    val result = ArrayList<Channel>(ids.size)
    for (id in ids) {
        found[id]?.let { result.add(it) }
    }
    return result
}