package com.example.kurdishtv.ui.keys

/**
 * How the remote's own keys are treated, in one place.
 *
 * ## Why this exists
 *
 * Both screens that answer the number pad — the browse grid and the player — used to
 * carry their own `onKeyEvent` with a single rule between them: *handle Key Down, and
 * only Key Down.* The reasoning written beside it was that Key Up "is the same
 * physical press reported again, and acting on both would enter every digit twice".
 *
 * That is true, and it is not the problem. Auto-repeat is delivered as a stream of
 * **additional Key Down events** carrying an increasing `repeatCount`, so filtering
 * Key Up does nothing about it at all:
 *
 *  - holding `4` entered `4444` — the four-digit cap in `TvViewModel` truncated it to
 *    a number the viewer never asked for rather than preventing it,
 *  - holding `BACKSPACE` emptied the number in a single press,
 *  - holding `ENTER` committed before the viewer had finished deciding.
 *
 * The channel keys want the opposite treatment, and this is why the rule could not
 * simply be "ignore every repeat": a thumb held on CH+ *should* walk down the list.
 * Android's own repeat rate is around thirty events a second, which on a grid of six
 * hundred channels means the viewer arrives somewhere nobody chose. So repeats are
 * allowed for stepping and rate-limited, and refused outright for everything that
 * builds a value.
 *
 * Pure and integer-only on purpose: the arithmetic here is what the D-pad gets right
 * or wrong, and it is the one part of this that a unit test can reach without a
 * device, a remote, and a stream.
 */
object RemoteKeyPolicy {

    /**
     * The shortest gap between two accepted channel steps while CH+ or CH- is held.
     *
     * Long enough that a held key reads as stepping through channels rather than as
     * teleporting; short enough that holding it across a screenful is not a chore.
     * Roughly four channels a second — about what a television set itself does.
     */
    const val CHANNEL_STEP_MIN_INTERVAL_MS = 250L

    /**
     * Whether this event is the platform repeating a key the viewer is still holding.
     *
     * `repeatCount` is 0 on the initial press and grows from there. Callers that build
     * a value — digits, commit, delete, cancel — want `!isAutoRepeat(...)`; callers
     * that move a selection want the rate limit in [acceptsChannelStep].
     */
    fun isAutoRepeat(repeatCount: Int): Boolean = repeatCount > 0

    /**
     * Whether a CH+/CH- repeat is far enough past the last accepted step to act on.
     *
     * @param lastStepAtMs when the previous step was accepted, or null when none has
     *   been — which always accepts, so the very first press of a held key lands.
     */
    fun acceptsChannelStep(nowMs: Long, lastStepAtMs: Long?): Boolean {
        if (lastStepAtMs == null) return true
        return nowMs - lastStepAtMs >= CHANNEL_STEP_MIN_INTERVAL_MS
    }
}
