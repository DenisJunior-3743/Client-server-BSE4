package com.trustbank.loanapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.trustbank.loanapp.ui.theme.AppColors
import com.trustbank.loanapp.util.Validators
import kotlinx.coroutines.delay

/**
 * A transient "you just typed something invalid" hint: call the returned
 * setter to show a message, and it clears itself a couple of seconds later
 * (or immediately, the next keystroke that isn't rejected passes `null`).
 */
@Composable
private fun rememberDismissingHint(): Pair<String?, (String?) -> Unit> {
    var hint by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(hint) {
        if (hint != null) {
            delay(2200)
            hint = null
        }
    }
    return hint to { new -> hint = new }
}

@Composable
fun AppTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    error: String? = null,
    /**
     * A transient warning naming a character just rejected as the user
     * typed (e.g. `"3" is not allowed — letters and spaces only`). Shown in
     * place of [error] while it's active, since it's the fresher signal.
     */
    hint: String? = null,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    trailingIcon: (@Composable () -> Unit)? = null,
    onFocusLost: (() -> Unit)? = null,
) {
    var hasBeenFocused by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(if (required) "$label *" else label) },
            placeholder = if (placeholder != null) {
                { Text(placeholder) }
            } else {
                null
            },
            isError = error != null || hint != null,
            singleLine = singleLine,
            minLines = minLines,
            enabled = enabled,
            trailingIcon = trailingIcon,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.Primary600,
                cursorColor = AppColors.Primary600,
                errorBorderColor = if (hint != null) AppColors.Warning600 else AppColors.Danger600,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        hasBeenFocused = true
                    } else if (hasBeenFocused) {
                        onFocusLost?.invoke()
                    }
                },
        )
        if (hint != null) {
            Text(
                text = hint,
                color = AppColors.Warning700,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        } else if (error != null) {
            Text(
                text = error,
                color = AppColors.Danger600,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
    }
}

@Composable
fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    error: String? = null,
    showStrengthMeter: Boolean = false,
    onFocusLost: (() -> Unit)? = null,
) {
    var visible by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        AppTextField(
            label = label,
            value = value,
            onValueChange = onValueChange,
            required = required,
            error = error,
            keyboardType = KeyboardType.Password,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            onFocusLost = onFocusLost,
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (visible) "Hide password" else "Show password",
                        tint = if (visible) AppColors.Primary600 else AppColors.Neutral500,
                    )
                }
            },
        )
        if (showStrengthMeter && value.isNotEmpty()) {
            PasswordStrengthMeter(value = value, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/**
 * Shows how much of the password policy (8+ chars, upper, lower, digit,
 * symbol) has been met so far: a color-coded progress bar plus a checklist
 * of the individual criteria, so the user can see exactly what's missing
 * instead of just a pass/fail message on submit.
 */
@Composable
fun PasswordStrengthMeter(value: String, modifier: Modifier = Modifier) {
    val percent = Validators.passwordStrengthPercent(value)
    val label = Validators.passwordStrengthLabel(percent)
    val color = when {
        percent < 40 -> AppColors.Danger600
        percent < 70 -> AppColors.Warning600
        percent < 100 -> AppColors.Primary600
        else -> AppColors.Success600
    }
    val animatedProgress by animateFloatAsState(targetValue = percent / 100f, label = "passwordStrength")

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Password strength", style = MaterialTheme.typography.bodySmall, color = AppColors.Neutral500)
            if (label.isNotEmpty()) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = animatedProgress,
            color = color,
            trackColor = AppColors.Neutral200,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
        )
        Spacer(Modifier.height(8.dp))
        val criteria = listOf(
            "8+ characters" to (value.length >= 8),
            "Uppercase" to value.any { it.isUpperCase() },
            "Lowercase" to value.any { it.isLowerCase() },
            "Number" to value.any { it.isDigit() },
            "Symbol (@#!...)" to value.any { !it.isLetterOrDigit() },
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            criteria.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { (text, met) -> PasswordCriterionRow(text, met) }
                }
            }
        }
    }
}

@Composable
private fun PasswordCriterionRow(text: String, met: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (met) Icons.Filled.Check else Icons.Filled.Close,
            contentDescription = null,
            tint = if (met) AppColors.Success600 else AppColors.Neutral400,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = if (met) AppColors.Neutral700 else AppColors.Neutral400,
        )
    }
}

/**
 * Full-name input: every keystroke/paste is filtered down to letters and
 * spaces in [onValueChange] itself, so a digit or symbol can never appear
 * in the field — and the moment a keystroke gets rejected, a hint names the
 * exact character that wasn't allowed. Mirrors web/src/components/ui/NameField.tsx.
 */
@Composable
fun NameField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    required: Boolean = true,
    error: String? = null,
    placeholder: String? = null,
    onFocusLost: (() -> Unit)? = null,
) {
    val (hint, setHint) = rememberDismissingHint()
    AppTextField(
        label = label,
        value = value,
        onValueChange = { raw ->
            val (sanitized, message) = Validators.sanitizeWithFeedback(
                raw = raw,
                allowedDescription = Validators.NAME_ALLOWED_DESCRIPTION,
            ) { it.isLetter() || it == ' ' }
            setHint(message)
            onValueChange(sanitized)
        },
        modifier = modifier,
        required = required,
        error = error,
        hint = hint,
        placeholder = placeholder,
        onFocusLost = onFocusLost,
    )
}

/**
 * Phone input for Uganda numbers: every keystroke/paste is filtered to
 * digits only and hard-capped at 10 characters in [onValueChange] itself —
 * so a non-digit character never appears in the field, typing past the 10th
 * digit is simply ignored, and either case shows a hint explaining why.
 * Mirrors web/src/components/ui/PhoneField.tsx.
 */
@Composable
fun PhoneField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Phone number",
    required: Boolean = true,
    error: String? = null,
    onFocusLost: (() -> Unit)? = null,
) {
    val (hint, setHint) = rememberDismissingHint()
    AppTextField(
        label = label,
        value = value,
        onValueChange = { raw ->
            val (sanitized, message) = Validators.sanitizeWithFeedback(
                raw = raw,
                maxLength = Validators.PHONE_MAX_LENGTH,
                allowedDescription = "digits only",
            ) { it.isDigit() }
            setHint(message)
            onValueChange(sanitized)
        },
        modifier = modifier,
        required = required,
        error = error,
        hint = hint,
        placeholder = "07XXXXXXXX",
        keyboardType = KeyboardType.NumberPassword,
        onFocusLost = onFocusLost,
    )
}

/**
 * Digits-only input (loan amounts, repayment terms, monthly income): every
 * keystroke is filtered to digits, optionally capped at [maxLength], with a
 * hint naming any character just rejected.
 */
@Composable
fun DigitsField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxLength: Int? = null,
    required: Boolean = true,
    error: String? = null,
    placeholder: String? = null,
    onFocusLost: (() -> Unit)? = null,
) {
    val (hint, setHint) = rememberDismissingHint()
    AppTextField(
        label = label,
        value = value,
        onValueChange = { raw ->
            val (sanitized, message) = Validators.sanitizeWithFeedback(
                raw = raw,
                maxLength = maxLength,
                allowedDescription = "digits only",
            ) { it.isDigit() }
            setHint(message)
            onValueChange(sanitized)
        },
        modifier = modifier,
        required = required,
        error = error,
        hint = hint,
        placeholder = placeholder,
        keyboardType = KeyboardType.Number,
        onFocusLost = onFocusLost,
    )
}

/**
 * Uganda National ID (NIN) input: filtered to letters/digits, upper-cased
 * and capped at 14 characters, with a rejection hint for anything else.
 */
@Composable
fun NationalIdField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "National ID",
    required: Boolean = true,
    error: String? = null,
    onFocusLost: (() -> Unit)? = null,
) {
    val (hint, setHint) = rememberDismissingHint()
    AppTextField(
        label = label,
        value = value,
        onValueChange = { raw ->
            val (sanitized, message) = Validators.sanitizeWithFeedback(
                raw = raw.uppercase(),
                maxLength = Validators.NATIONAL_ID_LENGTH,
                allowedDescription = Validators.NATIONAL_ID_ALLOWED_DESCRIPTION,
            ) { it.isLetterOrDigit() }
            setHint(message)
            onValueChange(sanitized)
        },
        modifier = modifier,
        required = required,
        error = error,
        hint = hint,
        placeholder = "e.g. CM12345678ABCD",
        onFocusLost = onFocusLost,
    )
}

/**
 * A simple, version-safe dropdown: a read-only OutlinedTextField with a
 * transparent clickable overlay (readOnly fields don't reliably forward
 * clicks on their own) plus a plain DropdownMenu anchored to it. Avoids
 * Material3's ExposedDropdownMenuBox/-Menu, whose exact API shape has moved
 * around across versions.
 */
@Composable
fun <T> AppDropdownField(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    error: String? = null,
    placeholder: String = "Select an option",
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selected?.let(optionLabel) ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(if (required) "$label *" else label) },
                placeholder = { Text(placeholder) },
                isError = error != null,
                trailingIcon = {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.Primary600,
                    errorBorderColor = AppColors.Danger600,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { expanded = !expanded },
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.92f),
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                    )
                }
            }
        }
        if (error != null) {
            Text(
                text = error,
                color = AppColors.Danger600,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    containerColor: Color = AppColors.Primary600,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = Color.White),
        modifier = modifier.fillMaxWidth().height(48.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(48.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
