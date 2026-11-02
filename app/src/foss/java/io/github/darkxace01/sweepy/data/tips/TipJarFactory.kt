package io.github.darkxace01.sweepy.data.tips

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow

/** The F-Droid build has no Google Play billing, so no tip jar. */
@Suppress("UNUSED_PARAMETER")
fun createTipJar(context: Context, scope: CoroutineScope, onTipped: (Tip, Int) -> Unit): TipJar = object : TipJar {
    override val supported = false
    override val state: StateFlow<TipJarState> = MutableStateFlow(TipJarState.Unavailable).asStateFlow()
    override val outcomes: Flow<TipOutcome> = emptyFlow()
    override fun refresh() = Unit
    override fun buy(activity: Activity, tip: Tip) = Unit
}
