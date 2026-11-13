package com.trustbank.loanapp.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * One form field's value + touched/error state, so validation runs on every
 * keystroke (and on focus loss) instead of only when the submit button is
 * pressed. A field only shows its error once the user has interacted with
 * it — [error] stays null while [touched] is false, so an empty required
 * field doesn't flash red the moment the screen appears.
 *
 * `validate` is re-invoked on every change, so it's safe for it to close
 * over other fields' current values for cross-field rules (e.g. confirming
 * a password matches) — call [revalidate] on the dependent field when the
 * field it depends on changes.
 */
class FieldState internal constructor(
    initialValue: String,
    private val validate: (String) -> String?,
) {
    var value: String by mutableStateOf(initialValue)
        private set
    var touched: Boolean by mutableStateOf(false)
        private set
    private var rawError: String? by mutableStateOf(null)

    val error: String?
        get() = if (touched) rawError else null

    fun onValueChange(newValue: String) {
        value = newValue
        touched = true
        rawError = validate(newValue)
    }

    fun onFocusLost() {
        touched = true
        rawError = validate(value)
    }

    /** Re-runs validation only if this field was already touched — used when a field it depends on changes. */
    fun revalidate() {
        if (touched) rawError = validate(value)
    }

    /** Forces this field to show its current error; used by the submit handler to reveal every remaining issue. */
    fun validateForSubmit(): Boolean {
        touched = true
        rawError = validate(value)
        return rawError == null
    }
}

@Composable
fun rememberFieldState(initialValue: String = "", validate: (String) -> String?): FieldState {
    return remember { FieldState(initialValue, validate) }
}
