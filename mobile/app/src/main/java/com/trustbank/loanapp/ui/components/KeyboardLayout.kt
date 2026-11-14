package com.trustbank.loanapp.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity

/** Applies once above navigation so every screen gets a keyboard-sized, scrollable viewport. */
@Composable
fun KeyboardAwareContent(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) { content() }
}

/** Re-check the entire focused field after each IME inset change, including keyboard animation. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.keepFocusedFieldVisible(): Modifier {
    val requester = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    val keyboardBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(focused, keyboardBottom) {
        if (focused) {
            // Insets are applied during layout; wait for that layout before requesting a scroll.
            withFrameNanos { }
            requester.bringIntoView()
        }
    }
    return bringIntoViewRequester(requester).onFocusChanged { focused = it.isFocused }
}
