package com.yusufteker.healthtrack.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/**
 * Keeps a whole "input group" (e.g. a text field **and** its primary action button)
 * visible above the soft keyboard.
 *
 * By default Compose only scrolls the focused text field itself into view, which often
 * leaves the submit button (Kaydol / Hesapla / Kaydet) hidden behind the keyboard.
 * Apply this modifier to the container that wraps the input(s) and the action button:
 * whenever any child gains focus, or the keyboard height changes while a child is focused,
 * the entire container is brought into the visible viewport of the nearest scrollable parent.
 *
 * Requires a scrollable ancestor (`verticalScroll` / `LazyColumn`) and IME-aware padding
 * (`imePadding()`) somewhere above it, per AGENTS.md Rule 1.2.
 */
fun Modifier.keepVisibleAboveIme(): Modifier = composed {
    val requester = remember { BringIntoViewRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime

    LaunchedEffect(requester) {
        snapshotFlow { hasFocus to imeInsets.getBottom(density) }
            .collectLatest { (focused, imeBottom) ->
                if (focused && imeBottom > 0) {
                    // Debounce while the keyboard slide animation settles, then reveal the group.
                    delay(90)
                    requester.bringIntoView()
                }
            }
    }

    this
        .bringIntoViewRequester(requester)
        .onFocusChanged { hasFocus = it.hasFocus }
}

/**
 * Returns a lambda that clears focus and hides the soft keyboard.
 * Use after a successful submit (öğün eklendi, kod tamamlandı, kaydet vb.).
 */
@Composable
fun rememberKeyboardDismisser(): () -> Unit {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val latestFocus by rememberUpdatedState(focusManager)
    val latestKeyboard by rememberUpdatedState(keyboardController)
    return remember {
        {
            latestFocus.clearFocus(force = true)
            latestKeyboard?.hide()
        }
    }
}
