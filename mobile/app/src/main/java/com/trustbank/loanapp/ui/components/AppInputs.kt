package com.trustbank.loanapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.trustbank.loanapp.ui.theme.AppColors
import com.trustbank.loanapp.util.Validators

@Composable
fun AppTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    error: String? = null,
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
            isError = error != null,
            singleLine = singleLine,
            minLines = minLines,
            enabled = enabled,
            trailingIcon = trailingIcon,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.Primary600,
                cursorColor = AppColors.Primary600,
                errorBorderColor = AppColors.Danger600,
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
fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    error: String? = null,
    onFocusLost: (() -> Unit)? = null,
) {
    var visible by remember { mutableStateOf(false) }

    AppTextField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
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
                    tint = AppColors.Neutral500,
                )
            }
        },
    )
}

/**
 * Full-name input: every keystroke/paste is filtered down to letters and
 * spaces in [onValueChange] itself, so a digit or symbol can never appear
 * in the field. Mirrors web/src/components/ui/NameField.tsx.
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
    AppTextField(
        label = label,
        value = value,
        onValueChange = { raw -> onValueChange(Validators.sanitizeNameInput(raw)) },
        modifier = modifier,
        required = required,
        error = error,
        placeholder = placeholder,
        onFocusLost = onFocusLost,
    )
}

/**
 * Phone input for Uganda numbers: every keystroke/paste is filtered to
 * digits only and hard-capped at 10 characters in [onValueChange] itself —
 * so a non-digit character never appears in the field, and typing past the
 * 10th digit is simply ignored. Mirrors web/src/components/ui/PhoneField.tsx.
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
    AppTextField(
        label = label,
        value = value,
        onValueChange = { raw -> onValueChange(Validators.sanitizePhoneInput(raw)) },
        modifier = modifier,
        required = required,
        error = error,
        placeholder = "07XXXXXXXX",
        keyboardType = KeyboardType.NumberPassword,
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
