package com.trustbank.loanapp

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.enableEdgeToEdge
import com.trustbank.loanapp.ui.components.AppTextField
import com.trustbank.loanapp.ui.components.KeyboardAwareContent
import com.trustbank.loanapp.ui.theme.TrustBankLoansTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class KeyboardVisibilityTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun bottomFieldAndTypedTextStayAboveTheActualSoftwareKeyboard() {
        var fieldBounds = Rect.Zero
        var typed = ""
        compose.activityRule.scenario.onActivity {
            it.enableEdgeToEdge()
            it.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        compose.setContent {
            TrustBankLoansTheme {
                KeyboardAwareContent {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Spacer(Modifier.height(300.dp))
                        var value by remember { mutableStateOf("") }
                        AppTextField(label = "Keyboard regression field", value = value,
                            onValueChange = { value = it; typed = it }, singleLine = false, minLines = 3,
                            modifier = Modifier.onGloballyPositioned { fieldBounds = it.boundsInWindow() })
                        Spacer(Modifier.height(220.dp))
                    }
                }
            }
        }
        compose.onNodeWithText("Keyboard regression field").performClick()
        compose.waitUntil(20_000) {
            ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        compose.onNodeWithText("Keyboard regression field").performTextInput("Visible while typing")
        compose.waitUntil(20_000) {
            val decor = compose.activity.window.decorView
            val ime = ViewCompat.getRootWindowInsets(decor)?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
            ime > 0 && fieldBounds.bottom <= decor.height - ime + 2 && fieldBounds.top >= 0
        }
        assertEquals("Visible while typing", typed)
        assertTrue(fieldBounds.height > 0)
    }
}
