package com.yusufteker.healthtrack.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Universal horizontal swipe modifier for navigating days.
 * - Swipe right (finger moves left -> right): triggers [onSwipeRight] (e.g. Previous Day)
 * - Swipe left (finger moves right -> left): triggers [onSwipeLeft] (e.g. Next Day)
 *
 * Designed to coexist smoothly with vertical scroll, button clicks, and child horizontal scrolling.
 */
fun Modifier.onHorizontalSwipe(
    threshold: Dp = 48.dp,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
): Modifier = composed {
    val density = LocalDensity.current
    val thresholdPx = remember(density, threshold) { with(density) { threshold.toPx() } }

    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(pass = PointerEventPass.Initial)
            var totalX = 0f
            var totalY = 0f
            var isHorizontal = false
            var isVertical = false
            var childConsumed = false

            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Final)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                if (change.isConsumed) {
                    childConsumed = true
                }

                val diffX = change.position.x - down.position.x
                val diffY = change.position.y - down.position.y
                totalX = diffX
                totalY = diffY

                if (!isHorizontal && !isVertical) {
                    if (abs(diffX) > viewConfiguration.touchSlop && abs(diffX) > abs(diffY) * 1.3f) {
                        isHorizontal = true
                    } else if (abs(diffY) > viewConfiguration.touchSlop) {
                        isVertical = true
                    }
                }

                if (change.changedToUp()) {
                    if (isHorizontal && !isVertical && !childConsumed && abs(totalX) >= thresholdPx) {
                        if (totalX > 0) {
                            onSwipeRight()
                        } else {
                            onSwipeLeft()
                        }
                    }
                    break
                }
            }
        }
    }
}
