package io.github.darkxace01.sweepy.ui.deck

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

private const val MAX_ZOOM = 5f

/**
 * Pinch with two fingers to look closer; let go and the photo springs back. Because the zoom
 * never sticks, it can't get in the way of swiping with one finger.
 */
@Composable
fun Modifier.pinchToPeek(): Modifier {
    val scope = rememberCoroutineScope()
    val zoom = remember { Animatable(1f) }
    val pan = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    return this
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                var pinching = false
                do {
                    val event = awaitPointerEvent()
                    val fingers = event.changes.count { it.pressed }
                    if (fingers >= 2) {
                        pinching = true
                        // Zoom around the point between the fingers, measured from the center.
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val focus = event.calculateCentroid() - center
                        val newZoom = (zoom.value * event.calculateZoom()).coerceIn(1f, MAX_ZOOM)
                        val factor = newZoom / zoom.value
                        val newPan = focus - (focus - pan.value) * factor + event.calculatePan()
                        scope.launch {
                            zoom.snapTo(newZoom)
                            pan.snapTo(newPan)
                        }
                    }
                    // Once a pinch has started, keep the card from also being dragged.
                    if (pinching) event.changes.forEach { it.consume() }
                } while (event.changes.any { it.pressed })
                if (pinching) {
                    val settle = spring<Float>(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)
                    scope.launch { zoom.animateTo(1f, settle) }
                    scope.launch { pan.animateTo(Offset.Zero, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)) }
                }
            }
        }
        .graphicsLayer {
            scaleX = zoom.value
            scaleY = zoom.value
            translationX = pan.value.x
            translationY = pan.value.y
        }
}
