package com.trustbank.loanapp.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    // Compose's LocalView.context is usually a ContextThemeWrapper around
    // the Activity, not the Activity itself — an `as? Activity` cast on it
    // silently fails, so this has to unwrap ContextWrapper layers instead.
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Edge-to-edge (`enableEdgeToEdge()` in MainActivity) draws app content
 * behind the system status bar, but doesn't know whether *this* screen's
 * content under that bar is light or dark — so without this, the system's
 * status/battery/network icons can end up the wrong color for the screen
 * (e.g. white-on-white and effectively invisible). Call this once per
 * screen with `darkIcons = true` when that screen's top area is a light
 * color (icons render dark), or `false` when it's a dark/saturated color
 * like the brand blue header (icons render light).
 */
@Composable
fun SetStatusBarAppearance(darkIcons: Boolean) {
    val view = LocalView.current
    val activity = view.context.findActivity() ?: return
    SideEffect {
        WindowCompat.getInsetsController(activity.window, view).isAppearanceLightStatusBars = darkIcons
    }
}
