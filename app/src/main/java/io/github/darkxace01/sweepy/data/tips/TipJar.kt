package io.github.darkxace01.sweepy.data.tips

import android.app.Activity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * One-time tips. They unlock nothing: everything in Sweepy is free. [productId] must match an
 * in-app product (consumable, so people can tip more than once) in the Play Console.
 */
enum class Tip(val productId: String) {
    COFFEE("tip_coffee"),
    LUNCH("tip_lunch"),
    DINNER("tip_dinner"),
}

sealed interface TipJarState {
    data object Loading : TipJarState

    /** No Play Store, not signed in, or the products aren't set up yet. */
    data object Unavailable : TipJarState

    /** Localized price of each tip, e.g. "$1.99" or "₹89". */
    data class Ready(val prices: Map<Tip, String>) : TipJarState
}

sealed interface TipOutcome {
    /** [count] is more than 1 when someone bought several at once. */
    data class Thanked(val tip: Tip, val count: Int = 1) : TipOutcome

    /** Paid with a slow method (cash, some bank transfers); the thank-you comes when it clears. */
    data object Pending : TipOutcome
    data object Failed : TipOutcome
}

interface TipJar {
    /** False in builds without Google Play billing; the app then hides every way in. */
    val supported: Boolean
    val state: StateFlow<TipJarState>

    /** Results of purchases, as they happen. */
    val outcomes: Flow<TipOutcome>

    /** Connects to Play, loads prices and finishes any tip that went through while the app was closed. */
    fun refresh()
    fun buy(activity: Activity, tip: Tip)
}
