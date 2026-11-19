package com.holki9.applock.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.holki9.applock.data.AppLockState
import com.holki9.applock.data.PreferencesManager
import com.holki9.applock.ui.components.PinDotsIndicator
import com.holki9.applock.ui.components.PinKeypad
import com.holki9.applock.ui.theme.DarkBackground
import com.holki9.applock.ui.theme.SmartAppLockTheme
import com.holki9.applock.util.AppHelper

class LockActivity : FragmentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "LOCKED_PACKAGE"
        const val EXTRA_APP_NAME = "LOCKED_APP_NAME"
    }

    private lateinit var targetPackage: String
    private lateinit var appName: String
    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PreferencesManager.getInstance(this)

        targetPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        appName = intent.getStringExtra(EXTRA_APP_NAME) ?: AppHelper.findLabelByPackage(this, targetPackage)

        // If back is pressed, take user to home screen instead of the locked app
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goToHomeScreen()
            }
        })

        setContent {
            SmartAppLockTheme(darkTheme = true) {
                LockScreenContent(
                    appName = appName,
                    targetPackage = targetPackage,
                    onUnlockSuccess = {
                        unlockAndFinish()
                    },
                    onTriggerBiometric = {
                        triggerBiometrics()
                    }
                )
            }
        }

        // Auto trigger biometrics if enabled
        if (prefs.isBiometricEnabled() && isBiometricAvailable()) {
            triggerBiometrics()
        }
    }

    private fun unlockAndFinish() {
        AppLockState.markUnlocked(targetPackage)
        finish()
    }

    private fun goToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    private fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun triggerBiometrics() {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                unlockAndFinish()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // Stay on PIN screen
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Разблокировка «$appName»")
            .setSubtitle("Подтвердите личность для доступа")
            .setNegativeButtonText("Использовать PIN")
            .build()

        prompt.authenticate(promptInfo)
    }

    @Composable
    private fun LockScreenContent(
        appName: String,
        targetPackage: String,
        onUnlockSuccess: () -> Unit,
        onTriggerBiometric: () -> Unit
    ) {
        var enteredPin by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }
        var showRecoveryDialog by remember { mutableStateOf(false) }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBackground.copy(alpha = 0.95f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top area: App info & Lock icon
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Приложение заблокировано",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = appName,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    PinDotsIndicator(
                        pinLength = 4,
                        enteredLength = enteredPin.length,
                        isError = isError
                    )
                }

                // Middle/Bottom area: Keypad
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    PinKeypad(
                        onDigitClick = { digit ->
                            if (enteredPin.length < 4) {
                                enteredPin += digit
                                isError = false
                                if (enteredPin.length == 4) {
                                    if (prefs.checkPin(enteredPin)) {
                                        onUnlockSuccess()
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
                        onBiometricClick = onTriggerBiometric,
                        showBiometric = prefs.isBiometricEnabled() && isBiometricAvailable()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Forgot PIN button
                    TextButton(onClick = { showRecoveryDialog = true }) {
                        Text(
                            text = "Забыли PIN-код?",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Recovery Dialog
        if (showRecoveryDialog) {
            var answer by remember { mutableStateOf("") }
            var ansError by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showRecoveryDialog = false },
                title = { Text("Восстановление доступа") },
                text = {
                    Column {
                        Text(
                            text = "Вопрос: ${prefs.getSecurityQuestion()}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = answer,
                            onValueChange = {
                                answer = it
                                ansError = false
                            },
                            label = { Text("Ваш секретный ответ") },
                            isError = ansError,
                            supportingText = {
                                if (ansError) {
                                    Text("Неверный ответ")
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (prefs.checkRecoveryAnswer(answer)) {
                            showRecoveryDialog = false
                            Toast.makeText(this@LockActivity, "Доступ подтвержден", Toast.LENGTH_SHORT).show()
                            onUnlockSuccess()
                        } else {
                            ansError = true
                        }
                    }) {
                        Text("Подтвердить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRecoveryDialog = false }) {
                        Text("Отмена")
                    }
                }
            )
        }
    }
}
