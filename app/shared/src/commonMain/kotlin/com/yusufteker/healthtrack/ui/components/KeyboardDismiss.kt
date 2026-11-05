package com.yusufteker.healthtrack.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/**
 * Universal Modifier to automatically dismiss the soft keyboard and clear active focus
 * when tapping anywhere on non-interactive areas of the screen or background.
 *
 * It listens in [PointerEventPass.Initial] to track touch initiation and verifies in
 * [PointerEventPass.Final] that the tap was neither a drag/scroll gesture nor consumed by
 * interactive child elements (such as buttons, text fields, or clickable items).
 *
 * This provides seamless iOS & Android "tap outside to dismiss keyboard" behavior.
 */
fun Modifier.clearFocusOnTap(): Modifier = composed {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(pass = PointerEventPass.Initial)
            val downPos = down.position
            var isDrag = false

            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Final)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                if ((change.position - downPos).getDistance() > viewConfiguration.touchSlop) {
                    isDrag = true
                }

                if (change.changedToUp()) {
                    if (!isDrag && !change.isConsumed) {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                    }
                    break
                }
            }
        }
    }
}
