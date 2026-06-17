package com.devsoto.monical.data.model

import kotlin.math.roundToInt

/** Default portion of a total owed back (the "devolución") when the user hasn't configured one. */
const val DEFAULT_RETURN_SHARE = 0.75

/**
 * User-scoped configuration, stored as a single document (`users/{uid}/meta/settings`). Kept as its
 * own type so more settings can be added later without touching the rest of the data layer.
 *
 * @property returnShare fraction of a receipt total owed back (e.g. `0.75` = 75%).
 */
data class UserSettings(
    val returnShare: Double = DEFAULT_RETURN_SHARE,
) {
    /** [returnShare] expressed as a whole percent, for display and the percentage editor. */
    val returnPercent: Int get() = (returnShare * 100).roundToInt()

    companion object {
        val DEFAULT = UserSettings()

        /** Builds settings from a whole-percent value (0–100), clamped to a sane range. */
        fun fromPercent(percent: Int): UserSettings =
            UserSettings(returnShare = percent.coerceIn(0, 100) / 100.0)
    }
}
