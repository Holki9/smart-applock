package com.holki9.applock.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.holki9.applock.data.PreferencesManager
import com.holki9.applock.ui.components.PinDotsIndicator
import com.holki9.applock.ui.components.PinKeypad
import com.holki9.applock.ui.screens.MainScreen
import com.holki9.applock.ui.screens.SettingsScreen
import com.holki9.applock.ui.screens.SetupScreen
import com.holki9.applock.ui.theme.DarkBackground
import com.holki9.applock.ui.theme.SmartAppLockTheme

enum class ScreenState {
    AUTHENTICATION,
    SETUP,
    MAIN,
    SETTINGS
}

class MainActivity : FragmentActivity() {

    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PreferencesManager.getInstance(this)

        setContent {
            SmartAppLockTheme {
                var currentScreen by remember {
                    mutableStateOf(
                        if (!prefs.isSetupCompleted()) {
                            ScreenState.SETUP
                        } else {
                            ScreenState.AUTHENTICATION
                        }
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AnimatedContent(targetState = currentScreen, label = "screenTransition") { screen ->
                        when (screen) {
                            ScreenState.AUTHENTICATION -> {
                                MainAuthScreen(
                                    onAuthenticated = {
                                        currentScreen = ScreenState.MAIN
                                    }
                                )
                            }
                            ScreenState.SETUP -> {
                                SetupScreen(
                                    onSetupFinished = {
                                        currentScreen = ScreenState.MAIN
                                    }
                                )
                            }
                            ScreenState.MAIN -> {
                                MainScreen(
                                    onNavigateToSettings = {
                                        currentScreen = ScreenState.SETTINGS
                                    }
                                )
                            }
                            ScreenState.SETTINGS -> {
                                SettingsScreen(
                                    onBackClick = {
                                        currentScreen = ScreenState.MAIN
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun MainAuthScreen(onAuthenticated: () -> Unit) {
        var enteredPin by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }

        fun triggerBiometrics() {
            val biometricManager = BiometricManager.from(this@MainActivity)
            val canAuth = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) return

            val executor = ContextCompat.getMainExecutor(this@MainActivity)
            val prompt = BiometricPrompt(this@MainActivity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onAuthenticated()
                }
            })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Вход в Smart AppLock")
                .setSubtitle("Подтвердите личность для управления защитой")
                .setNegativeButtonText("Использовать PIN")
                .build()

            prompt.authenticate(promptInfo)
        }

        LaunchedEffect(Unit) {
            if (prefs.isBiometricEnabled()) {
                triggerBiometrics()
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Smart AppLock",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Введите мастер PIN-код для входа",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    PinDotsIndicator(
                        pinLength = 4,
                        enteredLength = enteredPin.length,
                        isError = isError
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    PinKeypad(
                        onDigitClick = { digit ->
                            if (enteredPin.length < 4) {
                                enteredPin += digit
                                isError = false
                                if (enteredPin.length == 4) {
                                    if (prefs.checkPin(enteredPin)) {
                                        onAuthenticated()
                                    } else {
                                        isError = true
                                        enteredPin = ""
                                    }
                                }
                            }
                        },
                        onDeleteClick = {
                            isError = false
                            if (enteredPin.isNotEmpty()) {
                                enteredPin = enteredPin.dropLast(1)
                            }
                        },
                        onBiometricClick = { triggerBiometrics() },
                        showBiometric = prefs.isBiometricEnabled()
                    )
                }
            }
        }
    }
}
